package pathstore.system.schemaFSM;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.authentication.AuthenticationUtil;
import pathstore.authentication.ClientAuthenticationUtil;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreThreadManager;
import pathstore.common.QueryCache;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.PathStorePushServer;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.util.SchemaInfo;

import java.util.stream.Collectors;

/**
 * This class is the slave schema loader.
 *
 * <p>This class reads pathstore_applications.node_schemas and reads the relevant data associated to
 * this node. It will look for INSTALLING or REMOVING a certain process.
 *
 * @see PathStoreMasterSchemaServer
 */
public class PathStoreSlaveSchemaServer implements Runnable {

  /** Logger */
  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreSlaveSchemaServer.class);

  /** Session used to interact with pathstore */
  private final Session session = PathStoreCluster.getDaemonInstance().connect();

  /** Node id so you don't need to query the properties file every run */
  private final int nodeId = PathStoreProperties.getInstance().NodeID;

  /** Reference to the sub process thread pool */
  private final PathStoreThreadManager subProcessManager =
      PathStoreThreadManager.subProcessInstance();

  /**
   * This daemon is used to install an application on the local machine. The steps it takes are as
   * follows:
   *
   * <p>(1): Query the node_schemas table and with the conditions that the partition key is
   * (node_id: current_node_id)
   *
   * <p>(2): On retrieval of the information forall records write an update to the table
   * transitioning each statue to PROCESSING_INSTALLING and PROCESSING_REMOVING respectively
   *
   * <p>(3): After the update is complete start a sub process to actually perform each install /
   * removal concurrently
   */
  @Override
  public void run() {
    while (true) {
      // (1)
      Select deploymentRecordQuery =
          QueryBuilder.select()
              .all()
              .from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

      deploymentRecordQuery.where(
          QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, this.nodeId));

      for (Row row : this.session.execute(deploymentRecordQuery)) {
        ProccessStatus currentStatus =
            ProccessStatus.valueOf(row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS));

        if (currentStatus != ProccessStatus.INSTALLING && currentStatus != ProccessStatus.REMOVING)
          continue;

        String keyspaceName = row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME);

        // (2)
        this.transitionRow(currentStatus, keyspaceName);

        // (3)
        this.spawnSubProcess(currentStatus, keyspaceName);
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.error(e);
      }
    }
  }

  /**
   * This function transforms an Installing row to a Processing_Installing row and a Removing row to
   * a Processing_Removing row
   *
   * @param processStatus what is the status of the row
   * @param keyspace what is the keyspace (used to identify primary key)
   */
  private void transitionRow(final ProccessStatus processStatus, final String keyspace) {

    Update transitionState =
        QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    transitionState
        .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, this.nodeId))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspace))
        .with(
            QueryBuilder.set(
                Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS,
                processStatus == ProccessStatus.INSTALLING
                    ? ProccessStatus.PROCESSING_INSTALLING.toString()
                    : ProccessStatus.PROCESSING_REMOVING.toString()));

    this.session.execute(transitionState);
  }

  /**
   * Simple function that will start a sub process for each row that needs an operation performed on
   *
   * @param proccessStatus what is the current status (Installing or Removing since we never require
   *     after update)
   * @param keyspace what keyspace to perform the given operation on
   */
  private void spawnSubProcess(final ProccessStatus proccessStatus, final String keyspace) {

    this.subProcessManager.spawn(
        () -> {
          switch (proccessStatus) {
            case INSTALLING:
              logger.info(
                  String.format(
                      "Spawned sub thread to install %s on node %d", keyspace, this.nodeId));

              String augmentedKeyspace = this.getAugmentedKeyspace(keyspace);

              if (augmentedKeyspace == null) {
                logger.error("No augmented keyspace for: " + keyspace);
                return;
              }

              this.installApplication(keyspace, augmentedKeyspace);

              break;
            case REMOVING:
              logger.info(
                  String.format(
                      "Spawned sub thread to remove %s on node %d", keyspace, this.nodeId));

              this.removeApplication(keyspace);
              break;
          }
        });
  }

  /**
   * This function will return the augmented keyspace given the name of the keyspace
   *
   * @param keyspace keyspace to query
   * @return augmented keyspace if present else null
   */
  private String getAugmentedKeyspace(final String keyspace) {
    Select augmentedKeyspaceSelect =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);
    augmentedKeyspaceSelect.where(QueryBuilder.eq(Constants.APPS_COLUMNS.KEYSPACE_NAME, keyspace));

    for (Row row : session.execute(augmentedKeyspaceSelect))
      return row.getString(Constants.APPS_COLUMNS.AUGMENTED_SCHEMA);

    return null;
  }

  /**
   * Install an application first it queries the augmented schema from the apps table. If it exists
   * then we update the node_schemas table with that we have installed the application
   *
   * @param keyspace application to install
   */
  private void installApplication(final String keyspace, final String augmentedSchema) {

    Session superUserSession = PathStorePrivilegedCluster.getSuperUserInstance().connect();

    PathStoreSchemaLoaderUtils.parseSchema(augmentedSchema).forEach(superUserSession::execute);

    this.logger.info(String.format("Application loaded %s", keyspace));

    // after keyspace is loaded we need to inform the schemainfo class that a new keyspace has been
    // installed
    SchemaInfo.getInstance().loadKeyspace(keyspace);

    // grant permissions to daemon account on the write
    AuthenticationUtil.grantAccessToKeyspace(
        superUserSession, keyspace, Constants.PATHSTORE_DAEMON_USERNAME);

    this.logger.info(
        String.format(
            "Granted permission on keyspace %s to user %s",
            keyspace, Constants.PATHSTORE_DAEMON_USERNAME));

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    update
        .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, this.nodeId))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspace))
        .with(
            QueryBuilder.set(
                Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS,
                ProccessStatus.INSTALLED.toString()));

    this.session.execute(update);
  }

  /**
   * TODO: Force push data
   *
   * <p>Drops the keyspace if it exists then it updates node_schema that said keyspace is removed
   *
   * @param keyspace application to remove
   */
  private void removeApplication(final String keyspace) {

    Session superUserSession = PathStorePrivilegedCluster.getSuperUserInstance().connect();

    this.forcePush(keyspace);

    logger.info(String.format("Forced push all entries for keyspace %s", keyspace));

    SchemaInfo.getInstance().removeKeyspace(keyspace);

    QueryCache.getInstance().remove(keyspace);

    this.logger.info(String.format("Removed cache entries for keyspace %s", keyspace));

    if (ClientAuthenticationUtil.deleteClientAccount(keyspace))
      this.logger.info(String.format("Removed temporary client account for keyspace %s", keyspace));

    // called after schema info is removed so that the push server won't call on this keyspace and
    // throw an error
    // TODO: (Myles) do we need to wait for the push server to finish its current trip before
    // revoking permissions on that table
    AuthenticationUtil.revokeAccessToKeyspace(
        superUserSession, keyspace, Constants.PATHSTORE_DAEMON_USERNAME);

    this.logger.info(
        String.format(
            "Revoked access to keyspace %s to %s", keyspace, Constants.PATHSTORE_DAEMON_USERNAME));

    superUserSession.execute("drop keyspace if exists " + keyspace);

    this.logger.info(String.format("Removed keyspace %s", keyspace));

    Delete delete =
        QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    delete
        .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, this.nodeId))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspace));

    this.session.execute(delete);

    this.logger.info("Application removed " + keyspace);
  }

  /**
   * This function is used to force push all data from this keyspace to its parent before removal,
   * to not have any data missing
   *
   * @param keyspace keyspace to push data for
   */
  private void forcePush(final String keyspace) {
    PathStorePushServer.push(
        SchemaInfo.getInstance().getTablesFromKeyspace(keyspace).stream()
            .filter(PathStorePushServer.filterOutViewAndLocal)
            .collect(Collectors.toList()),
        PathStorePrivilegedCluster.getDaemonInstance().connect(),
        PathStorePrivilegedCluster.getParentInstance().connect(),
        SchemaInfo.getInstance(),
        PathStoreProperties.getInstance().NodeID);
  }
}

package pathstore.system.schemaFSM;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreThreadManager;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;
import pathstore.system.PathStorePriviledgedCluster;
import pathstore.util.SchemaInfo;

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
  private final Session session = PathStoreCluster.getInstance().connect();

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

        if (currentStatus != ProccessStatus.INSTALLING) continue;

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

              this.install_application(keyspace, augmentedKeyspace);

              break;
            case REMOVING:
              logger.info(
                  String.format(
                      "Spawned sub thread to remove %s on node %d", keyspace, this.nodeId));

              this.remove_application(keyspace);
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
  private void install_application(final String keyspace, final String augmentedSchema) {

    PathStoreSchemaLoaderUtils.parseSchema(augmentedSchema)
        .forEach(PathStorePriviledgedCluster.getInstance().connect()::execute);

    SchemaInfo.getInstance().getKeySpaceInfo(keyspace);

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    update
        .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, this.nodeId))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspace))
        .with(
            QueryBuilder.set(
                Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS,
                ProccessStatus.INSTALLED.toString()));

    this.session.execute(update);

    logger.info("Application loaded " + keyspace);
  }

  /**
   * Drops the keyspace if it exists then it updates node_schema that said keyspace is removed
   *
   * @param keyspace application to remove
   */
  private void remove_application(final String keyspace) {
    SchemaInfo.getInstance().removeKeyspace(keyspace);
    PathStorePriviledgedCluster.getInstance()
        .connect()
        .execute("drop keyspace if exists " + keyspace);

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    update
        .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, this.nodeId))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspace))
        .with(
            QueryBuilder.set(
                Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, ProccessStatus.REMOVED.toString()));

    this.session.execute(update);

    logger.info("Application removed " + keyspace);
  }
}

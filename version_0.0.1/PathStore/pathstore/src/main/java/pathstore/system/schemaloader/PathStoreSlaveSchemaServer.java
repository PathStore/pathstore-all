package pathstore.system.schemaloader;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
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
public class PathStoreSlaveSchemaServer extends Thread {

  /**
   * Selects all rows related to the nodeid specified in {@link PathStoreProperties#NodeID}
   *
   * <p>Then if the process_status is INSTALLING we install the application specified
   *
   * <p>If the process_Status is REMOVING we remove the application specified
   */
  @Override
  public void run() {
    while (true) {
      Session session = PathStoreCluster.getInstance().connect();
      Select current_processes_select_all =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);

      for (Row current_process_row : session.execute(current_processes_select_all)) {
        String keyspace_name = current_process_row.getString(Constants.APPS_COLUMNS.KEYSPACE_NAME);

        String augmentedSchema =
            current_process_row.getString(Constants.APPS_COLUMNS.AUGMENTED_SCHEMA);

        Select node_schemas_specific_select =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
        node_schemas_specific_select
            .where(
                QueryBuilder.eq(
                    Constants.NODE_SCHEMAS_COLUMNS.NODE_ID,
                    PathStoreProperties.getInstance().NodeID))
            .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspace_name));

        for (Row node_schemas_select : session.execute(node_schemas_specific_select)) {
          ProccessStatus current_process_status =
              ProccessStatus.valueOf(
                  node_schemas_select.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS));
          switch (current_process_status) {
            case INSTALLING:
              this.install_application(
                  session,
                  node_schemas_select.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME),
                  augmentedSchema,
                  node_schemas_select.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID));
              break;
            case REMOVING:
              this.remove_application(
                  session,
                  node_schemas_select.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME),
                  node_schemas_select.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID));
              break;
          }
        }
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Install an application first it queries the augmented schema from the apps table. If it exists
   * then we update the node_schemas table with that we have installed the application
   *
   * @param session {@link PathStoreCluster#connect()}
   * @param keyspace application to install
   */
  private void install_application(
      final Session session,
      final String keyspace,
      final String augmentedSchema,
      final String process_uuid) {
    // Query application, if not exist then just continue and wait for it to exist
    System.out.println("Loading application: " + keyspace);

    PathStoreSchemaLoaderUtils.parseSchema(augmentedSchema)
        .forEach(PathStorePriviledgedCluster.getInstance().connect()::execute);

    SchemaInfo.getInstance().getKeySpaceInfo(keyspace);

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    update
        .where(
            QueryBuilder.eq(
                Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, PathStoreProperties.getInstance().NodeID))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspace))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID, process_uuid))
        .with(
            QueryBuilder.set(
                Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS,
                ProccessStatus.INSTALLED.toString()));

    session.execute(update);
  }

  /**
   * Drops the keyspace if it exists then it updates node_schema that said keyspace is removed
   *
   * @param session {@link PathStoreCluster#connect()}
   * @param keyspace application to remove
   */
  private void remove_application(
      final Session session, final String keyspace, final String process_uuid) {
    System.out.println("Removing application " + keyspace);
    SchemaInfo.getInstance().removeKeyspace(keyspace);
    PathStorePriviledgedCluster.getInstance()
        .connect()
        .execute("drop keyspace if exists " + keyspace);

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    update
        .where(
            QueryBuilder.eq(
                Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, PathStoreProperties.getInstance().NodeID))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspace))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID, process_uuid))
        .with(
            QueryBuilder.set(
                Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, ProccessStatus.REMOVED.toString()));
    session.execute(update);
  }
}

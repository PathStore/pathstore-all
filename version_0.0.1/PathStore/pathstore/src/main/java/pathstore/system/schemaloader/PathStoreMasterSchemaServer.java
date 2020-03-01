package pathstore.system.schemaloader;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;
import pathstore.client.PathStoreResultSet;
import pathstore.common.Constants;
import pathstore.system.PathStorePriviledgedCluster;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This is the master schema server which will only run on the ROOTSERVER
 *
 * <p>Installing cycle:
 *
 * <p>Waiting_Install -> Installing -> Installed
 *
 * <p>Removing cycle:
 *
 * <p>Waiting_Remove -> Removing -> Removed
 */
public class PathStoreMasterSchemaServer extends Thread {
  /**
   * The logic behind this is that it will read all data from the node_schemas table and parse it
   * into a hashmap of keyspace -> list of {@link ApplicationEntry}
   *
   * <p>Then it will check all the waiting entries if what it is waiting for has completed if it has
   * completed it will update their status to either INSTALLING or REMOVING and the slave loader
   * will handle that
   */
  @Override
  public void run() {
    while (true) {

      // First we will query the database to read from pathstore_applications.node_schemas

      Session privileged_session = PathStorePriviledgedCluster.getInstance().connect();

      Select select =
          QueryBuilder.select()
              .all()
              .from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

      Map<String, List<ApplicationEntry>> data = new HashMap<>();

      for (Row row :
          new PathStoreResultSet(
              privileged_session.execute(select),
              Constants.PATHSTORE_APPLICATIONS,
              Constants.NODE_SCHEMAS)) {
        String keyspace = row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME);

        if (!data.containsKey(keyspace)) data.put(keyspace, new ArrayList<>());

        data.get(keyspace)
            .add(
                new ApplicationEntry(
                    row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID),
                    ProccessStatus.valueOf(
                        row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS)),
                    row.getInt(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR)));
      }

      // TODO: Clean this up. Lots of duplicate code
      // Can we make any assumptions about the node id? Currently my assumption is we can't
      for (String keyspace : data.keySet()) {
        List<ApplicationEntry> application_entries = data.get(keyspace);

        Set<Integer> installed =
            this.filter_application_entries_to_nodeids(
                application_entries, ProccessStatus.INSTALLED);

        Set<ApplicationEntry> waiting =
            this.filter_application_entries(application_entries, ProccessStatus.WAITING_INSTALL);

        // Maybe break here?
        for (ApplicationEntry node : waiting) {
          if (node.waiting_for == -1 || installed.contains(node.waiting_for)) {
            System.out.println(
                "Initiating application for: " + node.node_id + " with application: " + keyspace);
            this.update_application_status(node.node_id, keyspace, ProccessStatus.INSTALLING);
          }
        }

        Set<Integer> removed =
            this.filter_application_entries_to_nodeids(application_entries, ProccessStatus.REMOVED);

        Set<ApplicationEntry> waiting_remove =
            this.filter_application_entries(application_entries, ProccessStatus.WAITING_REMOVE);

        // Maybe break here?
        for (ApplicationEntry node : waiting_remove) {
          if (node.waiting_for == -1 || removed.contains(node.waiting_for)) {
            System.out.println(
                "Removing application for: " + node.node_id + " with application: " + keyspace);
            this.update_application_status(node.node_id, keyspace, ProccessStatus.REMOVING);
          }
        }
      }

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Filters application entires based on a status and returns a set of nodeids
   *
   * @param application_entries application entries to filter
   * @param filter_status what to filter by
   * @return set of nodeids of filtered data
   */
  private Set<Integer> filter_application_entries_to_nodeids(
      final List<ApplicationEntry> application_entries, final ProccessStatus filter_status) {
    return this.filter_application_entries(application_entries, filter_status).stream()
        .map(i -> i.node_id)
        .collect(Collectors.toSet());
  }

  /**
   * Filters application entires based on a status and returns a set of entries
   *
   * @param application_entries application entries to filter
   * @param filter_status what to filter by
   * @return set of entries that met the criteria
   */
  private Set<ApplicationEntry> filter_application_entries(
      final List<ApplicationEntry> application_entries, final ProccessStatus filter_status) {
    return application_entries.stream()
        .filter(i -> i.proccess_status == filter_status)
        .collect(Collectors.toSet());
  }

  /**
   * Updates a row in the node_schemas table with the new processing status to trigger the slave
   * schema loader to perform some action
   *
   * @param nodeid nodeid to update
   * @param keyspace_name application that you want to update
   * @param status status to set either {@link ProccessStatus#INSTALLED} or {@link
   *     ProccessStatus#REMOVING}
   */
  private void update_application_status(
      final int nodeid, final String keyspace_name, final ProccessStatus status) {
    Session client_session = PathStoreCluster.getInstance().connect();

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    update
        .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, nodeid))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspace_name))
        .with(QueryBuilder.set(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, status.toString()));

    client_session.execute(update);
  }
}

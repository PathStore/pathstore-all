package pathstore.system.schemaloader;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;

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
   * First sort all rows into groups based on their process id's
   *
   * <p>Then call {@link #update(ProccessStatus, ProccessStatus, ProccessStatus, Set)} on all groups
   * that remain to either update the next node in the chain or do nothing
   */
  @Override
  public void run() {
    while (true) {
      Session client_session = PathStoreCluster.getInstance().connect();

      Map<String, Set<ApplicationEntry>> keyspace_name_to_application_set = new HashMap<>();

      Select query_all_node_schema =
          QueryBuilder.select()
              .all()
              .from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

      // Query all application rows
      for (Row row : client_session.execute(query_all_node_schema)) {
        String keyspace_name = row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME);
        keyspace_name_to_application_set.computeIfAbsent(keyspace_name, k -> new HashSet<>());
        keyspace_name_to_application_set
            .get(keyspace_name)
            .add(
                new ApplicationEntry(
                    row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID),
                    keyspace_name,
                    ProccessStatus.valueOf(
                        row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS)),
                    row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID),
                    row.getInt(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR)));
      }

      for (String keyspace_name : keyspace_name_to_application_set.keySet()) {
        this.update(
            ProccessStatus.INSTALLED,
            ProccessStatus.INSTALLING,
            ProccessStatus.WAITING_INSTALL,
            keyspace_name_to_application_set.get(keyspace_name));

        this.update(
            ProccessStatus.REMOVED,
            ProccessStatus.REMOVING,
            ProccessStatus.WAITING_REMOVE,
            keyspace_name_to_application_set.get(keyspace_name));
      }

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * This function updates all entries that were waiting for something to occur, once that task has
   * finished we change their processing state for the slave loader to install or remove an
   * application
   *
   * @param finished finished state either {@link ProccessStatus#INSTALLED} or {@link
   *     ProccessStatus#REMOVED}
   * @param processing processing state either {@link ProccessStatus#INSTALLING} or {@link
   *     ProccessStatus#REMOVING}
   * @param waiting waiting state either {@link ProccessStatus#WAITING_INSTALL} or {@link
   *     ProccessStatus#WAITING_REMOVE}
   * @param entries list of all entries
   */
  private void update(
      final ProccessStatus finished,
      final ProccessStatus processing,
      final ProccessStatus waiting,
      final Set<ApplicationEntry> entries) {
    Set<Integer> finished_ids = this.filter_entries_to_node_id(entries, finished);

    Set<ApplicationEntry> waiting_entries = this.filter_entries(entries, waiting);

    for (ApplicationEntry entry : waiting_entries)
      if (entry.waiting_for == -1 || finished_ids.contains(entry.waiting_for))
        this.update_application_status(
            entry.node_id, entry.keyspace_name, processing, entry.process_uuid);
  }

  /**
   * Filters a set of application entries by their process status
   *
   * @param entries entries to filter
   * @param status status to filter by
   * @return a filtered set of entries
   */
  private Set<ApplicationEntry> filter_entries(
      final Set<ApplicationEntry> entries, final ProccessStatus status) {
    return entries.stream().filter(i -> i.proccess_status == status).collect(Collectors.toSet());
  }

  /**
   * Filters a set of application entries by a process status and collects them to a set of their
   * node id's
   *
   * @param entries entries to filter
   * @param status status to filter by
   * @return a filtered set of entries to their node ids
   */
  private Set<Integer> filter_entries_to_node_id(
      final Set<ApplicationEntry> entries, final ProccessStatus status) {
    return this.filter_entries(entries, status).stream()
        .map(i -> i.node_id)
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
      final int nodeid,
      final String keyspace_name,
      final ProccessStatus status,
      final String process_uuid) {

    System.out.println(
        (status == ProccessStatus.INSTALLING ? "Installing application " : "Removing application ")
            + keyspace_name
            + " on node "
            + nodeid
            + " which is part of the process group: "
            + process_uuid);

    Session client_session = PathStoreCluster.getInstance().connect();

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    update
        .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, nodeid))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspace_name))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID, process_uuid))
        .with(QueryBuilder.set(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, status.toString()));

    client_session.execute(update);
  }
}

package pathstore.system.schemaloader;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.*;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;

import java.util.*;

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

    String keyspace_name = null, processing_uuid = null;
    Set<Integer> finished_ids = new HashSet<>(), processing_ids = new HashSet<>();
    Set<ApplicationEntry> waiting_entries = new HashSet<>();

    for (ApplicationEntry entry : entries) {
      ProccessStatus current_status = entry.proccess_status;

      if (keyspace_name == null) keyspace_name = entry.keyspace_name;
      if (processing_uuid == null) processing_uuid = entry.process_uuid;

      if (current_status == finished) finished_ids.add(entry.node_id);
      else if (current_status == processing) processing_ids.add(entry.node_id);
      else if (current_status == waiting) waiting_entries.add(entry);
    }

    this.handle_current_process_table(
        finished_ids.size(),
        processing_ids.size(),
        waiting_entries.size(),
        processing_uuid,
        keyspace_name);

    for (ApplicationEntry entry : waiting_entries)
      if (entry.waiting_for == -1 || finished_ids.contains(entry.waiting_for))
        this.update_application_status(
            entry.node_id, entry.keyspace_name, processing, entry.process_uuid);
  }

  /**
   * This function will add or remove an entry from the current_processes table under certain
   * conditions.
   *
   * <p>there must be 0 rows currently be processed for either case to occur
   *
   * <p>if there are no finished entries then we know that the process has just been started. Thus
   * we add to the table
   *
   * <p>if there are no waiting entires then we know that the process has finished. Thus we remove
   * from the table
   *
   * @param num_of_finished # of finished entries
   * @param num_of_processing # of processing entries
   * @param num_of_waiting # of waiting entries
   * @param process_uuid process_uuid
   * @param keyspace_name keyspace_name
   */
  private void handle_current_process_table(
      final int num_of_finished,
      final int num_of_processing,
      final int num_of_waiting,
      final String process_uuid,
      final String keyspace_name) {

    if (num_of_processing == 0) {
      Session client_session = PathStoreCluster.getInstance().connect();
      if (num_of_finished == 0) {
        Select select =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.CURRENT_PROCESSES);
        select.where(
            QueryBuilder.eq(Constants.CURRENT_PROCESSES_COLUMNS.PROCESS_UUID, process_uuid));

        if (client_session.execute(select).one() == null) {
          System.out.println("Inserting process: " + process_uuid);
          Insert insert =
              QueryBuilder.insertInto(
                  Constants.PATHSTORE_APPLICATIONS, Constants.CURRENT_PROCESSES);
          insert.value(Constants.CURRENT_PROCESSES_COLUMNS.PROCESS_UUID, process_uuid);
          insert.value(Constants.CURRENT_PROCESSES_COLUMNS.KEYSPACE_NAME, keyspace_name);
          client_session.execute(insert);
        }
      } else if (num_of_waiting == 0) {
        Select select =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.CURRENT_PROCESSES);
        select.where(
            QueryBuilder.eq(Constants.CURRENT_PROCESSES_COLUMNS.PROCESS_UUID, process_uuid));

        if (client_session.execute(select).one() != null) {
          System.out.println("Removing process: " + process_uuid);
          Delete delete =
              QueryBuilder.delete()
                  .from(Constants.PATHSTORE_APPLICATIONS, Constants.CURRENT_PROCESSES);
          delete.where(
              QueryBuilder.eq(Constants.CURRENT_PROCESSES_COLUMNS.PROCESS_UUID, process_uuid));
          client_session.execute(delete);
        }
      }
    }
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

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
   * that remain to either update the next node(s) in the chain or do nothing
   */
  @Override
  @SuppressWarnings("ALL")
  public void run() {
    while (true) {
      Session client_session = PathStoreCluster.getInstance().connect();

      Map<UUID, ProccessStatus> proccessStatusMap = new HashMap<>();

      Map<UUID, String> keyspaceMap = new HashMap<>();

      Map<UUID, Set<ApplicationEntry>> processIdToApplicationSet = new HashMap<>();

      Select query_all_node_schema =
          QueryBuilder.select()
              .all()
              .from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

      // Query all application rows
      for (Row row : client_session.execute(query_all_node_schema)) {
        final UUID processUUID =
            UUID.fromString(row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID));

        final ProccessStatus currentProcessStatus =
            ProccessStatus.valueOf(row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS));

        proccessStatusMap.computeIfAbsent(processUUID, k -> currentProcessStatus);

        final String keyspace = row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME);

        keyspaceMap.computeIfAbsent(processUUID, k -> keyspace);

        processIdToApplicationSet.computeIfAbsent(processUUID, k -> new HashSet<>());
        processIdToApplicationSet
            .get(processUUID)
            .add(
                new ApplicationEntry(
                    row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID),
                    keyspace,
                    currentProcessStatus,
                    processUUID,
                    (List<Integer>) row.getObject(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR)));
      }

      System.out.println(proccessStatusMap);
      System.out.println(keyspaceMap);

      for (UUID processUUID : processIdToApplicationSet.keySet()) {

        final Set<ApplicationEntry> applicationEntries = processIdToApplicationSet.get(processUUID);

        final String keyspace = keyspaceMap.get(processUUID);

        switch (proccessStatusMap.get(processUUID)) {
          case WAITING_INSTALL:
          case INSTALLING:
          case INSTALLED:
            System.out.println(
                "Process UUID: "
                    + processUUID
                    + " is an installer with "
                    + applicationEntries.size()
                    + " entries");
            this.update(
                processUUID,
                keyspace,
                ProccessStatus.INSTALLED,
                ProccessStatus.INSTALLING,
                ProccessStatus.WAITING_INSTALL,
                applicationEntries);
            break;
          case WAITING_REMOVE:
          case REMOVING:
          case REMOVED:
            System.out.println(
                "Process UUID: "
                    + processUUID
                    + " is a remover with "
                    + applicationEntries.size()
                    + " entries");
            this.update(
                processUUID,
                keyspace,
                ProccessStatus.REMOVED,
                ProccessStatus.REMOVING,
                ProccessStatus.WAITING_REMOVE,
                applicationEntries);
            break;
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
   * @param entries list of all entries based on processUUID
   */
  private void update(
      final UUID processingUUID,
      final String keyspace,
      final ProccessStatus finished,
      final ProccessStatus processing,
      final ProccessStatus waiting,
      final Set<ApplicationEntry> entries) {

    Set<Integer> finishedIds = new HashSet<>(), processingIds = new HashSet<>();
    Set<ApplicationEntry> waitingEntries = new HashSet<>();

    for (ApplicationEntry entry : entries) {
      ProccessStatus currentStatus = entry.proccess_status;

      if (currentStatus == finished) finishedIds.add(entry.node_id);
      else if (currentStatus == processing) processingIds.add(entry.node_id);
      else if (currentStatus == waiting) waitingEntries.add(entry);
    }

    this.handle_current_process_table(
        finishedIds.size(), processingIds.size(), waitingEntries.size(), processingUUID, keyspace);

    for (ApplicationEntry entry : waitingEntries)
      if (finishedIds.containsAll(entry.waiting_for)
          || (entry.waiting_for.size() == 1 && entry.waiting_for.get(0) == -1))
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
   * @param numOfFinished # of finished entries
   * @param numOfProcessing # of processing entries
   * @param numOfWaiting # of waiting entries
   * @param process_uuid process_uuid
   * @param keyspace_name keyspace_name
   */
  private void handle_current_process_table(
      final int numOfFinished,
      final int numOfProcessing,
      final int numOfWaiting,
      final UUID process_uuid,
      final String keyspace_name) {

    if (numOfProcessing == 0) {
      Session clientSession = PathStoreCluster.getInstance().connect();
      if (numOfFinished == 0) {
        Select select =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.CURRENT_PROCESSES);
        select.where(
            QueryBuilder.eq(
                Constants.CURRENT_PROCESSES_COLUMNS.PROCESS_UUID, process_uuid.toString()));

        if (clientSession.execute(select).one() == null) {
          System.out.println("Inserting process: " + process_uuid);
          Insert insert =
              QueryBuilder.insertInto(
                  Constants.PATHSTORE_APPLICATIONS, Constants.CURRENT_PROCESSES);
          insert.value(Constants.CURRENT_PROCESSES_COLUMNS.PROCESS_UUID, process_uuid.toString());
          insert.value(Constants.CURRENT_PROCESSES_COLUMNS.KEYSPACE_NAME, keyspace_name);
          clientSession.execute(insert);
        }
      } else if (numOfWaiting == 0) {
        Select select =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.CURRENT_PROCESSES);
        select.where(
            QueryBuilder.eq(
                Constants.CURRENT_PROCESSES_COLUMNS.PROCESS_UUID, process_uuid.toString()));

        int numOfRows = 0;
        for (Row ignored : clientSession.execute(select)) numOfRows += 1;

        if (numOfRows == 1) {
          System.out.println("Removing process: " + process_uuid);
          Delete delete =
              QueryBuilder.delete()
                  .from(Constants.PATHSTORE_APPLICATIONS, Constants.CURRENT_PROCESSES);
          delete.where(
              QueryBuilder.eq(
                  Constants.CURRENT_PROCESSES_COLUMNS.PROCESS_UUID, process_uuid.toString()));
          clientSession.execute(delete);
        }
      }
    }
  }

  /**
   * Updates a row in the node_schemas table with the new processing status to trigger the slave
   * schema loader to perform some action
   *
   * @param nodeid nodeid to update
   * @param keyspaceName application that you want to update
   * @param status status to set either {@link ProccessStatus#INSTALLED} or {@link
   *     ProccessStatus#REMOVING}
   */
  private void update_application_status(
      final int nodeid,
      final String keyspaceName,
      final ProccessStatus status,
      final UUID processUuid) {

    System.out.println(
        (status == ProccessStatus.INSTALLING ? "Installing application " : "Removing application ")
            + keyspaceName
            + " on node "
            + nodeid
            + " which is part of the process group: "
            + processUuid);

    Session client_session = PathStoreCluster.getInstance().connect();

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    update
        .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, nodeid))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, keyspaceName))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID, processUuid.toString()))
        .with(QueryBuilder.set(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, status.toString()));

    client_session.execute(update);
  }
}

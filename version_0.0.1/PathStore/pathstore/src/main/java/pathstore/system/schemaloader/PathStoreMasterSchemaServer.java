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
  @Override
  public void run() {
    while (true) {
      Session privileged_session = PathStorePriviledgedCluster.getInstance().connect();

      Map<String, Set<ApplicationEntry>> process_uuid_to_set_of_entries = new HashMap<>();

      // Query all application rows
      for (Row row :
          privileged_session.execute(
              QueryBuilder.select().all().from(Constants.APPS, Constants.NODE_SCHEMAS))) {
        String process_uuid = row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID);
        process_uuid_to_set_of_entries.computeIfAbsent(process_uuid, k -> new HashSet<>());
        process_uuid_to_set_of_entries
            .get(process_uuid)
            .add(
                new ApplicationEntry(
                    row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID),
                    row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME),
                    ProccessStatus.valueOf(
                        row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS)),
                    row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS),
                    row.getInt(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR)));
      }

      List<String> to_delete = new LinkedList<>();

      // Filter out garbage process_ids
      for (String process_uuid : process_uuid_to_set_of_entries.keySet()) {
        int num_of_waiting = 0;
        int num_of_installing = 0;

        for (ApplicationEntry current_entry : process_uuid_to_set_of_entries.get(process_uuid)) {
          switch (current_entry.proccess_status) {
            case WAITING_INSTALL:
              num_of_waiting += 1;
              break;
            case INSTALLING:
              num_of_installing += 1;
              break;
          }
        }

        if (num_of_waiting == num_of_installing) to_delete.add(process_uuid);
      }

      to_delete.forEach(process_uuid_to_set_of_entries::remove);

      for (String process_uuid : process_uuid_to_set_of_entries.keySet()) {
        Set<Integer> installed =
            process_uuid_to_set_of_entries.get(process_uuid).stream()
                .filter(i -> i.proccess_status == ProccessStatus.INSTALLED)
                .map(i -> i.node_id)
                .collect(Collectors.toSet());

        Set<ApplicationEntry> waiting_install =
            process_uuid_to_set_of_entries.get(process_uuid).stream()
                .filter(i -> i.proccess_status == ProccessStatus.WAITING_INSTALL)
                .collect(Collectors.toSet());

        for (ApplicationEntry current_entry : waiting_install) {
          if (current_entry.waiting_for == -1 || installed.contains(current_entry.waiting_for)) {
            this.update_application_status(
                current_entry.node_id,
                current_entry.keyspace_name,
                ProccessStatus.INSTALLING,
                current_entry.process_uuid);
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

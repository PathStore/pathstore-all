package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.common.Constants;
import pathstore.system.schemaloader.ApplicationEntry;
import pathstore.system.schemaloader.ProccessStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class of shared functions between {@link InstallApplication} and {@link UnInstallApplication}
 */
public class ApplicationUtil {

  /**
   * Queries the previous state of the entire topology for the given keyspace. This is done to avoid
   * duplicate records being produced. See readme for additional info.
   *
   * @return map of nodeid to application entire if app is keyspace
   */
  @SuppressWarnings("ALL")
  public static Map<Integer, ApplicationEntry> getPreviousState(
      final Session session, final String keyspace) {
    Map<Integer, ApplicationEntry> previousState = new HashMap<>();

    Select query_node_schema =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    for (Row row : session.execute(query_node_schema)) {
      String rowKeyspace = row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME);

      if (rowKeyspace.equals(keyspace)) {
        int nodeId = row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID);

        previousState.put(
            nodeId,
            new ApplicationEntry(
                nodeId,
                rowKeyspace,
                ProccessStatus.valueOf(
                    row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS)),
                UUID.fromString(row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID)),
                (List<Integer>) row.getObject(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR)));
      }
    }

    return previousState;
  }

  /**
   * TODO: Rollback on ERROR.
   *
   * <p>This function inserts the current state records into the database.
   *
   * @param currentState state gathered from {@link InstallApplication} or {@link UnInstallApplication}
   * @return number of records written to the database
   */
  public static int insertRequestToDb(
      final Session session, final Map<Integer, ApplicationEntry> currentState) {
    for (Map.Entry<Integer, ApplicationEntry> current_entry : currentState.entrySet()) {
      ApplicationEntry applicationEntry = current_entry.getValue();

      Insert insert =
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

      insert
          .value(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, applicationEntry.node_id)
          .value(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, applicationEntry.keyspace_name)
          .value(
              Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS,
              applicationEntry.proccess_status.toString())
          .value(
              Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID, applicationEntry.process_uuid.toString())
          .value(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, applicationEntry.waiting_for);

      session.execute(insert);
    }

    return currentState.size();
  }
}

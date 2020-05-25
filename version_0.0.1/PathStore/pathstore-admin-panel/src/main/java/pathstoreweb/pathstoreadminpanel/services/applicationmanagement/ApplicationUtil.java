package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;

import java.util.HashMap;
import java.util.Map;

import pathstore.system.schemaFSM.NodeSchemaEntry;
import pathstore.system.schemaFSM.ProccessStatus;
import pathstoreweb.pathstoreadminpanel.services.RuntimeErrorFormatter;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter.UpdateApplicationStateFormatter;

/**
 * Utility class of shared functions between {@link InstallApplication} and {@link
 * UnInstallApplication}
 */
class ApplicationUtil {

  /**
   * Queries the previous state of the entire topology for the given keyspace. This is done to avoid
   * duplicate records being produced. See readme for additional info.
   *
   * @return map of nodeid to application entire if app is keyspace
   */
  public static Map<Integer, NodeSchemaEntry> getPreviousState(
      final Session session, final String keyspace) {
    Map<Integer, NodeSchemaEntry> previousState = new HashMap<>();

    Select query_node_schema =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    for (Row row : session.execute(query_node_schema)) {
      String rowKeyspace = row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME);

      if (rowKeyspace.equals(keyspace)) {
        int nodeId = row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID);

        previousState.put(
            nodeId,
            new NodeSchemaEntry(
                nodeId,
                rowKeyspace,
                ProccessStatus.valueOf(
                    row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS)),
                row.getList(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, Integer.class)));
      }
    }

    return previousState;
  }

  /**
   * TODO: Rollback on ERROR.
   *
   * <p>This function inserts the current state records into the database.
   *
   * @param currentState state gathered from {@link InstallApplication} or {@link
   *     UnInstallApplication}
   */
  private static void insertRequestToDb(
      final Session session, final Map<Integer, NodeSchemaEntry> currentState) {
    for (Map.Entry<Integer, NodeSchemaEntry> current_entry : currentState.entrySet()) {
      NodeSchemaEntry applicationEntry = current_entry.getValue();

      Insert insert =
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

      insert
          .value(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, applicationEntry.nodeId)
          .value(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, applicationEntry.keyspaceName)
          .value(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, applicationEntry.status.toString())
          .value(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, applicationEntry.waitFor);

      session.execute(insert);
    }
  }

  /**
   * This function handles the response for each class. If success it calls {@link
   * UpdateApplicationStateFormatter} otherwise it passes an error message to {@link
   * RuntimeErrorFormatter}
   *
   * @param session db session to root cassandra
   * @param currentState records generated to write to db
   * @param conflictMessage null unless a conflict occurred
   * @param noWrittenEntriesErrorMessage state message for redundant requests
   * @return json formatted response
   */
  static ResponseEntity<String> handleResponse(
      final Session session,
      final Map<Integer, NodeSchemaEntry> currentState,
      final String conflictMessage,
      final String noWrittenEntriesErrorMessage) {
    if (currentState != null && currentState.size() > 0) {
      insertRequestToDb(session, currentState);
      return new UpdateApplicationStateFormatter(currentState).format();
    } else if (currentState != null)
      return new RuntimeErrorFormatter(noWrittenEntriesErrorMessage).format();
    else return new RuntimeErrorFormatter(conflictMessage).format();
  }
}

package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.schemaFSM.NodeSchemaEntry;
import pathstore.system.schemaFSM.ProccessStatus;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter.GetApplicationStateFormatter;
import pathstoreweb.pathstoreadminpanel.services.IService;

import java.util.LinkedList;
import java.util.List;

/**
 * TODO: Maybe only include states that aren't current at the {@link ProccessStatus#REMOVED} state
 *
 * <p>TODO: Maybe use a map from nodeid, to linked list of states
 *
 * <p>This is the Application State service. It queries all nodes and what state they are in for
 * each application if applicable
 *
 * @see Constants#NODE_SCHEMAS
 * @see Constants.NODE_SCHEMAS_COLUMNS
 */
public class GetApplicationState implements IService {
  /**
   * @return json response of data queried
   * @see GetApplicationStateFormatter
   */
  @Override
  public ResponseEntity<String> response() {
    return new GetApplicationStateFormatter(this.getApplicationStates()).format();
  }

  /**
   * Queries all application states for each node into a list
   *
   * @return list of application entries
   * @see ApplicationEntry
   */
  @SuppressWarnings("ALL")
  private List<NodeSchemaEntry> getApplicationStates() {
    Session session = PathStoreCluster.getInstance().connect();

    Select queryAllNodeSchemas =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    LinkedList<NodeSchemaEntry> entries = new LinkedList<>();

    for (Row row : session.execute(queryAllNodeSchemas))
      entries.addFirst(
          new NodeSchemaEntry(
              row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID),
              row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME),
              ProccessStatus.valueOf(row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS)),
              row.getList(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, Integer.class)));

    return entries;
  }
}

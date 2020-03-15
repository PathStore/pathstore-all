package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.schemaloader.ApplicationEntry;
import pathstore.system.schemaloader.ProccessStatus;
import pathstoreweb.pathstoreadminpanel.formatter.applicationmanagement.ApplicationStateFormatter;
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
public class ApplicationState implements IService {
  /**
   * @return json response of data queried
   * @see ApplicationStateFormatter
   */
  @Override
  public String response() {
    return new ApplicationStateFormatter(this.getApplicationStates()).format();
  }

  /**
   * Queries all application states for each node into a list
   *
   * @return list of application entries
   * @see ApplicationEntry
   */
  private List<ApplicationEntry> getApplicationStates() {
    Session session = PathStoreCluster.getInstance().connect();

    Select queryAllNodeSchemas =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    LinkedList<ApplicationEntry> entries = new LinkedList<>();

    for (Row row : session.execute(queryAllNodeSchemas))
      entries.addFirst(
          new ApplicationEntry(
              row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID),
              row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME),
              ProccessStatus.valueOf(row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS)),
              row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID),
              row.getInt(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR)));

    return entries;
  }
}

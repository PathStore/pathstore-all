package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.schemaloader.ApplicationEntry;
import pathstore.system.schemaloader.ProccessStatus;
import pathstoreweb.pathstoreadminpanel.formatter.applicationmanagement.DeployApplicationFormatter;
import pathstoreweb.pathstoreadminpanel.services.IService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TODO: Create validations on input params
 *
 * <p>TODO: If installation is actually possible on that chain (check if a deletion is going on
 * currently)
 *
 * <p>This class is used for deploy an application from the root node to a set of nodes in {@link
 * #nodes} with application {@link #keyspace}
 *
 * @see DeployApplicationFormatter
 * @see Constants#NODE_SCHEMAS
 * @see Constants.NODE_SCHEMAS_COLUMNS
 */
public class DeployApplication implements IService {

  /** TODO: Fix logger. */
  private static final Logger logger = LoggerFactory.getLogger(DeployApplication.class);

  /** Application to install */
  private final String keyspace;

  /** Final nodes in the path to install on */
  private final int[] nodes;

  /** Session to {@link PathStoreCluster} */
  private final Session session;

  /**
   * @param keyspace {@link #keyspace}
   * @param nodes {@link #nodes}
   */
  public DeployApplication(final String keyspace, final int[] nodes) {
    this.keyspace = keyspace;
    this.nodes = nodes;
    this.session = PathStoreCluster.getInstance().connect();
  }

  /**
   * @return json response
   * @see DeployApplicationFormatter
   */
  @Override
  public String response() {

    int applicationInserted =
        this.insertRequestToDb(
            this.getCurrentState(this.getChildToParentMap(), this.getPreviousState()));

    return new DeployApplicationFormatter(applicationInserted).response();
  }

  /** TODO: Implement */
  private void isInstallationPossible() {}

  /**
   * Generates a map from the topology table. Keys are child id's, value is parent id
   *
   * @return map of child to parent
   */
  private Map<Integer, Integer> getChildToParentMap() {
    HashMap<Integer, Integer> childToParent = new HashMap<>();

    Select queryTopology =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.TOPOLOGY);

    for (Row row : this.session.execute(queryTopology))
      childToParent.put(
          row.getInt(Constants.TOPOLOGY_COLUMNS.NODE_ID),
          row.getInt(Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID));

    return childToParent;
  }

  /**
   * Queries the previous state of the entire topology for {@link #keyspace}. This is done to avoid
   * duplicate records being produced. See readme for additional info.
   *
   * @return map of nodeid to application entire if app is {@link #keyspace}
   */
  private Map<Integer, ApplicationEntry> getPreviousState() {
    Map<Integer, ApplicationEntry> previousState = new HashMap<>();

    Select query_node_schema =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    for (Row row : this.session.execute(query_node_schema)) {
      String rowKeyspace = row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME);

      if (rowKeyspace.equals(this.keyspace)) {
        int nodeId = row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID);

        previousState.put(
            nodeId,
            new ApplicationEntry(
                nodeId,
                rowKeyspace,
                ProccessStatus.valueOf(
                    row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS)),
                row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID),
                row.getInt(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR)));
      }
    }

    return previousState;
  }

  /**
   * Generates a current state map of node id to application state. These are the records that will
   * be written to the node schemas table
   *
   * @param childToParent {@link #getChildToParentMap()}
   * @param previousState {@link #getPreviousState()}
   * @return map of records to be written to table
   */
  private Map<Integer, ApplicationEntry> getCurrentState(
      final Map<Integer, Integer> childToParent,
      final Map<Integer, ApplicationEntry> previousState) {

    Map<Integer, ApplicationEntry> currentState = new HashMap<>();

    logger.info(
        "Gathered nodes with keyspace: " + this.keyspace + " to be: " + previousState.keySet());

    String process_uuid = UUID.randomUUID().toString();
    int numOfRowsInserted = 0;

    logger.info("Starting process batch with id: " + process_uuid);

    for (int current_node : this.nodes) {
      if (!childToParent.containsKey(current_node)) {
        logger.error("Error nodeid " + current_node + " is not a valid node in the topology");
      }

      for (int process_node = current_node;
          process_node != -1
              && !currentState.containsKey(process_node)
              && !previousState.containsKey(process_node);
          process_node = childToParent.get(process_node)) {
        numOfRowsInserted++;
        currentState.put(
            process_node,
            new ApplicationEntry(
                process_node,
                this.keyspace,
                ProccessStatus.WAITING_INSTALL,
                process_uuid,
                childToParent.get(process_node)));
      }
    }

    logger.info("Process batch finished gathering num of rows added: " + numOfRowsInserted);

    return currentState;
  }

  /**
   * TODO: Rollback on ERROR.
   *
   * <p>This function inserts the current state records into the database.
   *
   * @param currentState {@link #getCurrentState(Map, Map)}
   * @return number of records written to the database
   */
  private int insertRequestToDb(final Map<Integer, ApplicationEntry> currentState) {
    for (Map.Entry<Integer, ApplicationEntry> current_entry : currentState.entrySet()) {
      ApplicationEntry applicationEntry = current_entry.getValue();

      logger.info("Inserting: " + applicationEntry);

      Insert insert =
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

      insert
          .value(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, applicationEntry.node_id)
          .value(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, applicationEntry.keyspace_name)
          .value(
              Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS,
              applicationEntry.proccess_status.toString())
          .value(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID, applicationEntry.process_uuid)
          .value(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, applicationEntry.waiting_for);

      this.session.execute(insert);
    }

    return currentState.size();
  }
}

package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.schemaloader.ApplicationEntry;
import pathstore.system.schemaloader.ProccessStatus;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter.ConflictFormatter;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter.InstallApplicationFormatter;
import pathstoreweb.pathstoreadminpanel.services.IService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload.ApplicationManagementPayload;

/**
 * This class is used for deploy an application from the root node to a set of nodes in {@link
 * ApplicationManagementPayload#node} with application {@link
 * ApplicationManagementPayload#applicationName}
 *
 * @see InstallApplicationFormatter
 * @see Constants#NODE_SCHEMAS
 * @see Constants.NODE_SCHEMAS_COLUMNS
 */
public class InstallApplication implements IService {

  /** @see ApplicationManagementPayload */
  private final ApplicationManagementPayload applicationManagementPayload;

  /** Session to {@link PathStoreCluster} */
  private final Session session;

  /** Message that is sent when the call is essentially redundant */
  private static final String noRecordsWrittenResponse =
      "Your request cannot be processed as the set of nodes you passed already contains your specified application";

  /** This is used to set an error message if a conflict has occurred */
  private String conflictMessage = null;

  /** @param applicationManagementPayload {@link #applicationManagementPayload} */
  public InstallApplication(final ApplicationManagementPayload applicationManagementPayload) {
    this.applicationManagementPayload = applicationManagementPayload;
    this.session = PathStoreCluster.getInstance().connect();
  }

  /**
   * @return json response
   * @see InstallApplicationFormatter
   */
  @Override
  public String response() {

    Map<Integer, ApplicationEntry> currentState =
        this.getCurrentState(
            this.getChildToParentMap(),
            ApplicationUtil.getPreviousState(
                this.session, this.applicationManagementPayload.applicationName));

    if (currentState != null && currentState.size() > 0) {
      int applicationInserted = ApplicationUtil.insertRequestToDb(this.session, currentState);

      return new InstallApplicationFormatter(applicationInserted).format();
    } else if (currentState != null)
      return new ConflictFormatter(noRecordsWrittenResponse).format();
    else return new ConflictFormatter(this.conflictMessage).format();
  }

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
   * Generates a current state map of node id to application state. These are the records that will
   * be written to the node schemas table
   *
   * @param childToParent {@link #getChildToParentMap()}
   * @param previousState {@link ApplicationUtil#getPreviousState(Session, String)}
   * @return map of records to be written to table
   */
  private Map<Integer, ApplicationEntry> getCurrentState(
      final Map<Integer, Integer> childToParent,
      final Map<Integer, ApplicationEntry> previousState) {

    Map<Integer, ApplicationEntry> currentState = new HashMap<>();

    UUID processUUID = UUID.randomUUID();

    for (int currentNode : this.applicationManagementPayload.node)
      if (this.currentStateHelper(
          currentNode, processUUID, childToParent, previousState, currentState)) {

        this.conflictMessage =
            String.format(
                "There is a conflicting process uninstalling with the Process UUID is %s and we detected this conflict on Node: %d",
                processUUID, currentNode);
        return null;
      }

    return currentState;
  }

  /**
   * Loops through all nodes starting from a base node up to the root node as long as that node
   * hasn't been added to the current state from another sub job (another node in the nodes list)
   * the logic is as follows:
   *
   * <p>If a node has a previous state that is waiting_install / installing / installed. We return
   * true as if a node is at the installation state we don't need to check the parents as the
   * parents have the installed state.
   *
   * <p>If a node has a removed state we do nothing and continue up the tree.
   *
   * <p>If a node has a waiting_remove or removing we need to wait for that process job to finish,
   * Thus this job will fail and you can try again once the previous conflicting job has finished
   *
   * @param currentNode current node to check from {@link ApplicationManagementPayload#node}
   * @param processUUID our processUUID for the current job
   * @param childToParent {@link #getChildToParentMap()}
   * @param previousState {@link ApplicationUtil#getPreviousState(Session, String)}
   * @param currentState from {@link #getCurrentState(Map, Map)} used for multiple sub jobs
   * @return true if the job has a conflict else false.
   */
  private boolean currentStateHelper(
      final int currentNode,
      final UUID processUUID,
      final Map<Integer, Integer> childToParent,
      final Map<Integer, ApplicationEntry> previousState,
      final Map<Integer, ApplicationEntry> currentState) {

    for (int processingNode = currentNode;
        processingNode != -1 && !currentState.containsKey(processingNode);
        processingNode = childToParent.get(processingNode)) {

      if (previousState.containsKey(processingNode)) {
        switch (previousState.get(processingNode).proccess_status) {
            // keep going up the tree.
          case REMOVED:
            break;
            // Return true as we know that if a node is installed then all its parents are also
            // installed
          case WAITING_INSTALL:
          case INSTALLING:
          case INSTALLED:
            return false;
            // don't keep going up the tree, nothing you can do as another process is conflicting
            // with the new installation.
          default:
            return true;
        }
      }
      currentState.put(
          processingNode,
          new ApplicationEntry(
              processingNode,
              this.applicationManagementPayload.applicationName,
              ProccessStatus.WAITING_INSTALL,
              processUUID,
              Collections.singletonList(childToParent.get(processingNode))));
    }

    return false;
  }
}

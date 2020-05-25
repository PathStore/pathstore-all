package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstore.system.schemaFSM.NodeSchemaEntry;
import pathstore.system.schemaFSM.ProccessStatus;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter.UpdateApplicationStateFormatter;
import pathstoreweb.pathstoreadminpanel.services.IService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload.ModifyApplicationStatePayload;

/**
 * This class is used for deploy an application from the root node to a set of nodes in {@link
 * ModifyApplicationStatePayload#nodes} with application {@link
 * ModifyApplicationStatePayload#applicationName}
 *
 * @see UpdateApplicationStateFormatter
 * @see Constants#NODE_SCHEMAS
 * @see Constants.NODE_SCHEMAS_COLUMNS
 */
public class InstallApplication implements IService {

  /** @see ModifyApplicationStatePayload */
  private final ModifyApplicationStatePayload modifyApplicationStatePayload;

  /** Session to {@link PathStoreCluster} */
  private final Session session;

  /** Message that is sent when the call is essentially redundant */
  private static final String noRecordsWrittenResponse =
      "Your request cannot be processed as the set of nodes you passed already contains your specified application";

  /** This is used to set an error message if a conflict has occurred */
  private String conflictMessage = null;

  /** @param modifyApplicationStatePayload {@link #modifyApplicationStatePayload} */
  public InstallApplication(final ModifyApplicationStatePayload modifyApplicationStatePayload) {
    this.modifyApplicationStatePayload = modifyApplicationStatePayload;
    this.session = PathStoreCluster.getInstance().connect();
  }

  /**
   * @return json response
   * @see UpdateApplicationStateFormatter
   */
  @Override
  public ResponseEntity<String> response() {

    Map<Integer, NodeSchemaEntry> currentState =
        this.getCurrentState(
            this.getChildToParentMap(),
            ApplicationUtil.getPreviousState(
                this.session, this.modifyApplicationStatePayload.applicationName));

    return ApplicationUtil.handleResponse(
        this.session, currentState, this.conflictMessage, noRecordsWrittenResponse);
  }

  /**
   * Generates a map from the topology table. Keys are child id's, value is parent id
   *
   * @return map of child to parent
   */
  private Map<Integer, Integer> getChildToParentMap() {
    HashMap<Integer, Integer> childToParent = new HashMap<>();

    Select queryTopology =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

    for (Row row : this.session.execute(queryTopology))
      if (DeploymentProcessStatus.valueOf(
              row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS))
          == DeploymentProcessStatus.DEPLOYED)
        childToParent.put(
            row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID),
            row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID));

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
  private Map<Integer, NodeSchemaEntry> getCurrentState(
      final Map<Integer, Integer> childToParent,
      final Map<Integer, NodeSchemaEntry> previousState) {

    Map<Integer, NodeSchemaEntry> currentState = new HashMap<>();

    for (int currentNode : this.modifyApplicationStatePayload.nodes)
      if (this.currentStateHelper(currentNode, childToParent, previousState, currentState)) {

        this.conflictMessage =
            String.format("Conflict Detected this conflict on Node: %d", currentNode);
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
   * @param currentNode current node to check from {@link ModifyApplicationStatePayload#nodes}
   * @param childToParent {@link #getChildToParentMap()}
   * @param previousState {@link ApplicationUtil#getPreviousState(Session, String)}
   * @param currentState from {@link #getCurrentState(Map, Map)} used for multiple sub jobs
   * @return true if the job has a conflict else false.
   */
  private boolean currentStateHelper(
      final int currentNode,
      final Map<Integer, Integer> childToParent,
      final Map<Integer, NodeSchemaEntry> previousState,
      final Map<Integer, NodeSchemaEntry> currentState) {

    for (int processingNode = currentNode;
        processingNode != -1 && !currentState.containsKey(processingNode);
        processingNode = childToParent.get(processingNode)) {

      if (previousState.containsKey(processingNode)) {
        switch (previousState.get(processingNode).status) {
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
          new NodeSchemaEntry(
              processingNode,
              this.modifyApplicationStatePayload.applicationName,
              ProccessStatus.WAITING_INSTALL,
              Collections.singletonList(childToParent.get(processingNode))));
    }

    return false;
  }
}

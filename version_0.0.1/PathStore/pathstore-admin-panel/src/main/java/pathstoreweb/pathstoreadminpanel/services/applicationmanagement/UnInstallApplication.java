package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.schemaloader.ApplicationEntry;
import pathstore.system.schemaloader.ProccessStatus;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter.InstallApplicationFormatter;
import pathstoreweb.pathstoreadminpanel.services.IService;

import java.util.*;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload.ApplicationManagementPayload;

/**
 * This class is called when you want to remove an application from the network.
 *
 * <p>It will check to see if there are any conflicts. If there is no state will be changed. Else a
 * set of new entries will be added to the database
 */
public class UnInstallApplication implements IService {

  /** @see ApplicationManagementPayload */
  private final ApplicationManagementPayload applicationManagementPayload;

  /** Db connection session to pathstore network */
  private final Session session;

  /** @param applicationManagementPayload {@link #applicationManagementPayload} */
  public UnInstallApplication(final ApplicationManagementPayload applicationManagementPayload) {
    this.applicationManagementPayload = applicationManagementPayload;
    this.session = PathStoreCluster.getInstance().connect();
  }

  /**
   * Get the current state, if its non-null we know there is a success so we insert to the db and
   * format the output
   *
   * <p>else we return conflict
   *
   * @return response
   */
  @Override
  public String response() {
    Map<Integer, ApplicationEntry> currentState =
        this.getCurrentState(
            this.getParentToChildMap(),
            ApplicationUtil.getPreviousState(
                this.session, this.applicationManagementPayload.applicationName));

    if (currentState != null) {
      ApplicationUtil.insertRequestToDb(this.session, currentState);
      // todo
      return new InstallApplicationFormatter(currentState.size()).format();
    } else {
      // todo
      return "Conflict";
    }
  }

  /** @return map of network topology from parent to child */
  private Map<Integer, Set<Integer>> getParentToChildMap() {
    HashMap<Integer, Set<Integer>> parentToChild = new HashMap<>();

    Select queryTopology =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.TOPOLOGY);

    for (Row row : this.session.execute(queryTopology)) {
      int parentNodeId = row.getInt(Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID);
      parentToChild
          .computeIfAbsent(parentNodeId, k -> new HashSet<>())
          .add(row.getInt(Constants.TOPOLOGY_COLUMNS.NODE_ID));
    }

    return parentToChild;
  }

  /**
   * Loops through all nodes in {@link ApplicationManagementPayload#node} if any fails return null
   * else return the entry map created
   *
   * @param parentToChild {@link #getParentToChildMap()}
   * @param previousState {@link ApplicationUtil#getPreviousState(Session, String)}
   * @return map of entries to add. Null if there is a conflict
   */
  private Map<Integer, ApplicationEntry> getCurrentState(
      final Map<Integer, Set<Integer>> parentToChild,
      final Map<Integer, ApplicationEntry> previousState) {

    Map<Integer, ApplicationEntry> entryMap = new HashMap<>();

    UUID processUUID = UUID.randomUUID();

    for (int currentNode : this.applicationManagementPayload.node)
      if (this.currentStateHelper(currentNode, processUUID, parentToChild, previousState, entryMap)
          == HelperResponse.CONFLICT) return null;

    return entryMap;
  }

  /**
   * Simple enum to state the responses that can occur
   *
   * <p>Conflict for another job is currently in process (i.e a job is installing the keyspace on a
   * node that we were requested to remove from)
   *
   * <p>Already_Added is for when a node was already removed from
   *
   * <p>Added denotes we added that node for removal in the current state
   */
  private enum HelperResponse {
    CONFLICT,
    ALREADY_ADDED,
    ADDED
  }

  /**
   * If the current node that was passed has children then we recursively call this function for all
   * children. If there response is a conflict then we return a conflict to the original caller.
   * This will result in the user receiving a conflict error. (Only when another process is in the
   * middle of a job). The logic is as follows:
   *
   * <p>If a children returns already added we remove it from the children set. This is because when
   * we create that entry we don't want to say we are waiting on a node that is already removed or
   * has no record of installation / removal
   *
   * <p>Then we create the parent entry. The entry is only inserted if the previous state is
   * installed. If the previous state is waiting_remove, removing or removed we return already added
   * or if there is no prior state (thus not needing removal). If the state is Waiting_install or
   * installing there is a conflict.
   *
   * <p>If the children set is of size 0 then non of the nodes children have that keyspace installed
   * so we can treat it like a leaf node and set waiting to -1
   *
   * <p>If it is a leaf node then we create the new entry with waiting for -1. This is because there
   * are no children with that process_installed
   *
   * @param currentNode current node to check
   * @param processUUID current processUUID for this job
   * @param parentToChild {@link #getParentToChildMap()}
   * @param previousState {@link ApplicationUtil#getPreviousState(Session, String)}
   * @param currentState passed from {@link #getCurrentState(Map, Map)} to keep track of all sub
   *     jobs entries
   * @return true if there is an error else false
   */
  private HelperResponse currentStateHelper(
      final int currentNode,
      final UUID processUUID,
      final Map<Integer, Set<Integer>> parentToChild,
      final Map<Integer, ApplicationEntry> previousState,
      final Map<Integer, ApplicationEntry> currentState) {

    ApplicationEntry newEntry;

    if (parentToChild.containsKey(currentNode)) { // has children

      Set<Integer> children = new HashSet<>(parentToChild.get(currentNode));
      Set<Integer> toRemove = new HashSet<>();

      for (int childNode : children) {
        switch (this.currentStateHelper(
            childNode, processUUID, parentToChild, previousState, currentState)) {
          case CONFLICT:
            return HelperResponse.CONFLICT;
          case ALREADY_ADDED:
            toRemove.add(childNode);
        }
      }

      children.removeAll(toRemove);

      newEntry =
          new ApplicationEntry(
              currentNode,
              this.applicationManagementPayload.applicationName,
              ProccessStatus.WAITING_REMOVE,
              processUUID,
              children.size() > 0 ? new LinkedList<>(children) : Collections.singletonList(-1));
    } else
      newEntry =
          new ApplicationEntry(
              currentNode,
              this.applicationManagementPayload.applicationName,
              ProccessStatus.WAITING_REMOVE,
              processUUID,
              Collections.singletonList(-1));

    if (previousState.containsKey(currentNode))
      switch (previousState.get(currentNode).proccess_status) {
        case INSTALLED:
          currentState.put(currentNode, newEntry);
          return HelperResponse.ADDED;
        case WAITING_REMOVE:
        case REMOVING:
        case REMOVED:
          return HelperResponse.ALREADY_ADDED;
        default:
          return HelperResponse.CONFLICT;
      }
    else { // if there is no previous state for a node in a removal remove it from the list of
      // children to wait for
      return HelperResponse.ALREADY_ADDED;
    }
  }
}

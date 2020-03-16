package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.schemaloader.ApplicationEntry;
import pathstore.system.schemaloader.ProccessStatus;
import pathstoreweb.pathstoreadminpanel.formatter.applicationmanagement.DeployApplicationFormatter;
import pathstoreweb.pathstoreadminpanel.services.IService;

import java.util.*;

/**
 * This class is called when you want to remove an application from the network.
 *
 * <p>It will check to see if there are any conflicts. If there is no state will be changed. Else a
 * set of new entries will be added to the database
 */
public class RemoveApplication implements IService {

  /** Keyspace to remove */
  private final String keyspace;

  /** Nodes at the top of the tree. All nodes beneath will be removed from aswell */
  private final int[] nodes;

  /** Db connection session to pathstore network */
  private final Session session;

  /**
   * @param keyspace {@link #keyspace}
   * @param nodes {@link #nodes}
   */
  public RemoveApplication(final String keyspace, final int[] nodes) {
    this.keyspace = keyspace;
    this.nodes = nodes;
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
            ApplicationUtil.getPreviousState(this.session, this.keyspace));

    if (currentState != null) {
      ApplicationUtil.insertRequestToDb(this.session, currentState);
      // todo
      return new DeployApplicationFormatter(currentState.size()).format();
    } else {
      // todo
      return "Conflict";
    }
  }

  /** @return map of network topology from parent to child */
  private Map<Integer, LinkedList<Integer>> getParentToChildMap() {
    HashMap<Integer, LinkedList<Integer>> parentToChild = new HashMap<>();

    Select queryTopology =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.TOPOLOGY);

    for (Row row : this.session.execute(queryTopology)) {
      int parentNodeId = row.getInt(Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID);
      parentToChild
          .computeIfAbsent(parentNodeId, k -> new LinkedList<>())
          .addFirst(row.getInt(Constants.TOPOLOGY_COLUMNS.NODE_ID));
    }

    return parentToChild;
  }

  /**
   * Loops through all nodes in {@link #nodes} if any fails return null else return the entry map
   * created
   *
   * @param parentToChild {@link #getParentToChildMap()}
   * @param previousState {@link ApplicationUtil#getPreviousState(Session, String)}
   * @return map of entries to add. Null if there is a conflict
   */
  private Map<Integer, ApplicationEntry> getCurrentState(
      final Map<Integer, LinkedList<Integer>> parentToChild,
      final Map<Integer, ApplicationEntry> previousState) {

    Map<Integer, ApplicationEntry> entryMap = new HashMap<>();

    UUID processUUID = UUID.randomUUID();

    for (int currentNode : this.nodes)
      if (this.currentStateHelper(currentNode, processUUID, parentToChild, previousState, entryMap))
        return null;

    return entryMap;
  }

  /**
   * This function will setup a map of entries to execute in the db. It will start from a node and
   * work down the tree. The logic for collisions is as follows
   *
   * <p>If the node has children recursively call the function with each child id. If any child
   * fails then return true.
   *
   * <p>If the node is a leaf node (no children) then it must have a previous state which is
   * installed. As it would be impossible to remove an application that was never installed. If this
   * is true we add an entry else we return true
   *
   * @param currentNode current node to check
   * @param processUUID current processUUID for this job
   * @param parentToChild {@link #getParentToChildMap()}
   * @param previousState {@link ApplicationUtil#getPreviousState(Session, String)}
   * @param currentState passed from {@link #getCurrentState(Map, Map)} to keep track of all sub
   *     jobs entries
   * @return true if there is an error else false
   */
  private boolean currentStateHelper(
      final int currentNode,
      final UUID processUUID,
      final Map<Integer, LinkedList<Integer>> parentToChild,
      final Map<Integer, ApplicationEntry> previousState,
      final Map<Integer, ApplicationEntry> currentState) {

    if (parentToChild.containsKey(currentNode)) { // has children

      List<Integer> children = parentToChild.get(currentNode);

      for (int childNode : children)
        if (this.currentStateHelper(
            childNode, processUUID, parentToChild, previousState, currentState)) return true;

      currentState.put(
          currentNode,
          new ApplicationEntry(
              currentNode, this.keyspace, ProccessStatus.WAITING_REMOVE, processUUID, children));
    } else { // leaf node
      if (previousState.containsKey(currentNode)
          && previousState.get(currentNode).proccess_status == ProccessStatus.INSTALLED)
        currentState.put(
            currentNode,
            new ApplicationEntry(
                currentNode,
                this.keyspace,
                ProccessStatus.WAITING_REMOVE,
                processUUID,
                Collections.singletonList(-1)));
      else return true;
    }

    return false;
  }
}

package pathstore.system.schemaFSM;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.tables.NodeSchemaEntry;
import pathstore.common.tables.NodeSchemaProcessStatus;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import java.util.*;

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
public class PathStoreMasterSchemaServer implements Runnable {

  /** Logger */
  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreMasterSchemaServer.class);

  /** Session used to interact with pathstore */
  private final Session session = PathStoreCluster.getDaemonInstance().connect();

  /**
   * This daemon will transition rows that are WAITING_INSTALL to INSTALLING. The steps are:
   *
   * <p>(1): Query all rows from the Node Schemas table and filter them into sets of Installed,
   * Installing and Waiting
   *
   * <p>(2): Iterate over all waiting entries and transition them to installing iff they're who
   * they're waiting for are in the finished entry set
   */
  @Override
  public void run() {
    while (true) {
      // (1)
      Select selectAllNodeSchemaRecords =
          QueryBuilder.select()
              .all()
              .from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

      // installation
      Map<String, Set<Integer>> finished = new HashMap<>();
      Map<String, Set<NodeSchemaEntry>> waiting = new HashMap<>();

      // removal
      Map<String, Set<NodeSchemaEntry>> waitingRemoval = new HashMap<>();
      Map<String, Set<Integer>> completeSet = new HashMap<>();

      for (Row row : this.session.execute(selectAllNodeSchemaRecords)) {
        NodeSchemaEntry entry = NodeSchemaEntry.fromRow(row);

        switch (entry.nodeSchemaProcessStatus) {
          case INSTALLED:
            finished.computeIfAbsent(entry.keyspaceName, k -> new HashSet<>());
            finished.get(entry.keyspaceName).add(entry.nodeId);
            break;
          case WAITING_INSTALL:
            waiting.computeIfAbsent(entry.keyspaceName, k -> new HashSet<>());
            waiting.get(entry.keyspaceName).add(entry);
            break;
          case WAITING_REMOVE:
            waitingRemoval.computeIfAbsent(entry.keyspaceName, k -> new HashSet<>());
            waitingRemoval.get(entry.keyspaceName).add(entry);
            break;
        }

        // update complete set
        completeSet.computeIfAbsent(entry.keyspaceName, k -> new HashSet<>());
        completeSet.get(entry.keyspaceName).add(entry.nodeId);
      }

      // (2)
      this.deploymentTransition(waiting, finished);

      // (3)
      this.removalTransition(waitingRemoval, completeSet);

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        logger.error(e);
      }
    }
  }

  /**
   * Simple function that takes a set of waiting entries and confirms their pre-conditions. If they
   * hold then we can transition them to the next state
   *
   * @param waiting waiting entry set per keyspace
   * @param finished finished id set per keyspace
   */
  private void deploymentTransition(
      final Map<String, Set<NodeSchemaEntry>> waiting, final Map<String, Set<Integer>> finished) {

    waiting.forEach(
        (k, v) ->
            v.stream()
                .filter(
                    i ->
                        i.waitFor.equals(Collections.singletonList(-1))
                            || (finished.get(k) != null && finished.get(k).containsAll(i.waitFor)))
                .forEach(e -> this.transition(e, NodeSchemaProcessStatus.INSTALLING)));
  }

  /**
   * Simple function that takes a set of waiting entries that all their preconditions do not have
   * entries in the table. If so transition them to removing
   *
   * @param waiting waiting entry set per keyspace
   * @param completeSet of entries per keyspace
   */
  private void removalTransition(
      final Map<String, Set<NodeSchemaEntry>> waiting,
      final Map<String, Set<Integer>> completeSet) {

    waiting.forEach(
        (k, v) ->
            v.stream()
                .filter(
                    i ->
                        i.waitFor.equals(Collections.singletonList(-1))
                            || (completeSet.get(k) != null
                                && Collections.disjoint(completeSet.get(k), i.waitFor)))
                .forEach(e -> this.transition(e, NodeSchemaProcessStatus.REMOVING)));
  }

  /**
   * This function takes an entry as input and updates the process status to INSTALLING
   *
   * @param entry entry to transition
   */
  private void transition(
      final NodeSchemaEntry entry, final NodeSchemaProcessStatus nodeSchemaProcessStatus) {

    logger.info(
        String.format(
            "%s node %d for keyspace %s",
            nodeSchemaProcessStatus.toString(), entry.nodeId, entry.keyspaceName));

    Update transitionUpdate =
        QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    transitionUpdate
        .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, entry.nodeId))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, entry.keyspaceName))
        .with(
            QueryBuilder.set(
                Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, nodeSchemaProcessStatus.toString()));

    this.session.execute(transitionUpdate);
  }
}

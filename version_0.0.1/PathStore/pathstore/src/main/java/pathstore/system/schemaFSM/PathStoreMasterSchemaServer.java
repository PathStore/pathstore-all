package pathstore.system.schemaFSM;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;

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
  private final Session session = PathStoreCluster.getInstance().connect();

  /**
   * TODO: Removal
   *
   * <p>This daemon will transition rows that are WAITING_INSTALL to INSTALLING. The steps are:
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

      Map<String, Set<Integer>> finished = new HashMap<>();
      Map<String, Set<NodeSchemaEntry>> waiting = new HashMap<>();

      for (Row row : this.session.execute(selectAllNodeSchemaRecords)) {
        ProccessStatus status =
            ProccessStatus.valueOf(row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS));
        int nodeId = row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID);

        String keyspaceName = row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME);

        if (status == ProccessStatus.INSTALLED) {
          finished.computeIfAbsent(keyspaceName, k -> new HashSet<>());
          finished.get(keyspaceName).add(nodeId);
        } else if (status == ProccessStatus.WAITING_INSTALL) {
          List<Integer> waitFor =
              row.getList(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, Integer.class);

          waiting.computeIfAbsent(keyspaceName, k -> new HashSet<>());
          waiting.get(keyspaceName).add(new NodeSchemaEntry(nodeId, keyspaceName, status, waitFor));
        }
      }

      // (2)
      this.transitionIfApplicable(waiting, finished);

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
  private void transitionIfApplicable(
      final Map<String, Set<NodeSchemaEntry>> waiting, final Map<String, Set<Integer>> finished) {

    waiting.forEach(
        (k, v) ->
            v.stream()
                .filter(
                    i ->
                        i.waitFor.equals(Collections.singletonList(-1))
                            || (finished.get(k) != null && finished.get(k).containsAll(i.waitFor)))
                .forEach(this::transition));
  }

  /**
   * This function takes an entry as input and updates the process status to INSTALLING
   *
   * @param entry entry to transition
   */
  private void transition(final NodeSchemaEntry entry) {

    logger.info(
        String.format(
            "Installing node %d to %s for keyspace %s",
            entry.nodeId, ProccessStatus.INSTALLING, entry.keyspaceName));

    Update transitionUpdate =
        QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    transitionUpdate
        .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, entry.nodeId))
        .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, entry.keyspaceName))
        .with(
            QueryBuilder.set(
                Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS,
                ProccessStatus.INSTALLING.toString()));

    this.session.execute(transitionUpdate);
  }
}

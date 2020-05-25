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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class PathStoreMasterSchemaServer extends Thread {

  /** TODO: Comment */
  private static final class NodeSchemasEntry {
    public final int nodeId;
    public final String keyspaceName;
    public final ProccessStatus status;
    public final List<Integer> waitFor;

    private NodeSchemasEntry(
        final int nodeId,
        final String keyspaceName,
        final ProccessStatus status,
        final List<Integer> waitFor) {
      this.nodeId = nodeId;
      this.keyspaceName = keyspaceName;
      this.status = status;
      this.waitFor = waitFor;
    }

    @Override
    public String toString() {
      return "NodeSchemasEntry{"
          + "nodeId="
          + nodeId
          + ", keyspaceName='"
          + keyspaceName
          + '\''
          + ", status="
          + status
          + ", waitFor="
          + waitFor
          + '}';
    }
  }

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

      logger.debug("Checking Master");

      // (1)
      Select selectAllNodeSchemaRecords =
          QueryBuilder.select()
              .all()
              .from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

      Set<Integer> finished = new HashSet<>();
      Set<NodeSchemasEntry> waiting = new HashSet<>();

      for (Row row : this.session.execute(selectAllNodeSchemaRecords)) {
        ProccessStatus status =
            ProccessStatus.valueOf(row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS));
        int nodeId = row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID);

        if (status == ProccessStatus.INSTALLED) {
          finished.add(nodeId);
          continue;
        }

        String keyspaceName = row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME);
        List<Integer> waitFor = row.getList(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, Integer.class);

        waiting.add(new NodeSchemasEntry(nodeId, keyspaceName, status, waitFor));
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
   * @param waiting waiting entry set
   * @param finished finished id set
   */
  private void transitionIfApplicable(
      final Set<NodeSchemasEntry> waiting, final Set<Integer> finished) {

    logger.debug(String.format("Waiting set: %s, finished set %s", waiting, finished));

    waiting.stream()
        .filter(
            i -> i.waitFor.equals(Collections.singletonList(-1)) || finished.containsAll(i.waitFor))
        .forEach(this::transition);
  }

  /**
   * This function takes an entry as input and updates the process status to INSTALLING
   *
   * @param entry entry to transition
   */
  private void transition(final NodeSchemasEntry entry) {

    logger.info(
        String.format(
            "Transition node %d to %s for keyspace %s",
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

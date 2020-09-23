package pathstore.common.tables;

import com.datastax.driver.core.Row;

import java.util.List;

import static pathstore.common.Constants.NODE_SCHEMAS_COLUMNS.*;

/**
 * This class is used to define an entry within the node schemas table
 *
 * <p>TODO: Make sure every reference to the node schemas table uses the build from row
 */
public final class NodeSchemaEntry {
  public static NodeSchemaEntry fromRow(final Row row) {
    return new NodeSchemaEntry(
        row.getInt(NODE_ID),
        row.getString(KEYSPACE_NAME),
        NodeSchemaProcessStatus.valueOf(row.getString(PROCESS_STATUS)),
        row.getList(WAIT_FOR, Integer.class));
  }

  /** Node id of the node to install or remove on */
  public final int nodeId;

  /** What keyspace to perform the operation with */
  public final String keyspaceName;

  /** What is the current process status */
  public final NodeSchemaProcessStatus nodeSchemaProcessStatus;

  /** list of nodes that need to complete their operation before executing this operation */
  public final List<Integer> waitFor;

  /**
   * @param nodeId {@link #nodeId}
   * @param keyspaceName {@link #keyspaceName}
   * @param nodeSchemaProcessStatus {@link #nodeSchemaProcessStatus}
   * @param waitFor {@link #waitFor}
   */
  private NodeSchemaEntry(
      final int nodeId,
      final String keyspaceName,
      final NodeSchemaProcessStatus nodeSchemaProcessStatus,
      final List<Integer> waitFor) {
    this.nodeId = nodeId;
    this.keyspaceName = keyspaceName;
    this.nodeSchemaProcessStatus = nodeSchemaProcessStatus;
    this.waitFor = waitFor;
  }
}

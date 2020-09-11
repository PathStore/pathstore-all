package pathstore.system.schemaFSM;

import java.util.List;

/** This class is used to define an entry within the node schemas table */
public final class NodeSchemaEntry {
  /** Node id of the node to install or remove on */
  public final int nodeId;

  /** What keyspace to perform the operation with */
  public final String keyspaceName;

  /** What is the current process status */
  public final ProccessStatus status;

  /** list of nodes that need to complete their operation before executing this operation */
  public final List<Integer> waitFor;

  /**
   * @param nodeId {@link #nodeId}
   * @param keyspaceName {@link #keyspaceName}
   * @param status {@link #status}
   * @param waitFor {@link #waitFor}
   */
  public NodeSchemaEntry(
      final int nodeId,
      final String keyspaceName,
      final ProccessStatus status,
      final List<Integer> waitFor) {
    this.nodeId = nodeId;
    this.keyspaceName = keyspaceName;
    this.status = status;
    this.waitFor = waitFor;
  }
}

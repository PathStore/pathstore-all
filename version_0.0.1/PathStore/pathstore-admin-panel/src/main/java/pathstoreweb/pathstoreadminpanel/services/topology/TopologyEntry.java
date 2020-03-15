package pathstoreweb.pathstoreadminpanel.services.topology;

/**
 * Simple topology entry with node id and parent node id
 *
 * @see NetworkTopology
 */
public class TopologyEntry {
  /** Node id and parent node id */
  public final int nodeId, parentNodeId;

  /**
   * @param nodeId {@link #nodeId}
   * @param parentNodeId {@link #parentNodeId}
   */
  TopologyEntry(final int nodeId, final int parentNodeId) {
    this.nodeId = nodeId;
    this.parentNodeId = parentNodeId;
  }
}

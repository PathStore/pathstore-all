package pathstore.system.deployment.deploymentFSM;

import java.util.UUID;

/** This class is used to denote a deployment record entry in the deployment table */
public class DeploymentEntry {

  /** Node id of the new node */
  public final int newNodeId;

  /** Parent id of the new node */
  public final int parentNodeId;

  /** What is the node's status of deployment */
  public final DeploymentProcessStatus deploymentProcessStatus;

  /** Who is the new node waiting for */
  public final int waitFor;

  /** What server is the new node being deployed on */
  public final UUID serverUUID;

  /**
   * @param newNodeId {@link #newNodeId}
   * @param parentNodeId {@link #parentNodeId}
   * @param deploymentProcessStatus {@link #deploymentProcessStatus}
   * @param waitFor {@link #waitFor}
   * @param serverUUID {@link #serverUUID}
   */
  public DeploymentEntry(
      final int newNodeId,
      final int parentNodeId,
      final DeploymentProcessStatus deploymentProcessStatus,
      final int waitFor,
      final UUID serverUUID) {
    this.newNodeId = newNodeId;
    this.parentNodeId = parentNodeId;
    this.deploymentProcessStatus = deploymentProcessStatus;
    this.waitFor = waitFor;
    this.serverUUID = serverUUID;
  }

  /**
   * @return all data printed to screen. Used for debugging
   */
  @Override
  public String toString() {
    return "DeploymentEntry{"
        + "newNodeId="
        + newNodeId
        + ", parentNodeId="
        + parentNodeId
        + ", deploymentProcessStatus="
        + deploymentProcessStatus
        + ", waitFor="
        + waitFor
        + ", serverUUID="
        + serverUUID
        + '}';
  }
}

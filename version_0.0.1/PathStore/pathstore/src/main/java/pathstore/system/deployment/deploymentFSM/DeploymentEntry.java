package pathstore.system.deployment.deploymentFSM;

import java.util.UUID;

public class DeploymentEntry {
  public final int newNodeId;

  public final int parentNodeId;

  public final DeploymentProcessStatus deploymentProcessStatus;

  public final int waitFor;

  public final UUID serverUUID;

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

    @Override
    public String toString() {
        return "DeploymentEntry{" +
                "newNodeId=" + newNodeId +
                ", parentNodeId=" + parentNodeId +
                ", deploymentProcessStatus=" + deploymentProcessStatus +
                ", waitFor=" + waitFor +
                ", serverUUID=" + serverUUID +
                '}';
    }
}

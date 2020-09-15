package pathstore.system.deployment.deploymentFSM;

import com.datastax.driver.core.Row;

import java.util.List;
import java.util.UUID;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;

/** This class is used to denote a deployment record entry in the deployment table */
public class DeploymentEntry {

  /**
   * Build entry from row
   *
   * @param row row to build from
   * @return built entry
   */
  public static DeploymentEntry fromRow(final Row row) {
    return new DeploymentEntry(
        row.getInt(NEW_NODE_ID),
        row.getInt(PARENT_NODE_ID),
        DeploymentProcessStatus.valueOf(row.getString(PROCESS_STATUS)),
        row.getList(WAIT_FOR, Integer.class),
        UUID.fromString(row.getString(SERVER_UUID)));
  }

  /** Node id of the new node */
  public final int newNodeId;

  /** Parent id of the new node */
  public final int parentNodeId;

  /** What is the node's status of deployment */
  public final DeploymentProcessStatus deploymentProcessStatus;

  /** Who is the new node waiting for */
  public final List<Integer> waitFor;

  /** What server is the new node being deployed on */
  public final UUID serverUUID;

  /**
   * @param newNodeId {@link #newNodeId}
   * @param parentNodeId {@link #parentNodeId}
   * @param deploymentProcessStatus {@link #deploymentProcessStatus}
   * @param waitFor {@link #waitFor}
   * @param serverUUID {@link #serverUUID}
   */
  private DeploymentEntry(
      final int newNodeId,
      final int parentNodeId,
      final DeploymentProcessStatus deploymentProcessStatus,
      final List<Integer> waitFor,
      final UUID serverUUID) {
    this.newNodeId = newNodeId;
    this.parentNodeId = parentNodeId;
    this.deploymentProcessStatus = deploymentProcessStatus;
    this.waitFor = waitFor;
    this.serverUUID = serverUUID;
  }

  /** @return all data printed to screen. Used for debugging */
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

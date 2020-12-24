package pathstoreweb.pathstoreadminpanel.services.deployment;

/** This represents a individual record supplied by the frontend */
public class DeploymentRecord {
  /** ParentId of the new node */
  public int parentId;

  /** The new node's id */
  public int newNodeId;

  /** Where to install it */
  public String serverUUID;

  /** @return string for debug purposes */
  @Override
  public String toString() {
    return "AddDeploymentRecordPayload{"
        + "parentId="
        + parentId
        + ", newNodeId="
        + newNodeId
        + ", serverUUID="
        + serverUUID
        + '}';
  }
}

package pathstoreweb.pathstoreadminpanel.services.deployment.payload;

import pathstoreweb.pathstoreadminpanel.services.deployment.validator.*;

import java.util.List;

/** Payload for the add deployment record endpoint. This is used to validate input */
public final class AddDeploymentRecordPayload {

  @EmptyCheck(message = "You did not supply any records, you must supply at least one record")
  @ServerUUIDDuplicates(message = "You have entered duplicate serverUUID's")
  @NodeIdDuplicates(message = "You have entered duplicate newNodeId's")
  @UniquenessNodeId(
      message = "You have entered a node id that has already been used in the topology")
  @UniquenessServerUUID(
      message = "You have entered a serverUUID that has already been used in the topology")
  @ValidParentId(
      message =
          "You have a parent id that is not an existing node or part of your new network topology")
  @NewNodeIdParentIdCheck(message = "You cannot have matching parentId and newNodeId values")
  @ServerUUIDValidator(message = "You must only use existing serverUUID's")
  public List<DeploymentRecord> records;

  /** This represents a individual record supplied by the frontend */
  public static class DeploymentRecord {
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
}

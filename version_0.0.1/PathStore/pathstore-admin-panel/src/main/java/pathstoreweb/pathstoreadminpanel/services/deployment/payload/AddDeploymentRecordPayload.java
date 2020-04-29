package pathstoreweb.pathstoreadminpanel.services.deployment.payload;

import pathstoreweb.pathstoreadminpanel.services.deployment.validator.RecordValidator;

import java.util.List;

public class AddDeploymentRecordPayload {

  // TODO: split into multiple checkers
  @RecordValidator(message = "Known error occured")
  public List<DeploymentRecord> records;

  public static class DeploymentRecord {
    public int parentId;

    public int newNodeId;

    public String serverUUID;

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

package pathstoreweb.pathstoreadminpanel.services.deployment.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import pathstore.system.deployment.deploymentFSM.DeploymentEntry;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

import java.util.List;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;

public class AddDeploymentRecordsFormatter implements IFormatter {

  private final List<DeploymentEntry> entries;

  public AddDeploymentRecordsFormatter(final List<DeploymentEntry> entries) {
    this.entries = entries;
  }

  @Override
  public String format() {

    JSONArray array = new JSONArray();

    for (DeploymentEntry entry : this.entries)
      array.put(
          new JSONObject()
              .put(NEW_NODE_ID, entry.newNodeId)
              .put(PARENT_NODE_ID, entry.parentNodeId)
              .put(PROCESS_STATUS, entry.deploymentProcessStatus.toString())
              .put(WAIT_FOR, entry.waitFor)
              .put(SERVER_UUID, entry.serverUUID.toString()));

    return array.toString();
  }
}

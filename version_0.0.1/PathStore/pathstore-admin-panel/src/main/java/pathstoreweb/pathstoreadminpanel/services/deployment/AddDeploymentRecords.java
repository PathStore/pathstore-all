package pathstoreweb.pathstoreadminpanel.services.deployment;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.deployment.deploymentFSM.DeploymentEntry;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.formatter.AddDeploymentRecordsFormatter;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.AddDeploymentRecordPayload;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;
import static pathstore.common.Constants.SERVERS_COLUMNS.SERVER_UUID;

public class AddDeploymentRecords implements IService {

  private final AddDeploymentRecordPayload payload;

  public AddDeploymentRecords(final AddDeploymentRecordPayload payload) {
    this.payload = payload;
  }

  @Override
  public String response() {
    return new AddDeploymentRecordsFormatter(this.writeEntries()).format();
  }

  private List<DeploymentEntry> writeEntries() {

    Session session = PathStoreCluster.getInstance().connect();

    LinkedList<DeploymentEntry> linkedList = new LinkedList<>();

    for (AddDeploymentRecordPayload.DeploymentRecord record : payload.records) {

      DeploymentEntry entry =
          new DeploymentEntry(
              record.newNodeId,
              record.parentId,
              DeploymentProcessStatus.WAITING_DEPLOYMENT,
              record.parentId,
              UUID.fromString(record.serverUUID));

      Insert insert =
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT)
              .value(NEW_NODE_ID, entry.newNodeId)
              .value(PARENT_NODE_ID, entry.parentNodeId)
              .value(PROCESS_STATUS, entry.deploymentProcessStatus.toString())
              .value(WAIT_FOR, entry.parentNodeId)
              .value(SERVER_UUID, entry.serverUUID.toString());

      session.execute(insert);

      linkedList.addFirst(entry);
    }

    return linkedList;
  }
}

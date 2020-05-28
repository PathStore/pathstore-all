package pathstoreweb.pathstoreadminpanel.services.deployment;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.deployment.deploymentFSM.DeploymentEntry;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.formatter.DeploymentRecordsFormatter;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.AddDeploymentRecordPayload;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;
import static pathstore.common.Constants.SERVERS_COLUMNS.SERVER_UUID;

/** This class is used when the user passes a set of records to add */
public class AddDeploymentRecords implements IService {

  /**
   * Valid payload given by user
   *
   * @see AddDeploymentRecordPayload
   */
  private final AddDeploymentRecordPayload payload;

  /** @param payload {@link #payload} */
  public AddDeploymentRecords(final AddDeploymentRecordPayload payload) {
    this.payload = payload;
  }

  /** @return {@link DeploymentRecordsFormatter#format()} */
  @Override
  public ResponseEntity<String> response() {
    return new DeploymentRecordsFormatter(this.writeEntries()).format();
  }

  /**
   * Writes all entries that the user has provided with the {@link
   * DeploymentProcessStatus#WAITING_DEPLOYMENT} status. The DeploymentFSM will handle the
   * transitions of states and the slave deployment server will install when their state hits {@link
   * DeploymentProcessStatus#DEPLOYING}
   *
   * @return list of entries written. This is so the user can see the output
   */
  private List<DeploymentEntry> writeEntries() {

    Session session = PathStoreCluster.getInstance().connect();

    LinkedList<DeploymentEntry> linkedList = new LinkedList<>();

    for (AddDeploymentRecordPayload.DeploymentRecord record : payload.records) {

      DeploymentEntry entry =
          new DeploymentEntry(
              record.newNodeId,
              record.parentId,
              DeploymentProcessStatus.WAITING_DEPLOYMENT,
              Collections.singletonList(record.parentId),
              UUID.fromString(record.serverUUID));

      Insert insert =
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT)
              .value(NEW_NODE_ID, entry.newNodeId)
              .value(PARENT_NODE_ID, entry.parentNodeId)
              .value(PROCESS_STATUS, entry.deploymentProcessStatus.toString())
              .value(WAIT_FOR, entry.waitFor)
              .value(SERVER_UUID, entry.serverUUID.toString());

      session.execute(insert);

      linkedList.addFirst(entry);
    }

    return linkedList;
  }
}

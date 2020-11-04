package pathstoreweb.pathstoreadminpanel.services.deployment;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.formatter.DeploymentRecordsFormatter;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.AddDeploymentRecordPayload;

import java.util.Collections;
import java.util.LinkedList;

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
    this.writeEntries();

    return new DeploymentRecordsFormatter(new LinkedList<>()).format();
  }

  /**
   * Writes all entries that the user has provided with the {@link
   * DeploymentProcessStatus#WAITING_DEPLOYMENT} status. The DeploymentFSM will handle the
   * transitions of states and the slave deployment server will install when their state hits {@link
   * DeploymentProcessStatus#DEPLOYING}
   *
   * @return list of entries written. This is so the user can see the output
   */
  private void writeEntries() {

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    for (DeploymentRecord record : payload.records) {

      Insert insert =
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT)
              .value(NEW_NODE_ID, record.newNodeId)
              .value(PARENT_NODE_ID, record.parentId)
              .value(PROCESS_STATUS, DeploymentProcessStatus.WAITING_DEPLOYMENT.toString())
              .value(WAIT_FOR, new LinkedList<>(Collections.singleton(record.parentId)))
              .value(SERVER_UUID, record.serverUUID);

      session.execute(insert);
    }
  }
}

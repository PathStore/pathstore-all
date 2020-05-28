package pathstoreweb.pathstoreadminpanel.services.deployment;

import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.DeleteDeploymentRecordPayload;

import java.util.Collections;
import java.util.LinkedList;

public class DeleteDeploymentRecords implements IService {

  private final DeleteDeploymentRecordPayload payload;

  public DeleteDeploymentRecords(final DeleteDeploymentRecordPayload payload) {
    this.payload = payload;
  }

  @Override
  public ResponseEntity<String> response() {
    this.delete();
    return new ResponseEntity<>("Deleted", HttpStatus.OK);
  }

  private void delete() {
    Insert insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT)
            .value(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID, payload.record.newNodeId)
            .value(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID, payload.record.parentId)
            .value(
                Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS,
                DeploymentProcessStatus.WAITING_REMOVAL.toString())
            .value(
                Constants.DEPLOYMENT_COLUMNS.WAIT_FOR, new LinkedList<>(Collections.singleton(-1)))
            .value(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID, this.payload.record.serverUUID);

    PathStoreCluster.getInstance().connect().execute(insert);
  }
}

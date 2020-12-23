package pathstoreweb.pathstoreadminpanel.services.deployment;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.UpdateDeploymentRecordPayload;

/**
 * This service is used to take a failed deployment record and update it to deploying after the user
 * has fixed the cause of failure
 */
public class UpdateDeploymentRecord implements IService {

  /** Valid payload */
  private final UpdateDeploymentRecordPayload payload;

  /** @param payload {@link #payload} */
  public UpdateDeploymentRecord(final UpdateDeploymentRecordPayload payload) {
    this.payload = payload;
  }

  /**
   * TODO create formatter
   *
   * @return updated
   */
  @Override
  public ResponseEntity<String> response() {
    this.update();

    return new ResponseEntity<>(new JSONObject().toString(), HttpStatus.OK);
  }

  /** Updates the given record to the status of deploying */
  private void update() {
    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    update
        .where(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID, payload.record.newNodeId))
        .and(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID, payload.record.parentId))
        .and(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID, payload.record.serverUUID))
        .with(
            QueryBuilder.set(
                Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS,
                DeploymentProcessStatus.DEPLOYING.toString()));

    session.execute(update);
  }
}

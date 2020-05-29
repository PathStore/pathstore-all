package pathstoreweb.pathstoreadminpanel.services.deployment.payload;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.validator.ValidatedPayload;

import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.UPDATE_DEPLOYMENT_RECORD_PAYLOAD.*;

/** This payload is used to pass a record that has valid and update it to deploying */
public final class UpdateDeploymentRecordPayload extends ValidatedPayload {

  /**
   * deployment record based by user. This record must be a failed record in order to pass
   * validation
   */
  public AddDeploymentRecordPayload.DeploymentRecord record;

  /**
   * Validity check
   *
   * <p>(1): Enter a valid failed record
   *
   * @return true iff all validity checks pass
   */
  @Override
  protected String[] calculateErrors() {

    String[] errors = {INVALID_FAILED_ENTRY};

    Session session = PathStoreCluster.getInstance().connect();

    // (1)
    Select select =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    select.where(
        QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID, this.record.parentId));

    for (Row row : session.execute(select)) {
      int newNodeId = row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID);
      String serverUUID = row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID);
      DeploymentProcessStatus status =
          DeploymentProcessStatus.valueOf(
              row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS));

      if (newNodeId == this.record.newNodeId
          && serverUUID.equals(this.record.serverUUID)
          && status == DeploymentProcessStatus.FAILED) {
        errors[0] = null;
        break;
      }
    }
    return errors;
  }
}

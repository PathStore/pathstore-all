package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload;

import java.util.HashSet;
import java.util.Set;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.validator.ValidatedPayload;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID;
import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS;
import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.MODIFY_APPLICATION_STATE_PAYLOAD.*;

/**
 * Payload for when a user wants to remove an application or install an application on a list of
 * nodes
 */
public final class ModifyApplicationStatePayload extends ValidatedPayload {

  /**
   * Name of application to perform operation on.
   *
   * <p>Ensures that the application is actually a valid application
   */
  public final String applicationName;

  /**
   * List of nodes to perform the operation on
   *
   * <p>Ensures the nodes are valid nodes within the topology
   */
  public final Set<Integer> nodes;

  /**
   * @param application_name {@link #applicationName}
   * @param nodes {@link #nodes}
   */
  public ModifyApplicationStatePayload(final String application_name, final Set<Integer> nodes) {
    this.applicationName = application_name;
    this.nodes = nodes;
  }

  /**
   * TODO: Update ever .one is implemented
   *
   * <p>Validity checking:
   *
   * <p>(1): Wrong submission format
   *
   * <p>(2): Check {@link #applicationName} exists
   *
   * <p>(3): {@link #nodes} is non-empty
   *
   * <p>(4): {@link #nodes} all exist with the deployment table and are at the DEPLOYED state
   *
   * @return list of errors all null if no errors occured
   */
  @Override
  protected String[] calculateErrors() {

    // (1)
    if (this.bulkNullCheck(this.applicationName, this.nodes))
      return new String[] {WRONG_SUBMISSION_FORMAT};

    String[] errors = {APP_DOESNT_EXIST, null, null};

    Session session = PathStoreCluster.getInstance().connect();

    Select selectApps =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);
    selectApps.where(QueryBuilder.eq(Constants.APPS_COLUMNS.KEYSPACE_NAME, this.applicationName));

    // (2) If the app exists this result set is of size one
    for (Row row : session.execute(selectApps)) errors[0] = null;

    // (3)
    if (this.nodes.size() == 0) errors[1] = NODES_EMPTY;

    // (4)
    Select queryDeployment =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

    Set<Integer> nodeExists = new HashSet<>();

    for (Row row : session.execute(queryDeployment)) {
      int currentNode = row.getInt(NEW_NODE_ID);
      if (nodes.contains(currentNode)
          && DeploymentProcessStatus.valueOf(row.getString(PROCESS_STATUS))
              == DeploymentProcessStatus.DEPLOYED) nodeExists.add(currentNode);
    }

    if (nodeExists.size() != this.nodes.size()) errors[2] = NODES_DONT_EXIST;

    return errors;
  }
}

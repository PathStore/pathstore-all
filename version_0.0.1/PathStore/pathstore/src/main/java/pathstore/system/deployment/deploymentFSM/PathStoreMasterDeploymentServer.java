package pathstore.system.deployment.deploymentFSM;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * This class is used to read the deployment table and determine when to transition nodes.
 *
 * <p>Once a record has been transitions to deploying the slave deployment server will then execute
 * the deployment step. Once this step occurs it can either transition to deployed or failed. If
 * failed the administrator of the network will need to login to the web page in order to see the
 * error and request a retry, this retry rewrites the record of that node to deploying instead of
 * failed. This cycle could possibly continue until all errors are resolved. In order to avoid such
 * errors the administrator should follow the server setup guide on our github page.
 */
public class PathStoreMasterDeploymentServer extends Thread {

  /** Logger */
  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreMasterDeploymentServer.class);

  /** Session used to interact with pathstore */
  private final Session session = PathStoreCluster.getInstance().connect();

  /**
   * This daemon will transition rows that are WAITING_DEPLOYMENT to DEPLOYING. The steps are:
   *
   * <p>(1): Query all records from the deployment table and store them into a set of node_id's
   * denoted as finished and a set of DeploymentEntry for the waiting records
   *
   * <p>(2): Iterate over all waiting deployment records and if the node they're waiting for has
   * finished transition that node
   *
   * <p>(3): Iterate over all waiting removal records and if the node they're waiting for has been
   * removed transition that node
   */
  @Override
  public void run() {
    while (true) {
      // (1)
      Select selectAllDeploymentRecords =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      // Deployment
      Set<Integer> deployed = new HashSet<>();
      Set<DeploymentEntry> waitingDeployment = new HashSet<>();

      // Removal
      Set<DeploymentEntry> waitingRemoval = new HashSet<>();
      Set<Integer> completeSet = new HashSet<>();

      for (Row row : this.session.execute(selectAllDeploymentRecords)) {
        DeploymentProcessStatus status =
            DeploymentProcessStatus.valueOf(
                row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS));
        int newNodeId = row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID);

        // Setup filterable sets
        switch (status) {
          case DEPLOYED:
            deployed.add(newNodeId);
            break;
          case WAITING_DEPLOYMENT:
            waitingDeployment.add(
                new DeploymentEntry(
                    newNodeId,
                    row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID),
                    status,
                    row.getList(Constants.DEPLOYMENT_COLUMNS.WAIT_FOR, Integer.class),
                    UUID.fromString(row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID))));
            break;
          case WAITING_REMOVAL:
            waitingRemoval.add(
                new DeploymentEntry(
                    newNodeId,
                    row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID),
                    status,
                    row.getList(Constants.DEPLOYMENT_COLUMNS.WAIT_FOR, Integer.class),
                    UUID.fromString(row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID))));
            break;
        }

        // add to complete set
        completeSet.add(newNodeId);
      }

      // (2)
      waitingDeployment.stream()
          .filter(i -> deployed.containsAll(i.waitFor))
          .forEach(this::transitionDeploy);

      // (3) If all nodes i is waiting for aren't presented in the record set
      waitingRemoval.stream()
          .filter(i -> Collections.disjoint(i.waitFor, completeSet))
          .forEach(this::transitionRemoval);

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        logger.error(e);
      }
    }
  }

  /**
   * Transition the node in the table from waiting to deploying
   *
   * @param entry entry to transition
   */
  private void transitionDeploy(final DeploymentEntry entry) {

    logger.info(
        String.format(
            "Deploying a new child to %d with id %d", entry.parentNodeId, entry.newNodeId));

    PathStoreDeploymentUtils.updateState(entry, DeploymentProcessStatus.DEPLOYING);
  }

  /**
   * Transition the node in the table from waiting to deploying
   *
   * @param entry entry to transition
   */
  private void transitionRemoval(final DeploymentEntry entry) {

    logger.info(
        String.format("%d is removing the child node %d", entry.parentNodeId, entry.newNodeId));

    PathStoreDeploymentUtils.updateState(entry, DeploymentProcessStatus.REMOVING);
  }
}

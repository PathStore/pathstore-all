package pathstore.system.deployment.deploymentFSM;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * TODO; Handle removal of nodes
 *
 * <p>This class is used to read the deployment table and determine when to transition nodes.
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
   * TODO: Removal
   *
   * <p>This daemon will transition rows that are WAITING_DEPLOYMENT to DEPLOYING. The steps are:
   *
   * <p>(1): Query all records from the deployment table and store them into a set of node_id's
   * denoted as finished and a set of DeploymentEntry for the waiting records
   *
   * <p>(2): Iterate over all waiting records and if the node they're waiting for has finished
   * transition that node
   */
  @Override
  public void run() {
    while (true) {

      logger.debug("Deployment run");

      // (1)
      Select selectAllDeploymentRecords =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      Set<Integer> finished = new HashSet<>();
      Set<DeploymentEntry> waiting = new HashSet<>();

      for (Row row : this.session.execute(selectAllDeploymentRecords)) {
        DeploymentProcessStatus status =
            DeploymentProcessStatus.valueOf(
                row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS));
        int newNodeId = row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID);

        if (status == DeploymentProcessStatus.DEPLOYED) finished.add(newNodeId);
        else if (status == DeploymentProcessStatus.WAITING_DEPLOYMENT)
          waiting.add(
              new DeploymentEntry(
                  newNodeId,
                  row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID),
                  status,
                  row.getList(Constants.DEPLOYMENT_COLUMNS.WAIT_FOR, Integer.class),
                  UUID.fromString(row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID))));
      }

      // (2)
      waiting.stream().filter(i -> finished.containsAll(i.waitFor)).forEach(this::transition);

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
  private void transition(final DeploymentEntry entry) {

    logger.info(
        String.format(
            "Deploying a new child to %d with id %d", entry.parentNodeId, entry.newNodeId));

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    update
        .where(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID, entry.newNodeId))
        .and(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID, entry.parentNodeId))
        .and(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID, entry.serverUUID.toString()))
        .with(
            QueryBuilder.set(
                Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS,
                DeploymentProcessStatus.DEPLOYING.toString()));

    this.session.execute(update);
  }
}

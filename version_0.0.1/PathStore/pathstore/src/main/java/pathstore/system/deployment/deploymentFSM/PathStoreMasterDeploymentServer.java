package pathstore.system.deployment.deploymentFSM;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;

import java.util.*;
import java.util.stream.Collectors;

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

  /** Gather all deployment records into a set of ananlysis */
  @Override
  @SuppressWarnings("ALL")
  public void run() {
    while (true) {

      Session clientSession = PathStoreCluster.getInstance().connect();

      Set<DeploymentEntry> entrySet = new HashSet<>();

      Select selectAllFromDeployment =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      for (Row row : clientSession.execute(selectAllFromDeployment))
        entrySet.add(
            new DeploymentEntry(
                row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID),
                row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID),
                DeploymentProcessStatus.valueOf(
                    row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS)),
                row.getInt(Constants.DEPLOYMENT_COLUMNS.WAIT_FOR),
                UUID.fromString(row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID))));

      this.update(
          this.parseByState(entrySet, DeploymentProcessStatus.WAITING_DEPLOYMENT),
          this.parseByStateToNewNodeID(entrySet, DeploymentProcessStatus.DEPLOYED));

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Produces a set of entries based on a certain status
   *
   * @param entries entries to filter
   * @param state how to filter them
   * @return set of entries that hold the filter state
   */
  private Set<DeploymentEntry> parseByState(
      final Set<DeploymentEntry> entries, final DeploymentProcessStatus state) {
    return entries.stream()
        .filter(i -> i.deploymentProcessStatus == state)
        .collect(Collectors.toSet());
  }

  /**
   * Produces a set of node id's based on a deployment state
   *
   * @param entries entries to filter
   * @param state how to filter them
   * @return set of node id's that are at a certain state
   */
  private Set<Integer> parseByStateToNewNodeID(
      final Set<DeploymentEntry> entries, final DeploymentProcessStatus state) {
    return entries.stream()
        .filter(i -> i.deploymentProcessStatus == state)
        .map(i -> i.newNodeId)
        .collect(Collectors.toSet());
  }

  /**
   * Logic to transition nodes
   *
   * <p>If an entry is waiting for -1 or the entry that its waiting for is finished transition the
   * given node to deploying
   *
   * @param waiting set of waiting nodes
   * @param deployed set of id's that are finished
   */
  private void update(final Set<DeploymentEntry> waiting, final Set<Integer> deployed) {
    for (DeploymentEntry entry : waiting)
      if (entry.waitFor == -1 || deployed.contains(entry.waitFor)) this.transition(entry);
  }

  /**
   * Transition the node in the table from waiting to deploying
   *
   * @param entry entry to transition
   */
  private void transition(final DeploymentEntry entry) {
    Session clientSession = PathStoreCluster.getInstance().connect();

    System.out.println(
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

    clientSession.execute(update);
  }
}

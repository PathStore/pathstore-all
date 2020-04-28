package pathstore.system.deployment.deploymentFSM;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.jcraft.jsch.JSchException;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.Role;
import pathstore.system.deployment.commands.CommandError;
import pathstore.system.deployment.commands.ICommand;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstore.system.deployment.utilities.StartupUTIL;

import java.util.UUID;

/**
 * This slave deployment server will listen for a deploying state record in the deployment table
 * with their specified node id. Once they find such a record they will attempt to deploy a new
 * pathstore node on that machine. If it succeeds they will update the record to deployed or if it
 * fails they will write failed.
 *
 * <p>In order for the administrator of the system to minimize the chance of failure they should
 * follow the server setup guide on our github page to ensure that all required pre-requisites are
 * installed before attempting to deploy a pathstore instance to said server
 */
public class PathStoreSlaveDeploymentServer extends Thread {

  private static final String branch = "pathstore_init_script";
  private static final int cassandraPort = 9052;
  private static final int rmiPort = 1099;

  @Override
  public void run() {
    while (true) {

      Session session = PathStoreCluster.getInstance().connect();

      Select selectAllDeployment =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      for (Row row : session.execute(selectAllDeployment)) {
        int parentNodeId = row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID);

        DeploymentProcessStatus status =
            DeploymentProcessStatus.valueOf(
                row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS));

        if (parentNodeId == PathStoreProperties.getInstance().NodeID
            && status == DeploymentProcessStatus.DEPLOYING) {

          DeploymentEntry entry =
              new DeploymentEntry(
                  row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID),
                  parentNodeId,
                  DeploymentProcessStatus.valueOf(
                      row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS)),
                  row.getInt(Constants.DEPLOYMENT_COLUMNS.WAIT_FOR),
                  UUID.fromString(row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID)));

          Select getServer =
              QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);
          getServer.where(
              QueryBuilder.eq(Constants.SERVERS_COLUMNS.SERVER_UUID, entry.serverUUID.toString()));

          for (Row serverRow : session.execute(getServer)) {
            this.deploy(
                entry,
                serverRow.getString(Constants.SERVERS_COLUMNS.IP),
                serverRow.getString(Constants.SERVERS_COLUMNS.USERNAME),
                serverRow.getString(Constants.SERVERS_COLUMNS.PASSWORD));
          }
        }
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void deploy(
      final DeploymentEntry entry, final String ip, final String username, final String password) {
    System.out.println(
        String.format("Deploying %d from node %d", entry.newNodeId, entry.parentNodeId));

    try {
      SSHUtil sshUtil = new SSHUtil(ip, username, password, 22);

      System.out.println("Connection established to new node");

      for (ICommand command :
          StartupUTIL.initList(
              sshUtil,
              ip,
              branch,
              entry.newNodeId,
              entry.parentNodeId,
              Role.SERVER,
              "127.0.0.1",
              rmiPort,
              PathStoreProperties.getInstance().IP,
              rmiPort,
              "127.0.0.1",
              cassandraPort,
              PathStoreProperties.getInstance().IP,
              cassandraPort,
              "../docker-files/pathstore/pathstore.properties")) {
        System.out.println(command);
        try {
          command.execute();
        } catch (CommandError commandError) {
          System.out.println("[ERROR] " + commandError.errorMessage);
          this.updateState(entry, DeploymentProcessStatus.FAILED);
          break;
        }
      }

      sshUtil.disconnect();

      System.out.println("Successfully deployed");
      this.updateState(entry, DeploymentProcessStatus.DEPLOYED);

    } catch (JSchException e) {
      System.err.println("Could not connect to new node");
      this.updateState(entry, DeploymentProcessStatus.FAILED);
    }
  }

  private void updateState(final DeploymentEntry entry, final DeploymentProcessStatus status) {
    Session clientSession = PathStoreCluster.getInstance().connect();

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    update
        .where(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID, entry.newNodeId))
        .and(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID, entry.parentNodeId))
        .and(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID, entry.serverUUID.toString()))
        .with(QueryBuilder.set(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS, status.toString()));

    clientSession.execute(update);
  }
}

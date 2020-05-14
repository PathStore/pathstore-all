package pathstore.system.deployment.deploymentFSM;

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
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;
import pathstore.system.deployment.commands.CommandError;
import pathstore.system.deployment.commands.ICommand;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstore.system.deployment.utilities.StartupUTIL;
import pathstore.system.schemaFSM.PathStoreSlaveSchemaServer;

import java.util.UUID;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;
import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.SERVER_UUID;
import static pathstore.common.Constants.SERVERS_COLUMNS.*;

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

  /** Logger */
  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreSlaveSchemaServer.class);

  private static final int cassandraPort = 9052;
  private static final int rmiPort = 1099;

  /**
   * Iterate over all deployment records. Find a record that has the parent node id as the current
   * node. If the state of that node is deploying then start the deployment process
   */
  @Override
  public void run() {
    while (true) {
      logger.debug("Slave Schema run");

      Session session = PathStoreCluster.getInstance().connect();

      Select selectAllDeployment =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      // Query all rows from the deployment table
      for (Row row : session.execute(selectAllDeployment)) {
        int parentNodeId = row.getInt(PARENT_NODE_ID);

        DeploymentProcessStatus status =
            DeploymentProcessStatus.valueOf(row.getString(PROCESS_STATUS));

        // If the record is set to deploying and the parentNodeId is this node, start deployment
        if (parentNodeId == PathStoreProperties.getInstance().NodeID
            && status == DeploymentProcessStatus.DEPLOYING) {

          DeploymentEntry entry =
              new DeploymentEntry(
                  row.getInt(NEW_NODE_ID),
                  parentNodeId,
                  DeploymentProcessStatus.valueOf(row.getString(PROCESS_STATUS)),
                  row.getInt(WAIT_FOR),
                  UUID.fromString(row.getString(SERVER_UUID)));

          Select getServer =
              QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);
          getServer.where(QueryBuilder.eq(SERVER_UUID, entry.serverUUID.toString()));

          for (Row serverRow : session.execute(getServer))
            this.deploy(
                entry,
                serverRow.getString(IP),
                serverRow.getString(USERNAME),
                serverRow.getString(PASSWORD));
        }
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Deploy a future node as a child to the current node
   *
   * @param entry record that triggered the deployment process
   * @param ip ip of the server where the future node is going to be installed on
   * @param username username of the server
   * @param password password of the server
   * @see StartupUTIL#initList(SSHUtil, String, int, int, Role, String, int, String, int, String,
   *     int, String, int)
   */
  private void deploy(
      final DeploymentEntry entry, final String ip, final String username, final String password) {

    logger.info(String.format("Deploying %d from node %d", entry.newNodeId, entry.parentNodeId));

    try {
      SSHUtil sshUtil = new SSHUtil(ip, username, password, 22);

      logger.debug("Connection established to new node");

      try {

        // Get a list of commands based on what information the current node has in the properties
        // file and what the new node id was written to the deployment table
        for (ICommand command :
            StartupUTIL.initList(
                sshUtil,
                ip,
                entry.newNodeId,
                entry.parentNodeId,
                Role.SERVER,
                "127.0.0.1",
                rmiPort,
                PathStoreProperties.getInstance().ExternalAddress,
                rmiPort,
                "127.0.0.1",
                cassandraPort,
                PathStoreProperties.getInstance().ExternalAddress,
                cassandraPort)) {

          // Inform the user what command is being executed
          logger.info(command.toString());

          command.execute();
        }

        logger.info("Successfully deployed");
        this.updateState(entry, DeploymentProcessStatus.DEPLOYED);

      } catch (CommandError commandError) { // there was an error with a given command

        logger.error("Deployment failed");
        logger.error(commandError.errorMessage);
        this.updateState(entry, DeploymentProcessStatus.FAILED);

      } finally {
        sshUtil.disconnect();
      }

    } catch (JSchException e) { // the connection information given is not valid

      logger.error("Could not connect to new node");
      this.updateState(entry, DeploymentProcessStatus.FAILED);
    }
  }

  /**
   * Updates a records state to either failed or deployed based on the result of deployment
   *
   * @param entry record that triggered deployment
   * @param status status to update entry to
   */
  private void updateState(final DeploymentEntry entry, final DeploymentProcessStatus status) {
    Session clientSession = PathStoreCluster.getInstance().connect();

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    update
        .where(QueryBuilder.eq(NEW_NODE_ID, entry.newNodeId))
        .and(QueryBuilder.eq(PARENT_NODE_ID, entry.parentNodeId))
        .and(QueryBuilder.eq(SERVER_UUID, entry.serverUUID.toString()))
        .with(QueryBuilder.set(PROCESS_STATUS, status.toString()));

    clientSession.execute(update);
  }
}

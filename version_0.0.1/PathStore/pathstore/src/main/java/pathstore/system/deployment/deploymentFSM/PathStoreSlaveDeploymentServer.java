package pathstore.system.deployment.deploymentFSM;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.jcraft.jsch.JSchException;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreThreadManager;
import pathstore.common.Role;
import pathstore.common.logger.LoggerLevel;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;
import pathstore.system.PathStorePriviledgedCluster;
import pathstore.system.PathStorePushServer;
import pathstore.system.deployment.commands.CommandError;
import pathstore.system.deployment.commands.ICommand;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstore.system.deployment.utilities.StartupUTIL;
import pathstore.util.SchemaInfo;

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
public class PathStoreSlaveDeploymentServer implements Runnable {

  /** Logger */
  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreSlaveDeploymentServer.class);

  /** Cassandra port which is statically declared (temporary) */
  private static final int cassandraPort = 9052;

  /** Session used to interact with pathstore */
  private final Session session = PathStoreCluster.getInstance().connect();

  /** Node id so you don't need to query the properties file every run */
  private final int nodeId = PathStoreProperties.getInstance().NodeID;

  /** Reference to the sub process thread pool */
  private final PathStoreThreadManager subProcessManager =
      PathStoreThreadManager.subProcessInstance();

  /**
   * The daemon is used to deploy new children nodes for a given node. The steps to install a new
   * child node are as follows:
   *
   * <p>(1): Query the deployment records with the parent node id of the current node
   *
   * <p>(2): For all records retrieved that are of DEPLOYING status query their respective
   * server_uuid's and transition their row to PROCESSING_DEPLOYING
   *
   * <p>(3): After their server information is retrieved queue them for deployment
   */
  @Override
  public void run() {
    while (true) {
      // (1)
      Select selectAllDeployment =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
      selectAllDeployment.where(QueryBuilder.eq(PARENT_NODE_ID, this.nodeId));

      // Query all rows from the deployment table
      for (Row row : this.session.execute(selectAllDeployment)) {
        DeploymentProcessStatus currentStatus =
            DeploymentProcessStatus.valueOf(row.getString(PROCESS_STATUS));

        if (currentStatus != DeploymentProcessStatus.DEPLOYING
            && currentStatus != DeploymentProcessStatus.REMOVING) continue;

        String serverUUID = row.getString(SERVER_UUID);

        Select queryServer =
            QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);
        queryServer.where(QueryBuilder.eq(SERVER_UUID, serverUUID));

        // (2) && (3)
        for (Row serverRow : this.session.execute(queryServer))
          this.spawnSubProcess(
              new DeploymentEntry(
                  row.getInt(NEW_NODE_ID),
                  this.nodeId,
                  currentStatus,
                  row.getList(WAIT_FOR, Integer.class),
                  UUID.fromString(serverUUID)),
              serverRow.getString(IP),
              serverRow.getString(USERNAME),
              serverRow.getString(PASSWORD),
              serverRow.getInt(SSH_PORT),
              serverRow.getInt(RMI_PORT));
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        this.logger.error(e);
      }
    }
  }

  /**
   * This function will transition the row start to processing_deploying to denote that the node is
   * currently being handled. It will then start the deployment process on a seperate thread
   *
   * @param entry entry to use
   * @param ip ip of server
   * @param username username of server
   * @param password password of server
   * @param sshPort ssh port of server
   * @param rmiPort rmi port for server pathstore instance
   */
  private void spawnSubProcess(
      final DeploymentEntry entry,
      final String ip,
      final String username,
      final String password,
      final int sshPort,
      final int rmiPort) {

    // Determine how to update and what function to execute based on the status
    DeploymentProcessStatus newStatus;
    Runnable r;

    if (entry.deploymentProcessStatus == DeploymentProcessStatus.DEPLOYING) {
      newStatus = DeploymentProcessStatus.PROCESSING_DEPLOYING;
      r = () -> deploy(entry, ip, username, password, sshPort, rmiPort);
    } else {
      newStatus = DeploymentProcessStatus.PROCESSING_REMOVING;
      r = () -> remove(entry, ip, username, password, sshPort);
    }

    // (2)
    PathStoreDeploymentUtils.updateState(entry, newStatus);

    // (3)
    this.subProcessManager.spawn(r);
  }

  /**
   * Deploy a future node as a child to the current node
   *
   * @param entry record that triggered the deployment process
   * @param ip ip of the server where the future node is going to be installed on
   * @param username username of the server
   * @param password password of the server
   * @param sshPort ssh port for connection
   * @param rmiPort rmi port for rmi server
   * @see StartupUTIL#initDeploymentList(SSHUtil, String, int, int, Role, String, int, String, int,
   *     String, int, String, int)
   */
  private void deploy(
      final DeploymentEntry entry,
      final String ip,
      final String username,
      final String password,
      final int sshPort,
      final int rmiPort) {

    this.logger.info(
        String.format("Deploying %d from node %d", entry.newNodeId, entry.parentNodeId));

    try {
      SSHUtil sshUtil = new SSHUtil(ip, username, password, sshPort);

      this.logger.debug("Connection established to new node");

      try {

        // Get a list of commands based on what information the current node has in the properties
        // file and what the new node id was written to the deployment table
        for (ICommand command :
            StartupUTIL.initDeploymentList(
                sshUtil,
                ip,
                entry.newNodeId,
                entry.parentNodeId,
                Role.SERVER,
                "127.0.0.1",
                rmiPort,
                PathStoreProperties.getInstance().ExternalAddress,
                PathStoreProperties.getInstance().RMIRegistryPort,
                "127.0.0.1",
                cassandraPort,
                PathStoreProperties.getInstance().ExternalAddress,
                PathStoreProperties.getInstance().CassandraPort)) {

          // Inform the user what command is being executed
          this.logger.info(
              PathStoreDeploymentUtils.formatParallelMessages(entry.newNodeId, command.toString()));

          command.execute();
        }

        this.logger.info(String.format("Successfully deployed node with id %d", nodeId));
        PathStoreDeploymentUtils.updateState(entry, DeploymentProcessStatus.DEPLOYED);

      } catch (CommandError commandError) { // there was an error with a given command

        this.logger.error("Deployment failed");
        this.logger.error(commandError.errorMessage);
        PathStoreDeploymentUtils.updateState(entry, DeploymentProcessStatus.FAILED);

      } finally {
        sshUtil.disconnect();
      }

    } catch (JSchException e) { // the connection information given is not valid

      this.logger.error("Could not connect to new node");
      PathStoreDeploymentUtils.updateState(entry, DeploymentProcessStatus.FAILED);
    }
  }

  /**
   * This function removes a child node. It will also clear out all original finals
   *
   * <p>Steps:
   *
   * <p>Un-deploys node then removes:
   *
   * <p>Deployment record, available log dates, logs, App records
   *
   * @param entry entry to delete
   * @param ip ip to connect to
   * @param username username to connect
   * @param password password to connect
   * @param sshPort ssh port for server
   */
  private void remove(
      final DeploymentEntry entry,
      final String ip,
      final String username,
      final String password,
      final int sshPort) {

    this.logger.info(String.format("Starting removal of node %d", entry.newNodeId));

    try {
      SSHUtil sshUtil = new SSHUtil(ip, username, password, sshPort);

      this.logger.debug(String.format("Connection establish to %d", entry.newNodeId));

      try {

        this.logger.info("Starting the push of all dirty data from child");

        Session child = StartupUTIL.createCluster(ip, cassandraPort).connect();

        PathStorePushServer.push(
            child, PathStorePriviledgedCluster.getInstance().connect(), new SchemaInfo(child));

        child.close();

        this.logger.info("Finished pushing all dirty data");

        for (ICommand command : StartupUTIL.initUnDeploymentList(sshUtil)) {
          this.logger.info(
              PathStoreDeploymentUtils.formatParallelMessages(entry.newNodeId, command.toString()));
          command.execute();
        }

        this.logger.info(
            String.format("Deleting Available log dates and logs for node %d", entry.newNodeId));

        Select getAvailableLogDates =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.AVAILABLE_LOG_DATES);
        getAvailableLogDates.where(
            QueryBuilder.eq(Constants.AVAILABLE_LOG_DATES_COLUMNS.NODE_ID, entry.newNodeId));

        for (Row availableLogDateRow : this.session.execute(getAvailableLogDates)) {

          // Delete date record for available dates
          Delete availableLogDatesDelete =
              QueryBuilder.delete()
                  .from(Constants.PATHSTORE_APPLICATIONS, Constants.AVAILABLE_LOG_DATES);

          String date = availableLogDateRow.getString(Constants.AVAILABLE_LOG_DATES_COLUMNS.DATE);

          availableLogDatesDelete
              .where(
                  QueryBuilder.eq(Constants.AVAILABLE_LOG_DATES_COLUMNS.NODE_ID, entry.newNodeId))
              .and(QueryBuilder.eq(Constants.AVAILABLE_LOG_DATES_COLUMNS.DATE, date));

          this.session.execute(availableLogDatesDelete);

          // For all log levels delete all logs with the given date above
          for (LoggerLevel level : LoggerLevel.values()) {
            Select getLogs =
                QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);
            getLogs
                .where(QueryBuilder.eq(Constants.LOGS_COLUMNS.NODE_ID, entry.newNodeId))
                .and(QueryBuilder.eq(Constants.LOGS_COLUMNS.DATE, date))
                .and(QueryBuilder.eq(Constants.LOGS_COLUMNS.LOG_LEVEL, level.toString()));

            for (Row logRow : this.session.execute(getLogs)) {
              Delete logDelete =
                  QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);
              logDelete
                  .where(QueryBuilder.eq(Constants.LOGS_COLUMNS.NODE_ID, entry.newNodeId))
                  .and(QueryBuilder.eq(Constants.LOGS_COLUMNS.DATE, date))
                  .and(QueryBuilder.eq(Constants.LOGS_COLUMNS.LOG_LEVEL, level.toString()))
                  .and(
                      QueryBuilder.eq(
                          Constants.LOGS_COLUMNS.COUNT,
                          logRow.getInt(Constants.LOGS_COLUMNS.COUNT)));

              this.session.execute(logDelete);
            }
          }
        }

        this.logger.info(
            String.format("Deleting node schema records for node %d", entry.newNodeId));

        Select nodeSchemas =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
        nodeSchemas.where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, entry.newNodeId));

        for (Row row : this.session.execute(nodeSchemas)) {
          Delete nodeSchemaDelete =
              QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
          nodeSchemaDelete
              .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, entry.newNodeId))
              .and(
                  QueryBuilder.eq(
                      Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME,
                      row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME)));

          this.session.execute(nodeSchemaDelete);
        }

        this.logger.info(String.format("Deleting deployment record for node %d", entry.newNodeId));

        Delete deploymentDelete =
            QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
        deploymentDelete
            .where(QueryBuilder.eq(PARENT_NODE_ID, entry.parentNodeId))
            .and(QueryBuilder.eq(NEW_NODE_ID, entry.newNodeId));

        this.session.execute(deploymentDelete);

        this.logger.info(String.format("Successfully un-deployed node %d", entry.newNodeId));

      } catch (CommandError commandError) {
        PathStoreDeploymentUtils.updateState(entry, DeploymentProcessStatus.DEPLOYED);
        this.logger.error(
            "The un-deployment has failed. The state is updated to DEPLOYED. But the node may not function properly. This is to allow you to re-try the un-deployment after the underlying issue has been solved.");
        this.logger.error(commandError.errorMessage);
      } finally {
        sshUtil.disconnect();
      }
    } catch (JSchException e) {
      PathStoreDeploymentUtils.updateState(entry, DeploymentProcessStatus.DEPLOYED);
      this.logger.error(
          String.format("Could not connect to node %d to un-deploy it", entry.newNodeId));
    }
  }
}

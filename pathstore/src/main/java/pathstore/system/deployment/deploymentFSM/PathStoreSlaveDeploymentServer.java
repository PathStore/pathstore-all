/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package pathstore.system.deployment.deploymentFSM;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.jcraft.jsch.JSchException;
import pathstore.client.PathStoreSession;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreThreadManager;
import pathstore.common.Role;
import pathstore.common.tables.DeploymentEntry;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstore.common.tables.ServerEntry;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.deployment.commands.CommandError;
import pathstore.system.deployment.commands.ICommand;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstore.system.deployment.utilities.StartupUTIL;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID;
import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.SERVER_UUID;

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

  /** Session used to interact with pathstore */
  private final PathStoreSession session = PathStorePrivilegedCluster.getDaemonInstance().psConnect();

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
    try {
      while (true) {
        // (1)
        Select selectAllDeployment =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
        selectAllDeployment.where(QueryBuilder.eq(PARENT_NODE_ID, this.nodeId));

        // Query all rows from the deployment table
        for (Row row : this.session.execute(selectAllDeployment)) {
          DeploymentEntry entry = DeploymentEntry.fromRow(row);

          if (entry.deploymentProcessStatus != DeploymentProcessStatus.DEPLOYING
              && entry.deploymentProcessStatus != DeploymentProcessStatus.REMOVING) continue;

          Select queryServer =
              QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);
          queryServer.where(QueryBuilder.eq(SERVER_UUID, entry.serverUUID.toString()));

          // (2) && (3)
          for (Row serverRow : this.session.execute(queryServer))
            this.spawnSubProcess(entry, ServerEntry.fromRow(serverRow));
        }

        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          this.logger.error(e);
        }
      }
    } catch (Exception e) {
      logger.error(e);
    }
  }

  /**
   * This function will transition the row start to processing_deploying to denote that the node is
   * currently being handled. It will then start the deployment process on a seperate thread
   *
   * @param deploymentEntry entry to use
   */
  private void spawnSubProcess(
      final DeploymentEntry deploymentEntry, final ServerEntry serverEntry) {

    // Determine how to update and what function to execute based on the status
    DeploymentProcessStatus newStatus;
    Runnable r;

    if (deploymentEntry.deploymentProcessStatus == DeploymentProcessStatus.DEPLOYING) {
      newStatus = DeploymentProcessStatus.PROCESSING_DEPLOYING;
      r = () -> deploy(deploymentEntry, serverEntry);
    } else {
      newStatus = DeploymentProcessStatus.PROCESSING_REMOVING;
      r = () -> remove(deploymentEntry, serverEntry);
    }

    // (2)
    PathStoreDeploymentUtils.updateState(deploymentEntry, newStatus);

    // (3)
    this.subProcessManager.spawn(r);
  }

  /**
   * Deploy a future node as a child to the current node
   *
   * @param deploymentEntry record that triggered the deployment process
   * @see StartupUTIL#initDeploymentList(SSHUtil, String, int, int, Role, String, int, String, int,
   *     String, int, String, int)
   */
  private void deploy(final DeploymentEntry deploymentEntry, final ServerEntry serverEntry) {

    this.logger.info(
        String.format(
            "Deploying %d from node %d", deploymentEntry.newNodeId, deploymentEntry.parentNodeId));

    try {

      SSHUtil sshUtil = SSHUtil.buildFromServerEntry(serverEntry);

      this.logger.debug("Connection established to new node");

      try {

        // Get a list of commands based on what information the current node has in the properties
        // file and what the new node id was written to the deployment table
        for (ICommand command :
            StartupUTIL.initDeploymentList(
                sshUtil,
                serverEntry.ip,
                deploymentEntry.newNodeId,
                deploymentEntry.parentNodeId,
                Role.SERVER,
                "127.0.0.1",
                serverEntry.grpcPort,
                PathStoreProperties.getInstance().ExternalAddress,
                PathStoreProperties.getInstance().GRPCPort,
                "127.0.0.1",
                serverEntry.cassandraPort,
                PathStoreProperties.getInstance().ExternalAddress,
                PathStoreProperties.getInstance().CassandraPort)) {

          // Inform the user what command is being executed
          this.logger.info(
              PathStoreDeploymentUtils.formatParallelMessages(
                  deploymentEntry.newNodeId, command.toString()));

          command.execute();
        }

        this.logger.info(
            String.format("Successfully deployed node with id %d", deploymentEntry.newNodeId));
        PathStoreDeploymentUtils.updateState(deploymentEntry, DeploymentProcessStatus.DEPLOYED);

      } catch (Exception e) { // there was an error with a given command

        this.logger.error("Deployment failed");
        e.printStackTrace(); // TODO: Send this to the log
        PathStoreDeploymentUtils.updateState(deploymentEntry, DeploymentProcessStatus.FAILED);

      } finally {
        sshUtil.disconnect();
      }

    } catch (JSchException e) { // the connection information given is not valid
      this.logger.error("Could not connect to new node");
      e.printStackTrace();
      PathStoreDeploymentUtils.updateState(deploymentEntry, DeploymentProcessStatus.FAILED);
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
   * @param deploymentEntry entry to delete
   */
  private void remove(final DeploymentEntry deploymentEntry, final ServerEntry serverEntry) {

    this.logger.info(String.format("Starting removal of node %d", deploymentEntry.newNodeId));

    try {
      SSHUtil sshUtil = SSHUtil.buildFromServerEntry(serverEntry);

      this.logger.debug(String.format("Connection establish to %d", deploymentEntry.newNodeId));

      try {

        for (ICommand command :
            StartupUTIL.initUnDeploymentList(sshUtil, deploymentEntry, serverEntry)) {
          this.logger.info(
              PathStoreDeploymentUtils.formatParallelMessages(
                  deploymentEntry.newNodeId, command.toString()));
          command.execute();
        }

        this.logger.info(
            String.format("Successfully un-deployed node %d", deploymentEntry.newNodeId));

      } catch (CommandError commandError) {
        PathStoreDeploymentUtils.updateState(deploymentEntry, DeploymentProcessStatus.DEPLOYED);
        this.logger.error(
            "The un-deployment has failed. The state is updated to DEPLOYED. But the node may not function properly. This is to allow you to re-try the un-deployment after the underlying issue has been solved.");
        this.logger.error(commandError.errorMessage);
      } finally {
        sshUtil.disconnect();
      }
    } catch (JSchException e) {
      PathStoreDeploymentUtils.updateState(deploymentEntry, DeploymentProcessStatus.DEPLOYED);
      this.logger.error(
          String.format("Could not connect to node %d to un-deploy it", deploymentEntry.newNodeId));
    }
  }
}

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

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreSession;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentEntry;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstore.system.PathStorePrivilegedCluster;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;

/**
 * This utility class is used to share functionality between {@link PathStoreMasterDeploymentServer}
 * and {@link PathStoreSlaveDeploymentServer}. It also gives functionality to nodes to write task
 * completions on startup
 */
public class PathStoreDeploymentUtils {

  /**
   * Write that a task has been completed in the startup sequence
   *
   * @param session session of local db
   * @param task task to write to
   */
  public static void writeTaskDone(final Session session, final int task) {
    session.execute(
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_STARTUP)
            .value(Constants.LOCAL_STARTUP_COLUMNS.TASK_DONE, task));
  }

  /**
   * Updates a records state to either failed or deployed based on the result of deployment
   *
   * @param entry record that triggered deployment
   * @param status status to update entry to
   */
  public static void updateState(
      final DeploymentEntry entry, final DeploymentProcessStatus status) {
    PathStoreSession clientSession = PathStorePrivilegedCluster.getDaemonInstance().psConnect();

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    update
        .where(QueryBuilder.eq(NEW_NODE_ID, entry.newNodeId))
        .and(QueryBuilder.eq(PARENT_NODE_ID, entry.parentNodeId))
        .and(QueryBuilder.eq(SERVER_UUID, entry.serverUUID.toString()))
        .with(QueryBuilder.set(PROCESS_STATUS, status.toString()));

    clientSession.execute(update);
  }

  /**
   * Simple function to denote how the ICommand messages will be processed
   *
   * @param nodeId node id
   * @param messages messages
   * @return formatted string
   */
  public static String formatParallelMessages(final int nodeId, final String messages) {
    return String.format("Node %d is executing command %s", nodeId, messages);
  }
}

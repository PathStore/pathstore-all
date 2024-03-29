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
package pathstore.system.deployment.commands;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import lombok.RequiredArgsConstructor;
import pathstore.client.PathStoreSession;
import pathstore.common.Constants;
import pathstore.common.tables.NodeSchemaEntry;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.logging.LoggerLevel;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID;
import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID;

/**
 * This class is used to delete the node history of a given node.
 *
 * <p>It will remove: logging information, application records, and deployment records
 *
 * @see pathstore.system.schemaFSM.PathStoreSlaveSchemaServer
 * @see pathstore.system.schemaFSM.PathStoreMasterSchemaServer
 * @implNote This is only to be run on the server side as it uses {@link
 *     PathStorePrivilegedCluster#getDaemonInstance()}. This function does not connect to the child
 *     node at any point.
 */
@RequiredArgsConstructor
public class DeleteNodeHistory implements ICommand {
  /** Logger to display what is occuring */
  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(DeleteNodeHistory.class);

  /** Child node id who is being removed */
  private final int newNodeId;

  /** Node id if the parent node (the node running this application) */
  private final int parentNodeId;

  /**
   * Removes all records associated with {@link #newNodeId} from
   * pathstore_applications.{available_log_dates, logs, node_schemas, deployment}
   */
  @Override
  public void execute() {
    PathStoreSession session = PathStorePrivilegedCluster.getDaemonInstance().psConnect();

    this.logger.info(
        String.format("Deleting Available log dates and logs for node %d", this.newNodeId));

    Select getAvailableLogDates =
        QueryBuilder.select()
            .all()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.AVAILABLE_LOG_DATES);
    getAvailableLogDates.where(
        QueryBuilder.eq(Constants.AVAILABLE_LOG_DATES_COLUMNS.NODE_ID, this.newNodeId));

    for (Row availableLogDateRow : session.execute(getAvailableLogDates)) {

      // Delete date record for available dates
      Delete availableLogDatesDelete =
          QueryBuilder.delete()
              .from(Constants.PATHSTORE_APPLICATIONS, Constants.AVAILABLE_LOG_DATES);

      String date = availableLogDateRow.getString(Constants.AVAILABLE_LOG_DATES_COLUMNS.DATE);

      availableLogDatesDelete
          .where(QueryBuilder.eq(Constants.AVAILABLE_LOG_DATES_COLUMNS.NODE_ID, this.newNodeId))
          .and(QueryBuilder.eq(Constants.AVAILABLE_LOG_DATES_COLUMNS.DATE, date));

      session.execute(availableLogDatesDelete);

      // For all log levels delete all logs with the given date above
      for (LoggerLevel level : LoggerLevel.values()) {
        Select getLogs =
            QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);
        getLogs
            .where(QueryBuilder.eq(Constants.LOGS_COLUMNS.NODE_ID, this.newNodeId))
            .and(QueryBuilder.eq(Constants.LOGS_COLUMNS.DATE, date))
            .and(QueryBuilder.eq(Constants.LOGS_COLUMNS.LOG_LEVEL, level.toString()));

        for (Row logRow : session.execute(getLogs)) {
          Delete logDelete =
              QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);
          logDelete
              .where(QueryBuilder.eq(Constants.LOGS_COLUMNS.NODE_ID, this.newNodeId))
              .and(QueryBuilder.eq(Constants.LOGS_COLUMNS.DATE, date))
              .and(QueryBuilder.eq(Constants.LOGS_COLUMNS.LOG_LEVEL, level.toString()))
              .and(
                  QueryBuilder.eq(
                      Constants.LOGS_COLUMNS.COUNT, logRow.getInt(Constants.LOGS_COLUMNS.COUNT)));

          session.execute(logDelete);
        }
      }
    }

    this.logger.info(String.format("Deleting node schema records for node %d", this.newNodeId));

    Select nodeSchemas =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
    nodeSchemas.where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, this.newNodeId));

    for (Row row : session.execute(nodeSchemas)) {
      NodeSchemaEntry entry = NodeSchemaEntry.fromRow(row);

      Delete nodeSchemaDelete =
          QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
      nodeSchemaDelete
          .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, this.newNodeId))
          .and(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, entry.keyspaceName));

      session.execute(nodeSchemaDelete);
    }

    this.logger.info(String.format("Deleting deployment record for node %d", this.newNodeId));

    Delete deploymentDelete =
        QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    deploymentDelete
        .where(QueryBuilder.eq(PARENT_NODE_ID, this.parentNodeId))
        .and(QueryBuilder.eq(NEW_NODE_ID, this.newNodeId));

    session.execute(deploymentDelete);
  }

  /** @return information message to user */
  @Override
  public String toString() {
    return "Deleting nodes history";
  }
}

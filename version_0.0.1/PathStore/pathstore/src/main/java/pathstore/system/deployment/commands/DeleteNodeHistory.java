package pathstore.system.deployment.commands;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
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
 *     PathStoreCluster#getDaemonInstance()}. This function does not connect to the child node at any
 *     point.
 */
public class DeleteNodeHistory implements ICommand {
  /** Logger to display what is occuring */
  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(DeleteNodeHistory.class);

  /** Child node id who is being removed */
  private final int newNodeId;

  /** Node id if the parent node (the node running this application) */
  private final int parentNodeId;

  /**
   * @param newNodeId {@link #newNodeId}
   * @param parentNodeId {@link #parentNodeId}
   */
  public DeleteNodeHistory(final int newNodeId, final int parentNodeId) {
    this.newNodeId = newNodeId;
    this.parentNodeId = parentNodeId;
  }

  /**
   * Removes all records associated with {@link #newNodeId} from
   * pathstore_applications.{available_log_dates, logs, node_schemas, deployment}
   */
  @Override
  public void execute() {
    Session session = PathStoreCluster.getDaemonInstance().connect();

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
      Delete nodeSchemaDelete =
          QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);
      nodeSchemaDelete
          .where(QueryBuilder.eq(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, this.newNodeId))
          .and(
              QueryBuilder.eq(
                  Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME,
                  row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME)));

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
}

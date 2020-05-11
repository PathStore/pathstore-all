package pathstore.system.logging;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.UserType;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.core.schemabuilder.UDTType;
import com.sun.org.apache.bcel.internal.Const;
import jdk.jfr.internal.LogLevel;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.logger.LoggerLevel;
import pathstore.common.logger.PathStoreLoggerFactory;
import pathstore.common.logger.PathStoreLoggerMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO: Should this daemon also have a logger?
 *
 * <p>Logger daemon used to write logs to log table
 */
public class PathStoreLoggerDaemon extends Thread {

  /** Defines what log level we're querying */
  private final LoggerLevel level = LoggerLevel.FINEST;

  /** UDT for the custom data type to store messages */
  private final UserType logMessageType;

  /** Loads the log_message UDT */
  public PathStoreLoggerDaemon() {
    this.logMessageType =
        PathStoreCluster.getInstance()
            .getMetadata()
            .getKeyspace(Constants.PATHSTORE_APPLICATIONS)
            .getUserType(Constants.LOG_MESSAGE);
  }

  /**
   * Every 5 seconds write the lowest ordinal log level to the logs table.
   *
   * <p>Note: Parsing of this log based on log level will be done on the frontend to reduce api
   * traffic and to reduce number of records written to the logs table
   */
  @Override
  public void run() {
    Session session = PathStoreCluster.getInstance().connect();

    Insert insert = QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);

    insert
        .value(Constants.LOGS_COLUMNS.NODE_ID, PathStoreProperties.getInstance().NodeID)
        .value(Constants.LOGS_COLUMNS.LOG, new LinkedList<>());

    session.execute(insert);

    while (true) {
      if (PathStoreLoggerFactory.hasNew(level)) {

        List<PathStoreLoggerMessage> mergedMessages = PathStoreLoggerFactory.getMergedLog(level);

        List<UDTValue> convertedValues =
            mergedMessages.stream().map(this::createLogMessageType).collect(Collectors.toList());

        Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);

        update
            .where(
                QueryBuilder.eq(
                    Constants.LOGS_COLUMNS.NODE_ID, PathStoreProperties.getInstance().NodeID))
            .with(QueryBuilder.set(Constants.LOGS_COLUMNS.LOG, convertedValues));

        session.execute(update);
      }

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Creates a UDTValue to present a {@link PathStoreLoggerMessage} as a log_message
   *
   * @param pathStoreLoggerMessage message to convert
   * @return converted message
   */
  private UDTValue createLogMessageType(final PathStoreLoggerMessage pathStoreLoggerMessage) {
    return this.logMessageType
        .newValue()
        .setString(
            Constants.LOG_MESSAGE_PROPERTIES.MESSAGE_TYPE,
            pathStoreLoggerMessage.getLoggerLevel().name())
        .setString(
            Constants.LOG_MESSAGE_PROPERTIES.MESSAGE, pathStoreLoggerMessage.getFormattedMessage());
  }
}

package pathstore.system.logging;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.logger.LoggerLevel;
import pathstore.common.logger.PathStoreLoggerFactory;

import java.util.List;

/**
 * TODO: Should this daemon also have a logger?
 *
 * <p>Logger daemon used to write logs to log table
 */
public class PathStoreLoggerDaemon extends Thread {

  private final LoggerLevel level = LoggerLevel.FINEST;

  /**
   * Every 5 seconds write the lowest ordinal log level to the logs table.
   *
   * <p>Note: Parsing of this log based on log level will be done on the frontend to reduce api
   * traffic and to reduce number of records written to the logs table
   */
  @Override
  public void run() {
    Session session = PathStoreCluster.getInstance().connect();

    while (true) {

      if (PathStoreLoggerFactory.hasNew(level)) {

        List<String> mergedMessages = PathStoreLoggerFactory.getMergedLog(level);

        Insert insert = QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);

        insert
            .value(Constants.LOGS_COLUMNS.NODE_ID, PathStoreProperties.getInstance().NodeID)
            .value(Constants.LOGS_COLUMNS.LOG, mergedMessages);

        session.execute(insert);
      }

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}

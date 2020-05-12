package pathstore.system.logging;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.logger.PathStoreLoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TODO: Should this daemon also have a logger?
 *
 * <p>Logger daemon used to write logs to log table
 */
public class PathStoreLoggerDaemon extends Thread {

  /** String to denote how to format the date */
  private static String dateFormat = "yyyy-MM-dd";

  /**
   * Every 5 seconds write the lowest ordinal log level to the logs table.
   *
   * @apiNote Parsing of this log based on log level will be done on the frontend to reduce api
   *     traffic and to reduce number of records written to the logs table
   */
  @Override
  public void run() {

    Session session = PathStoreCluster.getInstance().connect();

    while (true) {
      if (PathStoreLoggerFactory.hasNew()) {
        PathStoreLoggerFactory.getMergedLog()
            .forEach(
                i -> {
                  Insert insert =
                      QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);
                  insert
                      .value(
                          Constants.LOGS_COLUMNS.NODE_ID, PathStoreProperties.getInstance().NodeID)
                      .value(
                          Constants.LOGS_COLUMNS.DATE,
                          new SimpleDateFormat(dateFormat).format(new Date()))
                      .value(Constants.LOGS_COLUMNS.COUNT, i.getCount())
                      .value(Constants.LOGS_COLUMNS.LOG_LEVEL, i.getLoggerLevel().name())
                      .value(Constants.LOGS_COLUMNS.LOG, i.getFormattedMessage());

                  session.execute(insert);
                });
      }
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}

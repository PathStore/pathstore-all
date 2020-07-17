package pathstore.system.deployment.commands;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.common.Constants;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.HashMap;
import java.util.Map;

/** This class is used to wait for pathstore to start up */
public class WaitForPathStore implements ICommand {

  /** Logger */
  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(WaitForPathStore.class);

  /**
   * TODO: Make timeout function optional
   *
   * <p>Max wait time is 5 minutes
   */
  private static final int maxWaitTime = 60 * 5;

  /** Cluster to child node */
  private final PathStorePrivilegedCluster cluster;

  /** Session to child node */
  private final Session session;

  /** Denotes the current amount of time waited in seconds */
  private int currentWaitCount;

  /** Tasks that need to be completed before we can consider pathstore to be officially online */
  private final Map<Integer, String> neededRecords;

  /**
   * Creates cluster
   *
   * @param username child username
   * @param password child password
   * @param ip ip of new root
   * @param port cassandra port
   */
  public WaitForPathStore(
      final String username, final String password, final String ip, final int port) {
    this.cluster = PathStorePrivilegedCluster.getChildInstance(username, password, ip, port);
    this.session = cluster.connect();
    this.currentWaitCount = 0;
    this.neededRecords = new HashMap<>();
    this.neededRecords.put(0, "RMI Server started");
    this.neededRecords.put(1, "Pathstore Application keyspace loaded");
    this.neededRecords.put(2, "Daemons started");
  }

  /**
   * Every second check to see if rows have been added and remove them from neededRecords if not
   * already done so and print out what their task was to stdout so user knows whats happening
   *
   * @throws CommandError contains a message to denote what went wrong
   */
  @Override
  public void execute() throws CommandError {

    Select tasks = QueryBuilder.select().all().from(Constants.LOCAL_KEYSPACE, Constants.STARTUP);

    for (Row row : session.execute(tasks)) {
      int task = row.getInt(Constants.STARTUP_COLUMNS.TASK_DONE);

      if (neededRecords.containsKey(task)) {
        logger.info(neededRecords.get(task));
        neededRecords.remove(task);
      }
    }

    if (neededRecords.size() > 0) {
      try {
        if (this.currentWaitCount >= maxWaitTime)
          throw new CommandError(
              String.format("Exceeded max wait time of %d seconds", maxWaitTime));

        this.currentWaitCount++;
        Thread.sleep(1000);
        this.execute();
      } catch (InterruptedException e) {
        throw new CommandError("Sleep was interrupted while waiting for pathstore to come online");
      }
    } else {
      logger.info("PathStore started up");
      this.cluster.close();
    }
  }

  /**
   * Inform user that pathstore is currently waiting to come online
   *
   * @return msg
   */
  @Override
  public String toString() {
    return "Waiting for PathStore to startup";
  }
}

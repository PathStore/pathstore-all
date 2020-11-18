package pathstore.system.deployment.commands;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.common.Constants;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to wait for pathstore to start up
 *
 * @implNote The local cassandra instance must be up before the you can create a cluster connection,
 *     that is why the cluster is created in the {@link #execute()} function rather then in the
 *     constructed
 */
public class WaitForPathStore implements ICommand {

  /** Logger */
  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(WaitForPathStore.class);

  /**
   * TODO: Make timeout function optional
   *
   * <p>Max wait time is 5 minutes
   */
  private static final int maxWaitTime = 60 * 5;

  /** Cassandra credentials to connect with */
  private final DeploymentCredential cassandraCredentials;

  /** Denotes the current amount of time waited in seconds */
  private int currentWaitCount;

  /** Tasks that need to be completed before we can consider pathstore to be officially online */
  private final Map<Integer, String> neededRecords;

  /**
   * Creates cluster
   *
   * @param cassandraCredentials {@link #cassandraCredentials}
   */
  public WaitForPathStore(final DeploymentCredential cassandraCredentials) {
    this.cassandraCredentials = cassandraCredentials;
    this.currentWaitCount = 0;
    this.neededRecords = new HashMap<>();
    this.neededRecords.put(0, "GRPC Server started");
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

    PathStorePrivilegedCluster cluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredentials);
    Session session = cluster.rawConnect();

    Select tasks =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_STARTUP);

    for (Row row : session.execute(tasks)) {
      int task = row.getInt(Constants.LOCAL_STARTUP_COLUMNS.TASK_DONE);

      if (this.neededRecords.containsKey(task)) {
        this.logger.info(neededRecords.get(task));
        this.neededRecords.remove(task);
      }
    }

    if (this.neededRecords.size() > 0) {
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
      this.logger.info("PathStore started up");
      cluster.close();
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

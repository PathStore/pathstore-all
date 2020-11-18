package pathstore.system.deployment.commands;

import com.datastax.driver.core.exceptions.NoHostAvailableException;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

/**
 * This class is used to denote a single step where after launching cassandra we wait for a
 * connection to be possible. This is because if we don't want for cassandra to start up pathstore
 * will have to do it.
 */
public class WaitForCassandra implements ICommand {

  /** Logger */
  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(WaitForCassandra.class);

  /**
   * TODO: Make timeout function optional
   *
   * <p>Max wait time is 5 minutes
   */
  private static final int maxWaitTime = 60 * 5;

  /** Cassandra credentials */
  private final DeploymentCredential cassandraCredentials;

  /** Denotes the current amount of time waited */
  private int currentWaitCount;

  /** @param cassandraCredentials {@link #cassandraCredentials} */
  public WaitForCassandra(final DeploymentCredential cassandraCredentials) {
    this.cassandraCredentials = cassandraCredentials;
    this.currentWaitCount = 0;
  }

  /**
   * Continue to try and make a connection, if an exception is thrown wait 1 second and try again
   * until a successful connection is made, then close connection
   *
   * @throws CommandError contains a message to denote what went wrong
   */
  @Override
  public void execute() throws CommandError {
    PathStorePrivilegedCluster cluster = null;
    try {
      cluster = PathStorePrivilegedCluster.getChildInstance(cassandraCredentials);

      logger.info("Cassandra is online");

    } catch (NoHostAvailableException e) {
      try {
        if (this.currentWaitCount >= maxWaitTime)
          throw new CommandError(
              String.format("Exceeded max wait time of %d seconds", maxWaitTime));

        this.currentWaitCount++;
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        throw new CommandError("Sleep was interrupted while waiting for cassandra to come online");
      }
      this.execute();
    } finally {
      if (cluster != null) cluster.close();
    }
  }

  /** @return states that we're waiting for cassandra to come online */
  @Override
  public String toString() {
    return "Waiting for cassandra to come online";
  }
}

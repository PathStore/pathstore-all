package pathstore.system.deployment.commands;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import pathstore.system.deployment.utilities.StartupUTIL;
import pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils;

/**
 * This class is used to denote a single step where after launching cassandra we wait for a
 * connection to be possible. This is because if we don't want for cassandra to start up pathstore
 * will have to do it.
 */
public class WaitForCassandra implements ICommand {

  /** Ip of new root node */
  private final String ip;

  /** Cassandra port */
  private final int port;

  /**
   * @param ip {@link #ip}
   * @param port {@link #port}
   */
  public WaitForCassandra(final String ip, final int port) {
    this.ip = ip;
    this.port = port;
  }

  /**
   * Continue to try and make a connection, if an exception is thrown wait 1 second and try again
   * until a successful connection is made, then close connection
   *
   * @throws CommandError contains a message to denote what went wrong
   */
  @Override
  public void execute() throws CommandError {
    try {
      Cluster cluster = StartupUTIL.createCluster(this.ip, this.port);
      Session session = cluster.connect();

      PathStoreSchemaLoaderUtils.loadLocalKeyspace(session);

      session.close();
      cluster.close();
    } catch (NoHostAvailableException e) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        throw new CommandError(
            "Sleep was interrupted while waiting for cassandra to come online");
      }
      this.execute();
    }
  }

  /** @return states that we're waiting for cassandra to come online */
  @Override
  public String toString() {
    return "Waiting for cassandra to come online";
  }
}

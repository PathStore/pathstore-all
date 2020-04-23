package pathstoreweb.pathstoreadminpanel.startup.commands;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import pathstoreweb.pathstoreadminpanel.startup.SSHUtil;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.InternalException;

/**
 * This class is used to denote a single step where after launching cassandra we wait for a
 * connection to be possible. This is because if we don't want for cassandra to start up pathstore
 * will have to do it.
 */
public class WaitForCassandra implements ICommand {

  /**
   * Continue to try and make a connection, if an exception is thrown wait 1 second and try again
   * until a successful connection is made, then close connection
   *
   * @param sshUtil ssh utility to access the remote host
   * @throws InternalException if there is an interrupted exception thrown during sleep[
   */
  @Override
  public void execute(SSHUtil sshUtil) throws InternalException {
    try {
      Cluster cluster =
          new Cluster.Builder()
              .addContactPoints(sshUtil.host)
              .withPort(9052)
              .withSocketOptions(
                  (new SocketOptions()).setTcpNoDelay(true).setReadTimeoutMillis(15000000))
              .withQueryOptions(
                  (new QueryOptions())
                      .setRefreshNodeIntervalMillis(0)
                      .setRefreshNodeListIntervalMillis(0)
                      .setRefreshSchemaIntervalMillis(0))
              .build();
      cluster.connect().close();
      cluster.close();
    } catch (NoHostAvailableException e) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        throw new InternalException();
      }
      this.execute(sshUtil);
    }
  }

  /** @return states that we're waiting for cassandra to come online */
  @Override
  public String toString() {
    return "Waiting for cassandra to come online";
  }
}

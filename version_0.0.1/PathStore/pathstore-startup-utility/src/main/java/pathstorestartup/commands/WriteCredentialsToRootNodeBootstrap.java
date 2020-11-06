package pathstorestartup.commands;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.authentication.credentials.DeploymentCredential;;
import pathstore.common.Constants;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.deployment.commands.ICommand;

/**
 * This command is used to write the daemon account credentials of the root node to the
 * local_keyspace.auth table.
 */
public class WriteCredentialsToRootNodeBootstrap implements ICommand {
  /** Cassandra credentials to connect to */
  private final DeploymentCredential cassandraCredentials;

  /** Node id to write to table */
  private final int nodeIdToWrite;

  /** Username to write to table */
  private final String usernameToWrite;

  /** Password to write to table */
  private final String passwordToWrite;

  /**
   * @param cassandraCredentials {@link #cassandraCredentials}
   * @param nodeIdToWrite {@link #nodeIdToWrite}
   * @param usernameToWrite {@link #usernameToWrite}
   * @param passwordToWrite {@link #passwordToWrite}
   */
  public WriteCredentialsToRootNodeBootstrap(
      final DeploymentCredential cassandraCredentials,
      final int nodeIdToWrite,
      final String usernameToWrite,
      final String passwordToWrite) {
    this.cassandraCredentials = cassandraCredentials;
    this.nodeIdToWrite = nodeIdToWrite;
    this.usernameToWrite = usernameToWrite;
    this.passwordToWrite = passwordToWrite;
  }

  /** Connect to the root node then write the credential object and close the connection */
  @Override
  public void execute() {
    PathStorePrivilegedCluster rootCluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredentials);

    rootCluster
        .connect()
        .execute(
            QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_NODE_AUTH)
                .value(Constants.LOCAL_NODE_AUTH_COLUMNS.NODE_ID, this.nodeIdToWrite)
                .value(Constants.LOCAL_NODE_AUTH_COLUMNS.USERNAME, this.usernameToWrite)
                .value(Constants.LOCAL_NODE_AUTH_COLUMNS.PASSWORD, this.passwordToWrite));

    rootCluster.close();
  }

  /** @return inform the user which user account is being written with what node id */
  @Override
  public String toString() {
    return String.format(
        "Writing %s to root node table with id %d", this.usernameToWrite, this.nodeIdToWrite);
  }
}

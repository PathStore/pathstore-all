package pathstorestartup.commands;

import pathstore.authentication.Credential;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.deployment.commands.ICommand;

/**
 * This command is used to write the daemon account credentials of the root node to the
 * local_keyspace.auth table.
 *
 * @see pathstore.authentication.CredentialInfo
 */
public class WriteCredentialsToRootNodeBootstrap implements ICommand {
  /** Username to connect with */
  private final String connectionUsername;

  /** Password to connect with */
  private final String connectionPassword;

  /** ip of root node */
  private final String ip;

  /** Port of root node */
  private final int port;

  /** Node id to write to table */
  private final int nodeIdToWrite;

  /** Username to write to table */
  private final String usernameToWrite;

  /** Password to write to table */
  private final String passwordToWrite;

  /**
   * @param connectionUsername {@link #connectionUsername}
   * @param connectionPassword {@link #connectionPassword}
   * @param ip {@link #ip}
   * @param port {@link #port}
   * @param nodeIdToWrite {@link #nodeIdToWrite}
   * @param usernameToWrite {@link #usernameToWrite}
   * @param passwordToWrite {@link #passwordToWrite}
   */
  public WriteCredentialsToRootNodeBootstrap(
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port,
      final int nodeIdToWrite,
      final String usernameToWrite,
      final String passwordToWrite) {
    this.connectionUsername = connectionUsername;
    this.connectionPassword = connectionPassword;
    this.ip = ip;
    this.port = port;
    this.nodeIdToWrite = nodeIdToWrite;
    this.usernameToWrite = usernameToWrite;
    this.passwordToWrite = passwordToWrite;
  }

  /** Connect to the root node then write the credential object and close the connection */
  @Override
  public void execute() {
    PathStorePrivilegedCluster rootCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.connectionUsername, this.connectionPassword, this.ip, this.port);

    Credential.writeCredentialToRow(
        rootCluster.connect(),
        new Credential(this.nodeIdToWrite, this.usernameToWrite, this.passwordToWrite));

    rootCluster.close();
  }

  /** @return inform the user which user account is being written with what node id */
  @Override
  public String toString() {
    return String.format(
        "Writing %s to root node table with id %d", this.usernameToWrite, this.nodeIdToWrite);
  }
}

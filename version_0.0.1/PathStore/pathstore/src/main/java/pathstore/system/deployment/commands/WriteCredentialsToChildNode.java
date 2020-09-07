package pathstore.system.deployment.commands;

import pathstore.authentication.Credential;
import pathstore.authentication.CredentialCache;
import pathstore.system.PathStorePrivilegedCluster;

/**
 * This command is used to write the current node's daemon account to the child node's
 * pathstore_applications.local_auth table so they can access the parent node's cassandra during push and pull
 * operations
 */
public class WriteCredentialsToChildNode implements ICommand {
  /** current node id */
  private final int nodeid;

  /** Username to connect to child with */
  private final String connectionUsername;

  /** Password to connect to child with */
  private final String connectionPassword;

  /** Child ip */
  private final String ip;

  /** Child port */
  private final int port;

  /**
   * @param nodeid {@link #nodeid}
   * @param connectionUsername {@link #connectionUsername}
   * @param connectionPassword {@link #connectionPassword}
   * @param ip {@link #ip}
   * @param port {@link #port}
   */
  public WriteCredentialsToChildNode(
      final int nodeid,
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port) {
    this.nodeid = nodeid;
    this.connectionUsername = connectionUsername;
    this.connectionPassword = connectionPassword;
    this.ip = ip;
    this.port = port;
  }

  /** Connect to the child node, write the credentials to that node, and close the cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.connectionUsername, this.connectionPassword, this.ip, this.port);

    Credential.writeCredentialToRow(
        childCluster.connect(), CredentialCache.getInstance().getCredential(this.nodeid));

    childCluster.close();
  }

  /** @return command inform message */
  @Override
  public String toString() {
    return String.format(
        "Writing account with username %s to child node",
        CredentialCache.getInstance().getCredential(this.nodeid).username);
  }
}

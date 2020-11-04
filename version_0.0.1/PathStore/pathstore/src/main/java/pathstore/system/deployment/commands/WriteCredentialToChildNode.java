package pathstore.system.deployment.commands;

import pathstore.authentication.CredentialDataLayer;
import pathstore.authentication.credentials.Credential;
import pathstore.system.PathStorePrivilegedCluster;

/**
 * This command is used to write the current node's daemon account to the child node's
 * pathstore_applications.local_auth table so they can access the parent node's cassandra during
 * push and pull operations
 */
public class WriteCredentialToChildNode<SearchableT, CredentialT extends Credential<SearchableT>>
    implements ICommand {
  /** Data layer to write to */
  private final CredentialDataLayer<SearchableT, CredentialT> dataLayer;

  /** Credential to write */
  private final CredentialT credential;

  /** Username to connect to child with */
  private final String connectionUsername;

  /** Password to connect to child with */
  private final String connectionPassword;

  /** Child ip */
  private final String ip;

  /** Child port */
  private final int port;

  /**
   * @param dataLayer {@link #dataLayer}
   * @param credential {@link #credential}
   * @param connectionUsername {@link #connectionUsername}
   * @param connectionPassword {@link #connectionPassword}
   * @param ip {@link #ip}
   * @param port {@link #port}
   */
  public WriteCredentialToChildNode(
      final CredentialDataLayer<SearchableT, CredentialT> dataLayer,
      final CredentialT credential,
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port) {
    this.dataLayer = dataLayer;
    this.credential = credential;
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

    this.dataLayer.write(childCluster.connect(), this.credential);

    childCluster.close();
  }

  /** @return command inform message */
  @Override
  public String toString() {
    return String.format(
        "Writing account with username %s to child node", this.credential.getUsername());
  }
}

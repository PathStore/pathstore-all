package pathstore.system.deployment.commands;

import pathstore.authentication.CredentialDataLayer;
import pathstore.authentication.credentials.Credential;
import pathstore.authentication.credentials.DeploymentCredential;
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

  /** Cassandra credentials to connect with */
  private final DeploymentCredential cassandraCredentials;

  /**
   * @param dataLayer {@link #dataLayer}
   * @param credential {@link #credential}
   * @param cassandraCredentials {@link #cassandraCredentials}
   */
  public WriteCredentialToChildNode(
      final CredentialDataLayer<SearchableT, CredentialT> dataLayer,
      final CredentialT credential,
      final DeploymentCredential cassandraCredentials) {
    this.dataLayer = dataLayer;
    this.credential = credential;
    this.cassandraCredentials = cassandraCredentials;
  }

  /** Connect to the child node, write the credentials to that node, and close the cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredentials);

    this.dataLayer.write(childCluster.rawConnect(), this.credential);

    childCluster.close();
  }

  /** @return command inform message */
  @Override
  public String toString() {
    return String.format(
        "Writing account with username %s to child node", this.credential.getUsername());
  }
}

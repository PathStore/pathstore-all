package pathstore.system.deployment.commands;

import pathstore.authentication.Credential;
import pathstore.authentication.CredentialInfo;
import pathstore.system.PathStorePrivilegedCluster;

public class WriteCredentialsToChildNode implements ICommand {
  private final int nodeid;

  private final String connectionUsername;

  private final String connectionPassword;

  private final String ip;

  private final int port;

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

  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.connectionUsername, this.connectionPassword, this.ip, this.port);

    Credential.writeCredentialToRow(
        childCluster.connect(), CredentialInfo.getInstance().getCredential(this.nodeid));

    childCluster.close();
  }

  @Override
  public String toString() {
    return String.format(
        "Writing account with username %s to child node",
        CredentialInfo.getInstance().getCredential(this.nodeid).username);
  }
}

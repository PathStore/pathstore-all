package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.authentication.Credential;
import pathstore.authentication.CredentialInfo;
import pathstore.system.PathStorePrivilegedCluster;

public class WriteCredentialsToChildNode implements ICommand {
  private final int nodeid;

  private final String username;

  private final String password;

  private final String ip;

  private final int port;

  public WriteCredentialsToChildNode(
      final int nodeid,
      final String username,
      final String password,
      final String ip,
      final int port) {
    this.nodeid = nodeid;
    this.username = username;
    this.password = password;
    this.ip = ip;
    this.port = port;
  }

  @Override
  public void execute() {
    Credential credential = CredentialInfo.getInstance().getCredential(this.nodeid);

    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.username, this.password, this.ip, this.port);

    Session childSession = childCluster.connect();

    childSession.execute(
        QueryBuilder.insertInto("local_keyspace", "auth")
            .value("node_id", credential.node_id)
            .value("username", credential.username)
            .value("password", credential.password));

    childCluster.close();
  }

  @Override
  public String toString() {
    return String.format(
        "Writing account with username %s to child node",
        CredentialInfo.getInstance().getCredential(this.nodeid).username);
  }
}

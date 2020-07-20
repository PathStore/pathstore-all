package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.authentication.Credential;
import pathstore.system.PathStorePrivilegedCluster;

public class WriteCredentialsToChildNode implements ICommand {
  private final Credential credentialsToWrite;

  private final String username;

  private final String password;

  private final String ip;

  private final int port;

  public WriteCredentialsToChildNode(
      final Credential credentialsToWrite,
      final String username,
      final String password,
      final String ip,
      final int port) {
    this.credentialsToWrite = credentialsToWrite;
    this.username = username;
    this.password = password;
    this.ip = ip;
    this.port = port;
  }

  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.username, this.password, this.ip, this.port);

    Session childSession = childCluster.connect();

    childSession.execute(
        QueryBuilder.insertInto("local_keyspace", "auth")
            .value("node_id", this.credentialsToWrite.node_id)
            .value("username", this.credentialsToWrite.username)
            .value("password", this.credentialsToWrite.password));

    childCluster.close();
  }

  @Override
  public String toString() {
    return String.format(
        "Writing account with username %s to child node", this.credentialsToWrite.username);
  }
}

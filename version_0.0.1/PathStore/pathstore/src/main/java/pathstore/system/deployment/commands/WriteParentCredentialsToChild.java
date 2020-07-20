package pathstore.system.deployment.commands;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.Session;
import pathstore.common.PathStoreProperties;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.authentication.Credential;

public class WriteParentCredentialsToChild implements ICommand {
  private final Credential parentCredentials;

  private final String username;

  private final String password;

  private final String ip;

  private final int port;

  public WriteParentCredentialsToChild(
      final Credential parentCredentials,
      final String username,
      final String password,
      final String ip,
      final int port) {
    this.parentCredentials = parentCredentials;
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
            .value("node_id", PathStoreProperties.getInstance().NodeID)
            .value("username", this.parentCredentials.username)
            .value("password", this.parentCredentials.password));

    childCluster.close();
  }

  @Override
  public String toString() {
    return "Writing parent credentials to child cassandra";
  }
}

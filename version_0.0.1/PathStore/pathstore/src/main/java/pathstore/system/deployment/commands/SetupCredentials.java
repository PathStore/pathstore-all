package pathstore.system.deployment.commands;

import authentication.Credential;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.PathStoreProperties;
import pathstore.system.PathStorePrivilegedCluster;

// assumed cassandra has started up
// TODO: Load new child role and delete cassandra role
public class SetupCredentials implements ICommand {

  private final Credential parentCredentials;

  private final String username;

  private final String password;

  private final PathStorePrivilegedCluster childCluster;

  public SetupCredentials(
      final Credential parentCredentials,
      final String username,
      final String password,
      final String ip,
      final int port) {
    this.parentCredentials = parentCredentials;
    this.username = username;
    this.password = password;
    this.childCluster =
        PathStorePrivilegedCluster.getChildInstance("cassandra", "cassandra", ip, port);
  }

  @Override
  public void execute() {
    Session childSession = childCluster.connect();

    childSession.execute(
        QueryBuilder.insertInto("local_keyspace", "auth")
            .value("node_id", PathStoreProperties.getInstance().NodeID)
            .value("username", this.parentCredentials.username)
            .value("password", this.parentCredentials.password));

    // load new child role and delete old role.

    this.childCluster.close();
  }
}

package pathstore.system.deployment.commands;

import authentication.Credential;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;
import pathstore.system.PathStorePrivilegedCluster;

// assumed cassandra has started up
// TODO: Load new child role and delete cassandra role
public class SetupCredentials implements ICommand {

  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(SetupCredentials.class);

  private final Credential parentCredentials;

  private final String username;

  private final String password;

  private final String ip;

  private final int port;

  public SetupCredentials(
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
            Constants.DEFAULT_CASSANDRA_USERNAME,
            Constants.DEFAULT_CASSANDRA_PASSWORD,
            this.ip,
            this.port);

    System.out.println("Got cluster");

    Session childSession = childCluster.connect();

    System.out.println("Got client");

    // load new child role and delete old role.

    String command =
        String.format(
            "CREATE ROLE %s WITH SUPERUSER = true AND LOGIN = true and PASSWORD = '%s'",
            this.username, this.password);

    childSession.execute(command);

    this.logger.info(
        String.format("Generated Role with login %s %s", this.username, this.password));

    childCluster.close();

    childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.username, this.password, this.ip, this.port);
    childSession = childCluster.connect();

    childSession.execute(String.format("DROP ROLE %s", "cassandra"));

    this.logger.info("Dropped role cassandra");

    childSession.execute(
        QueryBuilder.insertInto("local_keyspace", "auth")
            .value("node_id", PathStoreProperties.getInstance().NodeID)
            .value("username", this.parentCredentials.username)
            .value("password", this.parentCredentials.password));

    this.logger.info("Wrote parent credentials to child database");

    childCluster.close();
  }
}

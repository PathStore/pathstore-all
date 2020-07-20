package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import pathstore.common.Constants;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;
import pathstore.system.PathStorePrivilegedCluster;

// assumed cassandra has started up
// TODO: Load new child role and delete cassandra role
public class CreateSuperUserAccount implements ICommand {

  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(CreateSuperUserAccount.class);

  private final String username;

  private final String password;

  private final String ip;

  private final int port;

  public CreateSuperUserAccount(
      final String username, final String password, final String ip, final int port) {
    this.username = username;
    this.password = password;
    this.ip = ip;
    this.port = port;
  }

  @Override
  public void execute() {
    try {
      PathStorePrivilegedCluster childCluster =
          PathStorePrivilegedCluster.getChildInstance(
              Constants.DEFAULT_CASSANDRA_USERNAME,
              Constants.DEFAULT_CASSANDRA_PASSWORD,
              this.ip,
              this.port);

      Session childSession = childCluster.connect();

      // load new child role and delete old role.

      childSession.execute(
          String.format(
              "CREATE ROLE %s WITH SUPERUSER = true AND LOGIN = true and PASSWORD = '%s'",
              this.username, this.password));

      this.logger.info(
          String.format("Generated Role with login %s %s", this.username, this.password));

      childCluster.close();

      childCluster =
          PathStorePrivilegedCluster.getChildInstance(
              this.username, this.password, this.ip, this.port);
      childSession = childCluster.connect();

      childSession.execute("DROP ROLE cassandra");

      this.logger.info("Dropped role cassandra");

      childCluster.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public String toString() {
    return "Creating super user account for child node";
  }
}

package pathstore.system.deployment.commands;

import pathstore.authentication.AuthenticationUtil;
import pathstore.common.Constants;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.system.PathStorePrivilegedCluster;

// assumed cassandra has started up
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
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            Constants.DEFAULT_CASSANDRA_USERNAME,
            Constants.DEFAULT_CASSANDRA_PASSWORD,
            this.ip,
            this.port);

    // load new child role and delete old role.

    AuthenticationUtil.createRole(childCluster.connect(), this.username, true, true, this.password);

    // TODO: Remove, this is temporary
    this.logger.info(
        String.format("Generated Role with login %s %s", this.username, this.password));

    childCluster.close();

    childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.username, this.password, this.ip, this.port);

    AuthenticationUtil.dropRole(childCluster.connect(), Constants.DEFAULT_CASSANDRA_USERNAME);

    this.logger.info(String.format("Dropped role %s", Constants.DEFAULT_CASSANDRA_USERNAME));

    childCluster.close();
  }

  @Override
  public String toString() {
    return "Creating super user account for child node";
  }
}

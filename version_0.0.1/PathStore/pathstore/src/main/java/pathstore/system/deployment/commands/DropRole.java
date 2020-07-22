package pathstore.system.deployment.commands;

import pathstore.authentication.AuthenticationUtil;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

public class DropRole implements ICommand {

  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(DropRole.class);

  private final String connectionUsername;

  private final String connectionPassword;

  private final String ip;

  private final int port;

  private final String roleName;

  public DropRole(
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port,
      final String roleName) {
    this.connectionUsername = connectionUsername;
    this.connectionPassword = connectionPassword;
    this.ip = ip;
    this.port = port;
    this.roleName = roleName;
  }

  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.connectionUsername, this.connectionPassword, this.ip, this.port);

    AuthenticationUtil.dropRole(childCluster.connect(), this.roleName);

    this.logger.info(String.format("Dropped role %s", this.roleName));

    childCluster.close();
  }

  @Override
  public String toString() {
    return String.format("Dropping role with name %s", this.roleName);
  }
}

package pathstore.system.deployment.commands;

import pathstore.authentication.AuthenticationUtil;
import pathstore.system.PathStorePrivilegedCluster;

public class GrantDaemonPermissions implements ICommand {

  private final String connectionUsername;

  private final String connectionPassword;

  private final String ip;

  private final int port;

  private final String roleName;

  private final String keyspace;

  public GrantDaemonPermissions(
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port,
      final String roleName,
      final String keyspace) {
    this.connectionUsername = connectionUsername;
    this.connectionPassword = connectionPassword;
    this.ip = ip;
    this.port = port;
    this.roleName = roleName;
    this.keyspace = keyspace;
  }

  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.connectionUsername, this.connectionPassword, this.ip, this.port);

    AuthenticationUtil.grantAccessToKeyspace(childCluster.connect(), this.keyspace, this.roleName);

    childCluster.close();
  }
}

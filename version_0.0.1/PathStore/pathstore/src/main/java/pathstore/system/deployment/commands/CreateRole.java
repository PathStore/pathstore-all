package pathstore.system.deployment.commands;

import pathstore.authentication.AuthenticationUtil;
import pathstore.system.PathStorePrivilegedCluster;

public class CreateRole implements ICommand {

  private final String connectionUsername;

  private final String connectionPassword;

  private final String ip;

  private final int port;

  private final String roleName;

  private final String rolePassword;

  private boolean isSuperUser;

  public CreateRole(
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port,
      final String roleName,
      final String rolePassword,
      final boolean isSuperUser) {
    this.connectionUsername = connectionUsername;
    this.connectionPassword = connectionPassword;
    this.ip = ip;
    this.port = port;
    this.roleName = roleName;
    this.rolePassword = rolePassword;
    this.isSuperUser = isSuperUser;
  }

  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.connectionUsername, this.connectionPassword, this.ip, this.port);

    // load new child role and delete old role.
    AuthenticationUtil.createRole(
        childCluster.connect(), this.roleName, this.isSuperUser, true, this.rolePassword);

    childCluster.close();
  }

  @Override
  public String toString() {
    return String.format(
        "Creating user account for child node with username %s and super user %b",
        this.roleName, this.isSuperUser);
  }
}

package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import pathstore.authentication.AuthenticationUtil;
import pathstore.common.Constants;
import pathstore.system.PathStorePrivilegedCluster;

public class CreateDaemonAccount implements ICommand {
  private final String username;

  private final String password;

  private final String ip;

  private final int port;

  private final String daemonUsername;

  private final String daemonPassword;

  public CreateDaemonAccount(
      final String username,
      final String password,
      final String ip,
      final int port,
      final String daemonUsername,
      final String daemonPassword) {
    this.username = username;
    this.password = password;
    this.ip = ip;
    this.port = port;
    this.daemonUsername = daemonUsername;
    this.daemonPassword = daemonPassword;
  }

  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.username, this.password, this.ip, this.port);

    Session childSession = childCluster.connect();

    AuthenticationUtil.createRole(
        childSession, this.daemonUsername, false, true, this.daemonPassword);

    AuthenticationUtil.grantAccessToKeyspace(
        childSession, Constants.PATHSTORE_APPLICATIONS, this.daemonUsername);

    childCluster.close();
  }

  @Override
  public String toString() {
    return "Creating daemon account for child";
  }
}

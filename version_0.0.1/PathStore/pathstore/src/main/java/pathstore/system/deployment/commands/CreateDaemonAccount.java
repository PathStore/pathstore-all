package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import pathstore.common.Constants;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils;

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

    childSession.execute(
        String.format(
            "CREATE ROLE %s WITH SUPERUSER = false AND LOGIN = true and PASSWORD = '%s'",
            this.daemonUsername, this.daemonPassword));

    PathStoreSchemaLoaderUtils.grantAccessToKeyspace(
        childSession, Constants.PATHSTORE_APPLICATIONS, this.daemonUsername);

    childCluster.close();
  }

  @Override
  public String toString() {
    return "Create daemon account for child";
  }
}

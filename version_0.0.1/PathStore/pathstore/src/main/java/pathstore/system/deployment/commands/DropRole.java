package pathstore.system.deployment.commands;

import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.system.PathStorePrivilegedCluster;

/** This command is used to drop a role on the child node during node deployment */
public class DropRole implements ICommand {

  /** Username to connect to child with */
  private final String connectionUsername;

  /** Password to connect to child with */
  private final String connectionPassword;

  /** Ip of child */
  private final String ip;

  /** Port of child */
  private final int port;

  /** Role to drop */
  private final String roleName;

  /**
   * @param connectionUsername {@link #connectionUsername}
   * @param connectionPassword {@link #connectionPassword}
   * @param ip {@link #ip}
   * @param port {@link #port}
   * @param roleName {@link #roleName}
   */
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

  /** Connect to the child, drop role and close cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.connectionUsername, this.connectionPassword, this.ip, this.port);

    CassandraAuthenticationUtil.dropRole(childCluster.connect(), this.roleName);

    childCluster.close();
  }

  /** @return command inform message */
  @Override
  public String toString() {
    return String.format("Dropping role with name %s", this.roleName);
  }
}

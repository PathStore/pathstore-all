package pathstore.system.deployment.commands;

import pathstore.authentication.AuthenticationUtil;
import pathstore.system.PathStorePrivilegedCluster;

/**
 * This class is used to grant read and write permissions to a given role on a given keyspace
 *
 * @apiNote the role name must exist and the keyspace must already exist as no validity check occurs
 */
public class GrantDaemonPermissions implements ICommand {

  /**
   * Login username for cassandra, this role must be capable of granting role permissions (most
   * likely a super user username)
   */
  private final String connectionUsername;

  /** Login password for cassandra */
  private final String connectionPassword;

  /** Ip of cassandra instance */
  private final String ip;

  /** Native transport port for cassandra */
  private final int port;

  /** Role to give permissions to */
  private final String roleName;

  /** Keyspace to grant read and write permissions on */
  private final String keyspace;

  /**
   * @param connectionUsername {@link #connectionUsername}
   * @param connectionPassword {@link #connectionPassword}
   * @param ip {@link #ip}
   * @param port {@link #port}
   * @param roleName {@link #roleName}
   * @param keyspace {@link #keyspace}
   */
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

  /** Connects to child node, grants permissions then closes the connection */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.connectionUsername, this.connectionPassword, this.ip, this.port);

    AuthenticationUtil.grantAccessToKeyspace(childCluster.connect(), this.keyspace, this.roleName);

    childCluster.close();
  }

  /** @return inform user who is getting read and write on what keyspace */
  @Override
  public String toString() {
    return String.format(
        "Granting read and write access on keyspace %s to role %s", this.keyspace, this.roleName);
  }
}

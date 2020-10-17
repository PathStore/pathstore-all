package pathstore.system.deployment.commands;

import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.system.PathStorePrivilegedCluster;

/** This command is used to create a role on the child node during deployment */
public class CreateRole implements ICommand {

  /** Username to connect to child with */
  private final String connectionUsername;

  /** Password to connect to child with */
  private final String connectionPassword;

  /** Child ip */
  private final String ip;

  /** Child cassandra port */
  private final int port;

  /** Role to create */
  private final String roleName;

  /** Role password */
  private final String rolePassword;

  /** Is the role a super user */
  private boolean isSuperUser;

  /**
   * @param connectionUsername {@link #connectionUsername}
   * @param connectionPassword {@link #connectionPassword}
   * @param ip {@link #ip}
   * @param port {@link #port}
   * @param roleName {@link #roleName}
   * @param rolePassword {@link #rolePassword}
   * @param isSuperUser {@link #isSuperUser}
   */
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

  /** Connect to the child node, create the role and close the cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.connectionUsername, this.connectionPassword, this.ip, this.port);

    // load new child role and delete old role.
    CassandraAuthenticationUtil.createRole(
        childCluster.connect(), this.roleName, this.isSuperUser, true, this.rolePassword);

    childCluster.close();
  }

  /** @return inform print out message */
  @Override
  public String toString() {
    return String.format(
        "Creating user account for child node with username %s and super user %b",
        this.roleName, this.isSuperUser);
  }
}

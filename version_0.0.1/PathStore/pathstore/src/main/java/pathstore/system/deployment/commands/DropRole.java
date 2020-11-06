package pathstore.system.deployment.commands;

import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.system.PathStorePrivilegedCluster;

/** This command is used to drop a role on the child node during node deployment */
public class DropRole implements ICommand {

  /** Cassandra credentials to connect with */
  private final DeploymentCredential cassandraCredentials;

  /** Role to drop */
  private final String roleName;

  /**
   * @param cassandraCredentials {@link #cassandraCredentials}
   * @param roleName {@link #roleName}
   */
  public DropRole(
          final DeploymentCredential cassandraCredentials, final String roleName) {
    this.cassandraCredentials = cassandraCredentials;
    this.roleName = roleName;
  }

  /** Connect to the child, drop role and close cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredentials);

    CassandraAuthenticationUtil.dropRole(childCluster.connect(), this.roleName);

    childCluster.close();
  }

  /** @return command inform message */
  @Override
  public String toString() {
    return String.format("Dropping role with name %s", this.roleName);
  }
}

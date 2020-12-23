package pathstore.system.deployment.commands;

import lombok.RequiredArgsConstructor;
import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.system.PathStorePrivilegedCluster;

/**
 * This class is used to grant read and write permissions to a given role on a given keyspace
 *
 * @apiNote the role name must exist and the keyspace must already exist as no validity check occurs
 */
@RequiredArgsConstructor
public class GrantReadAndWriteAccess implements ICommand {

  /** Cassandra credentials to connect with */
  private final DeploymentCredential cassandraCredentials;

  /** Role to give permissions to */
  private final String roleName;

  /** Keyspace to grant read and write permissions on */
  private final String keyspace;

  /** Connects to child node, grants permissions then closes the connection */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredentials);

    CassandraAuthenticationUtil.grantAccessToKeyspace(
        childCluster.rawConnect(), this.keyspace, this.roleName);

    childCluster.close();
  }

  /** @return inform user who is getting read and write on what keyspace */
  @Override
  public String toString() {
    return String.format(
        "Granting read and write access on keyspace %s to role %s", this.keyspace, this.roleName);
  }
}

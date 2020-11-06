package pathstore.system.deployment.commands;

import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.authentication.credentials.Credential;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.system.PathStorePrivilegedCluster;

/** This command is used to create a role on the child node during deployment */
public class CreateRole implements ICommand {

  /** Cassandra credentials to connect with */
  private final DeploymentCredential cassandraCredentials;

  /** Role to create credentials */
  private final Credential<?> credential;

  /** Is the role a super user */
  private boolean isSuperUser;

  /**
   * @param cassandraCredentials {@link #cassandraCredentials}
   * @param credential {@link #credential}
   * @param isSuperUser {@link #isSuperUser}
   */
  public CreateRole(
      final DeploymentCredential cassandraCredentials,
      final Credential<?> credential,
      final boolean isSuperUser) {
    this.cassandraCredentials = cassandraCredentials;
    this.credential = credential;
    this.isSuperUser = isSuperUser;
  }

  /** Connect to the child node, create the role and close the cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredentials);

    // load new child role and delete old role.
    CassandraAuthenticationUtil.createRole(
        childCluster.connect(),
        this.credential.getUsername(),
        this.isSuperUser,
        true,
        this.credential.getPassword());

    childCluster.close();
  }

  /** @return inform print out message */
  @Override
  public String toString() {
    return String.format(
        "Creating user account for child node with username %s and super user %b",
        this.credential.getUsername(), this.isSuperUser);
  }
}

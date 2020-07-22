package pathstorestartup.constants;

import pathstore.system.deployment.commands.*;
import pathstore.system.deployment.utilities.DeploymentBuilder;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstorestartup.commands.CreateWebsitePropertiesFile;
import pathstorestartup.commands.WriteCredentialsToRootNodeBootstrap;

/**
 * Deployment functions specific to the startup utility
 *
 * @see pathstore.system.deployment.utilities.DeploymentBuilder
 */
public class BootstrapDeploymentBuilder extends DeploymentBuilder<BootstrapDeploymentBuilder> {

  /** @param remoteHostConnect root node connection */
  public BootstrapDeploymentBuilder(final SSHUtil remoteHostConnect) {
    super(remoteHostConnect);
  }

  /**
   * This function is used to remove all remote references to the admin panel. This should be used
   * ontop of {@link DeploymentBuilder#init()}
   */
  public BootstrapDeploymentBuilder initBootstrap() {
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.INIT_BOOTSTRAP_COMMANDS.KILL_PATHSTORE_ADMIN_PANEL,
            -1));
    commands.add(
        new Exec(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.INIT_BOOTSTRAP_COMMANDS.REMOVE_PATHSTORE_ADMIN_PANEL,
            -1));

    commands.add(
        new Exec(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.INIT_BOOTSTRAP_COMMANDS.REMOVE_PATHSTORE_ADMIN_PANEL_IMAGE,
            -1));

    return this;
  }

  /**
   * This function is used to generate the properties file for the website, transfer it, then delete
   * the local copy
   *
   * @param ip ip of the root node
   * @param cassandraPort cassandra port of the root node
   * @param rmiPort rmi port of the root node
   * @param username super user username for root node
   * @param password super user password for root node
   */
  public BootstrapDeploymentBuilder generateWebsiteProperties(
      final String ip,
      final int cassandraPort,
      final int rmiPort,
      final String username,
      final String password) {
    this.commands.add(
        new CreateWebsitePropertiesFile(
            ip,
            cassandraPort,
            rmiPort,
            BootstrapDeploymentConstants.LOCAL_TEMP_PROPERTIES_FILE,
            username,
            password));

    this.commands.add(
        new FileTransfer(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.LOCAL_TEMP_PROPERTIES_FILE,
            BootstrapDeploymentConstants.REMOTE_DIRECTORIES_AND_FILES
                .REMOTE_PATHSTORE_ADMIN_PANEL_PROPERTIES_FILE));

    this.commands.add(
        new RemoveGeneratedPropertiesFile(BootstrapDeploymentConstants.LOCAL_TEMP_PROPERTIES_FILE));

    return this;
  }

  /**
   * This function is used to a record to the root nodes local_keyspace.auth table
   *
   * @param connectionUsername username to connect (super user account)
   * @param connectionPassword password to connection
   * @param ip ip to connect to
   * @param port port to connect to
   * @param nodeIdToWrite node id to write
   * @param usernameToWrite username to write for root node
   * @param passwordToWrite password to write for root node
   * @return this
   */
  public BootstrapDeploymentBuilder writeCredentialsToRootNodeBootstrap(
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port,
      final int nodeIdToWrite,
      final String usernameToWrite,
      final String passwordToWrite) {
    this.commands.add(
        new WriteCredentialsToRootNodeBootstrap(
            connectionUsername,
            connectionPassword,
            ip,
            port,
            nodeIdToWrite,
            usernameToWrite,
            passwordToWrite));

    return this;
  }
}

package pathstore.system.deployment.utilities;

import com.datastax.driver.core.Session;
import pathstore.common.Role;
import pathstore.system.deployment.commands.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class is used to create a sequence of commands to perform some operation on a remote host
 */
public class DeploymentBuilder<T extends DeploymentBuilder<T>> {

  /** List of commands */
  protected final List<ICommand> commands = new ArrayList<>();

  /** remote host where you are executing these commands */
  protected final SSHUtil remoteHostConnect;

  public DeploymentBuilder(final SSHUtil remoteHostConnect) {
    this.remoteHostConnect = remoteHostConnect;
  }

  /**
   * This function will kill pathstore and remove its image, then optional force push data then kill
   * cassandra and remove its image, then remove the installation directory
   *
   * @param forcePush if you wish to force push data you need to provide an instance of that class
   * @return this
   */
  public T remove(final ForcePush forcePush) {

    this.commands.add(
        new Exec(this.remoteHostConnect, DeploymentConstants.REMOVAL_COMMANDS.KILL_PATHSTORE, -1));
    this.commands.add(
        new Exec(
            this.remoteHostConnect, DeploymentConstants.REMOVAL_COMMANDS.REMOVE_PATHSTORE, -1));
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            DeploymentConstants.REMOVAL_COMMANDS.REMOVE_PATHSTORE_IMAGE,
            -1));

    if (forcePush != null) this.commands.add(forcePush);

    this.commands.add(
        new Exec(this.remoteHostConnect, DeploymentConstants.REMOVAL_COMMANDS.KILL_CASSANDRA, -1));
    this.commands.add(
        new Exec(
            this.remoteHostConnect, DeploymentConstants.REMOVAL_COMMANDS.REMOVE_CASSANDRA, -1));
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            DeploymentConstants.REMOVAL_COMMANDS.REMOVE_CASSANDRA_IMAGE,
            -1));

    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            DeploymentConstants.REMOVAL_COMMANDS.REMOVE_BASE_DIRECTORY,
            -1));

    return (T) this;
  }

  /**
   * Docker sanity check then remove all prior info, then create a new installation directory
   *
   * @return this
   */
  public T init() {
    this.commands.add(
        new Exec(this.remoteHostConnect, DeploymentConstants.INIT_COMMANDS.DOCKER_CHECK, 0));

    this.remove(null);

    this.commands.add(
        new Exec(
            this.remoteHostConnect, DeploymentConstants.INIT_COMMANDS.CREATE_BASE_DIRECTORY, 0));

    return (T) this;
  }

  /**
   * This function creates a remote directory
   *
   * @param directory directory to create
   * @return this
   */
  public T createRemoteDirectory(final String directory) {
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            String.format(
                DeploymentConstants.CREATE_REMOTE_DIRECTORY.CREATE_SUB_DIRECTORY, directory),
            0));

    return (T) this;
  }

  /**
   * This function will copy a local tar to the remote host and load that tar into the docker
   * registry
   *
   * @param localTar local tar to copy
   * @param remoteTar where to copy this local tar on the remote host
   * @return this
   */
  public T copyAndLoad(final String localTar, final String remoteTar) {
    this.commands.add(new FileTransfer(this.remoteHostConnect, localTar, remoteTar));
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            String.format(DeploymentConstants.COPY_AND_LOAD.LOAD_DOCKER_IMAGE, remoteTar),
            0));

    return (T) this;
  }

  /**
   * This function is used to create a properties file locally, transfer it to a remote destination
   * and then remote the local copy
   *
   * @param nodeID node id
   * @param ip ip
   * @param parentNodeId parent node id
   * @param role role of node
   * @param rmiRegistryIP rmi ip
   * @param rmiRegistryPort rmi port
   * @param rmiRegistryParentIP rmi parent ip
   * @param rmiRegistryParentPort rmi parent port
   * @param cassandraIP cassandra ip
   * @param cassandraPort cassandra port
   * @param cassandraParentIP cassandra parent ip
   * @param cassandraParentPort cassandra parent port
   * @param localProperties where to store the local copy
   * @param remoteProperties where to transfer to
   * @param username username to local cassandra node
   * @param password password to local cassandra node
   * @return this
   */
  public T generatePropertiesFiles(
      final int nodeID,
      final String ip,
      final int parentNodeId,
      final Role role,
      final String rmiRegistryIP,
      final int rmiRegistryPort,
      final String rmiRegistryParentIP,
      final int rmiRegistryParentPort,
      final String cassandraIP,
      final int cassandraPort,
      final String cassandraParentIP,
      final int cassandraParentPort,
      final String localProperties,
      final String remoteProperties,
      final String username,
      final String password) {

    this.commands.add(
        new GeneratePropertiesFile(
            nodeID,
            ip,
            parentNodeId,
            role,
            rmiRegistryIP,
            rmiRegistryPort,
            rmiRegistryParentIP,
            rmiRegistryParentPort,
            cassandraIP,
            cassandraPort,
            cassandraParentIP,
            cassandraParentPort,
            localProperties,
            username,
            password));

    this.commands.add(new FileTransfer(this.remoteHostConnect, localProperties, remoteProperties));

    this.commands.add(new RemoveGeneratedPropertiesFile(localProperties));

    return (T) this;
  }

  /**
   * Create a role on the child node
   *
   * @param connectionUsername username to connect
   * @param connectionPassword password to connect
   * @param ip ip of child
   * @param port port of child
   * @param roleName role to create
   * @param rolePassword role password
   * @param isSuperUser whether the user is a super user or not
   * @return this
   */
  public T createRole(
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port,
      final String roleName,
      final String rolePassword,
      final boolean isSuperUser) {
    this.commands.add(
        new CreateRole(
            connectionUsername, connectionPassword, ip, port, roleName, rolePassword, isSuperUser));
    return (T) this;
  }

  /**
   * Drop a role on the child node
   *
   * @param connectionUsername username to connect
   * @param connectionPassword password to connect
   * @param ip ip of child
   * @param port port of child
   * @param roleName role to drop
   * @return this
   */
  public T dropRole(
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port,
      final String roleName) {
    this.commands.add(new DropRole(connectionUsername, connectionPassword, ip, port, roleName));
    return (T) this;
  }

  /**
   * Grant read and write access on a keyspace to a role on the child
   *
   * @param connectionUsername username to connect
   * @param connectionPassword password to connect
   * @param ip ip of child
   * @param port port of child
   * @param roleName role name to grant to
   * @param keyspace keyspace to grant on
   * @return this
   */
  public T grantReadAndWriteAccess(
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port,
      final String roleName,
      final String keyspace) {
    this.commands.add(
        new GrantReadAndWriteAccess(
            connectionUsername, connectionPassword, ip, port, roleName, keyspace));

    return (T) this;
  }

  /**
   * Write a credential to the child local_keyspace.auth table
   *
   * @param nodeId node id to get credentials from
   * @param connectionUsername username to connect with
   * @param connectionPassword password to connect with
   * @param ip ip of child
   * @param port port of child
   * @return this
   */
  public T writeCredentialsToChildNode(
      final int nodeId,
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port) {
    this.commands.add(
        new WriteCredentialsToChildNode(nodeId, connectionUsername, connectionPassword, ip, port));
    return (T) this;
  }

  /**
   * Write a the child daemon account to the local node cassandra instance
   *
   * @param childNodeId child node id
   * @param username daemon username of child
   * @param password daemon password of child
   * @return this
   */
  public T writeChildAccountToCassandra(
      final int childNodeId, final String username, final String password) {
    this.commands.add(new WriteChildCredentialsToCassandra(childNodeId, username, password));
    return (T) this;
  }

  /**
   * Load a keyspace on the child node
   *
   * @param connectionUsername username to connect with
   * @param connectionPassword password to connect with
   * @param ip ip of child
   * @param port child port
   * @param loadKeyspaceFunction function that consumes a session object to load the keyspace
   * @param keyspaceName keyspace name
   * @return this
   */
  public T loadKeyspace(
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port,
      final Consumer<Session> loadKeyspaceFunction,
      final String keyspaceName) {
    this.commands.add(
        new LoadKeyspace(
            connectionUsername, connectionPassword, ip, port, loadKeyspaceFunction, keyspaceName));
    return (T) this;
  }

  /**
   * Remove a credential from the local nodes local_keyspace.auth table
   *
   * @param nodeId node id to remove (As it must already be in the credential cache)
   * @return this
   */
  public T removeLocalCredential(final int nodeId) {
    this.commands.add(new RemoveLocalCredential(nodeId));
    return (T) this;
  }

  /**
   * Delete a nodes history on un-deployment
   *
   * @param newNodeId node id (child node)
   * @param parentNodeId parent node id (current node)
   * @return this
   */
  public T deleteNodeHistory(final int newNodeId, final int parentNodeId) {
    this.commands.add(new DeleteNodeHistory(newNodeId, parentNodeId));
    return (T) this;
  }

  /**
   * This function will run some command remotely to start a container and then pass a wait object
   * to wait to determine if the process inside the container has successfully started up
   *
   * @param runCommand command to start the container
   * @param waitObject wait object to wait for the container
   * @return this
   */
  public T startImageAndWait(final String runCommand, final ICommand waitObject) {
    this.commands.add(new Exec(this.remoteHostConnect, runCommand, 0));

    if (waitObject != null) this.commands.add(waitObject);

    return (T) this;
  }

  /**
   * Add a custom command to the sequence
   *
   * @param command command to add
   * @return this
   */
  public T custom(final ICommand command) {
    this.commands.add(command);

    return (T) this;
  }

  /** @return list of commands in order */
  public List<ICommand> build() {
    return this.commands;
  }
}

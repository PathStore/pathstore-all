package pathstore.system.deployment.utilities;

import com.datastax.driver.core.Session;
import lombok.RequiredArgsConstructor;
import pathstore.authentication.credentials.AuxiliaryCredential;
import pathstore.authentication.credentials.Credential;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.authentication.credentials.NodeCredential;
import pathstore.authentication.datalayerimpls.AuxiliaryDataLayer;
import pathstore.authentication.datalayerimpls.NodeDataLayer;
import pathstore.common.Role;
import pathstore.system.deployment.commands.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class is used to create a sequence of commands to perform some operation on a remote host
 */
@RequiredArgsConstructor
public class DeploymentBuilder<T extends DeploymentBuilder<T>> {

  /** List of commands */
  protected final List<ICommand> commands = new ArrayList<>();

  /** remote host where you are executing these commands */
  protected final SSHUtil remoteHostConnect;

  /**
   * This function will remove pathstore, optionally force push, then remove cassandra. Then it will
   * clean up any left over directories
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

    if (forcePush != null) this.commands.add(forcePush);

    this.commands.add(
        new Exec(this.remoteHostConnect, DeploymentConstants.REMOVAL_COMMANDS.KILL_CASSANDRA, -1));

    this.commands.add(
        new Exec(
            this.remoteHostConnect, DeploymentConstants.REMOVAL_COMMANDS.REMOVE_CASSANDRA, -1));

    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            DeploymentConstants.REMOVAL_COMMANDS.REMOVE_BASE_DIRECTORY,
            -1));

    this.commands.add(
        new Exec(
            this.remoteHostConnect, DeploymentConstants.REMOVAL_COMMANDS.REMOVE_DOCKER_CERTS, -1));

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
   * This function is used to copy the pathstore-registry certificates from the current node to the
   * new child
   *
   * @param registryIP registry ip
   * @return this
   */
  public T copyRegistryCertificate(final String registryIP) {
    this.commands.add(
        new FileTransfer(
            this.remoteHostConnect,
            "/etc/pathstore/pathstore-registry.crt",
            "pathstore-install/pathstore/pathstore-registry.crt"));

    return (T) this;
  }

  /**
   * This function is used to load the registry certificaates on the child node
   *
   * @param registryIP registry ip
   * @return this
   */
  public T loadRegistryCertificateOnChild(final String registryIP) {
    String dir = String.format("/etc/docker/certs.d/%s", registryIP);

    // create certs directory
    this.commands.add(new Exec(this.remoteHostConnect, String.format("mkdir -p %s", dir), 0));

    // copy cert into directory
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            String.format(
                "cp ~/pathstore-install/pathstore/pathstore-registry.crt /etc/docker/certs.d/%s/ca.crt",
                registryIP),
            0));

    // set dir group
    this.commands.add(
        new Exec(this.remoteHostConnect, String.format("chgrp docker -R %s", dir), 0));

    // set dir permissions
    this.commands.add(new Exec(this.remoteHostConnect, String.format("chmod 775 -R %s", dir), 0));

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
   * @param grpcIP grpc ip
   * @param grpcPort grpc port
   * @param grpcParentIP grpc parent ip
   * @param grpcParentPort grpc parent port
   * @param cassandraIP cassandra ip
   * @param cassandraPort cassandra port
   * @param cassandraParentIP cassandra parent ip
   * @param cassandraParentPort cassandra parent port
   * @param localProperties where to store the local copy
   * @param remoteProperties where to transfer to
   * @param username username to local cassandra node
   * @param password password to local cassandra node
   * @param registryIP registry ip to pull containers from
   * @param pathstoreVersion version to install from registry
   * @return this
   */
  public T generatePropertiesFiles(
      final int nodeID,
      final String ip,
      final int parentNodeId,
      final Role role,
      final String grpcIP,
      final int grpcPort,
      final String grpcParentIP,
      final int grpcParentPort,
      final String cassandraIP,
      final int cassandraPort,
      final String cassandraParentIP,
      final int cassandraParentPort,
      final String localProperties,
      final String remoteProperties,
      final String username,
      final String password,
      final String registryIP,
      final String pathstoreVersion) {

    this.commands.add(
        new GeneratePropertiesFile(
            nodeID,
            ip,
            parentNodeId,
            role,
            grpcIP,
            grpcPort,
            grpcParentIP,
            grpcParentPort,
            cassandraIP,
            cassandraPort,
            cassandraParentIP,
            cassandraParentPort,
            localProperties,
            username,
            password,
            registryIP,
            pathstoreVersion));

    this.commands.add(new FileTransfer(this.remoteHostConnect, localProperties, remoteProperties));

    this.commands.add(new RemoveGeneratedPropertiesFile(localProperties));

    return (T) this;
  }

  /**
   * Create a role on the child node
   *
   * @param deploymentCredential connection credential
   * @param credential of account to write
   * @param isSuperUser whether the user is a super user or not
   * @return this
   */
  public T createRole(
      final DeploymentCredential deploymentCredential,
      final Credential<?> credential,
      final boolean isSuperUser) {
    this.commands.add(new CreateRole(deploymentCredential, credential, isSuperUser));
    return (T) this;
  }

  /**
   * Drop a role on the child node
   *
   * @param cassandraCredentials cassandra credentials to connect with
   * @param roleName role to drop
   * @return this
   */
  public T dropRole(final DeploymentCredential cassandraCredentials, final String roleName) {
    this.commands.add(new DropRole(cassandraCredentials, roleName));
    return (T) this;
  }

  /**
   * Grant read and write access on a keyspace to a role on the child
   *
   * @param cassandraCredentials cassandra credentials to connect with
   * @param roleName role name to grant to
   * @param keyspace keyspace to grant on
   * @return this
   */
  public T grantReadAndWriteAccess(
      final DeploymentCredential cassandraCredentials,
      final String roleName,
      final String keyspace) {
    this.commands.add(new GrantReadAndWriteAccess(cassandraCredentials, roleName, keyspace));

    return (T) this;
  }

  /**
   * Write a credential to the child pathstore_applications.local_auth table
   *
   * @param credential creedentials to write
   * @param cassandraCredentials cassandra credentials to connect with
   * @return this
   */
  public T writeNodeCredentialToChildNode(
      final NodeCredential credential, final DeploymentCredential cassandraCredentials) {
    this.commands.add(
        new WriteCredentialToChildNode<>(
            NodeDataLayer.getInstance(), credential, cassandraCredentials));
    return (T) this;
  }

  /**
   * Write a credential to the child pathstore_applications.local_auth table
   *
   * @param credential auxiliary credential object to pass
   * @param cassandraCredentials cassandra credential to connect to
   * @return this
   */
  public T writeAuxiliaryCredentialToChildNode(
      final AuxiliaryCredential credential, final DeploymentCredential cassandraCredentials) {
    this.commands.add(
        new WriteCredentialToChildNode<>(
            AuxiliaryDataLayer.getInstance(), credential, cassandraCredentials));
    return (T) this;
  }

  /**
   * Write a the child daemon account to the local node cassandra instance
   *
   * @param childCredential child credential to write
   * @return this
   */
  public T writeChildAccountToCassandra(final NodeCredential childCredential) {
    this.commands.add(new WriteChildCredentialsToCassandra(childCredential));
    return (T) this;
  }

  /**
   * Load a keyspace on the child node
   *
   * @param cassandraCredentials cassandra credentials to connect with
   * @param loadKeyspaceFunction function that consumes a session object to load the keyspace
   * @param keyspaceName keyspace name
   * @return this
   */
  public T loadKeyspace(
      final DeploymentCredential cassandraCredentials,
      final Consumer<Session> loadKeyspaceFunction,
      final String keyspaceName) {
    this.commands.add(new LoadKeyspace(cassandraCredentials, loadKeyspaceFunction, keyspaceName));
    return (T) this;
  }

  /**
   * Remove a credential from the local nodes pathstore_applications.local_auth table
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

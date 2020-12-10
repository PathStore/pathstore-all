package pathstore.system.deployment.utilities;

import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.authentication.CredentialCache;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.authentication.credentials.NodeCredential;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.Role;
import pathstore.common.tables.DeploymentEntry;
import pathstore.common.tables.ServerEntry;
import pathstore.system.deployment.commands.*;
import pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Things related to cassandra for startup that can't rely on pathstore properties file */
public class StartupUTIL {
  /**
   * Convert a local relative path to an absolute
   *
   * @param relativePath local relative path
   * @return local absolute path
   * @throws CommandError if relative path is invalid
   */
  public static String getAbsolutePathFromRelativePath(final String relativePath)
      throws CommandError {
    try {
      return new File(relativePath).getCanonicalPath();
    } catch (IOException e) {
      throw new CommandError(
          String.format("We where unable to convert %s to its absolute path", relativePath));
    }
  }

  /**
   * @param sshUtil used for commands that need to use ssh
   * @param ip ip of new node
   * @param nodeID new node's id
   * @param parentNodeId new node's parent id
   * @param role role of new node
   * @param grpcIP new node's local grpc ip
   * @param grpcPort new node's local grpc port
   * @param grpcParentIP new node's parent grpc ip
   * @param grpcParentPort new node's parent grpc port
   * @param cassandraIP new node's local cassandra instance ip
   * @param cassandraPort new node's local cassandra instance port
   * @param cassandraParentIP new node's parent cassandra instance ip
   * @param cassandraParentPort new nodes' parent cassandra instance port
   * @return list of deployment commands to execute
   */
  public static List<ICommand> initDeploymentList(
      final SSHUtil sshUtil,
      final String ip,
      final int nodeID,
      final int parentNodeId,
      final Role role,
      final String grpcIP,
      final int grpcPort,
      final String grpcParentIP,
      final int grpcParentPort,
      final String cassandraIP,
      final int cassandraPort,
      final String cassandraParentIP,
      final int cassandraParentPort) {

    DeploymentCredential defaultLogin =
        new DeploymentCredential(
            Constants.DEFAULT_CASSANDRA_USERNAME,
            Constants.DEFAULT_CASSANDRA_PASSWORD,
            ip,
            cassandraPort);

    DeploymentCredential childSuperUserCredential =
        new DeploymentCredential(
            Constants.PATHSTORE_SUPERUSER_USERNAME,
            CassandraAuthenticationUtil.generateAlphaNumericPassword(),
            ip,
            cassandraPort);

    NodeCredential childDaemonCredential =
        new NodeCredential(
            nodeID,
            Constants.PATHSTORE_DAEMON_USERNAME,
            CassandraAuthenticationUtil.generateAlphaNumericPassword());

    String registryIP = PathStoreProperties.getInstance().registryIP;

    return new DeploymentBuilder<>(sshUtil)
        .init(registryIP)
        .createRemoteDirectory(DeploymentConstants.REMOTE_PATHSTORE_LOGS_SUB_DIR)
        .copyRegistryCertificate()
        .loadRegistryCertificateOnChild(registryIP)
        .generatePropertiesFiles(
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
            childSuperUserCredential.getUsername(),
            childSuperUserCredential.getPassword(),
            registryIP,
            PathStoreProperties.getInstance().pathstoreVersion,
            DeploymentConstants.GENERATE_PROPERTIES.LOCAL_TEMP_PROPERTIES_FILE(nodeID),
            DeploymentConstants.GENERATE_PROPERTIES.REMOTE_PATHSTORE_PROPERTIES_FILE)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.CASSANDRA_REMOVE_TAG(registryIP),
            DeploymentConstants.RUN_COMMANDS.CASSANDRA_RUN(registryIP),
            new WaitForCassandra(defaultLogin))
        .createRole(defaultLogin, childSuperUserCredential, true)
        .dropRole(childSuperUserCredential, Constants.DEFAULT_CASSANDRA_USERNAME)
        .loadKeyspace(
            childSuperUserCredential,
            PathStoreSchemaLoaderUtils::loadApplicationSchema,
            Constants.PATHSTORE_APPLICATIONS)
        .createRole(childSuperUserCredential, childDaemonCredential, false)
        .grantReadAndWriteAccess(
            childSuperUserCredential,
            childDaemonCredential.getUsername(),
            Constants.PATHSTORE_APPLICATIONS)
        .createRole(
            childSuperUserCredential,
            CredentialCache.getAuxiliary()
                .getCredential(Constants.AUXILIARY_ACCOUNTS.NETWORK_ADMINISTRATOR),
            true)
        .writeChildAccountToCassandra(childDaemonCredential)
        .writeNodeCredentialToChildNode( // Writes parent credentials to child node
            CredentialCache.getNodes().getCredential(parentNodeId), childSuperUserCredential)
        .writeNodeCredentialToChildNode( // Writes daemon account to child node
            childDaemonCredential, childSuperUserCredential)
        .writeAuxiliaryCredentialToChildNode( // Writes network admin account
            CredentialCache.getAuxiliary()
                .getCredential(Constants.AUXILIARY_ACCOUNTS.NETWORK_ADMINISTRATOR),
            childSuperUserCredential)
        .writeAuxiliaryCredentialToChildNode( // Writes network wide grpc credential
            CredentialCache.getAuxiliary()
                .getCredential(Constants.AUXILIARY_ACCOUNTS.NETWORK_WIDE_GRPC_CREDENTIAL),
            childSuperUserCredential)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.PATHSTORE_REMOVE_TAG(
                registryIP, PathStoreProperties.getInstance().pathstoreVersion),
            DeploymentConstants.RUN_COMMANDS.PATHSTORE_RUN(
                registryIP, PathStoreProperties.getInstance().pathstoreVersion),
            new WaitForPathStore(childSuperUserCredential))
        .build();
  }

  /**
   * This function generate the list of commands to remove a node
   *
   * @param sshUtil how to connect
   * @return list of removal commands
   */
  public static List<ICommand> initUnDeploymentList(
      final SSHUtil sshUtil, final DeploymentEntry deploymentEntry, final ServerEntry serverEntry) {
    return new DeploymentBuilder<>(sshUtil)
        .removeLocalCredential(deploymentEntry.newNodeId)
        .remove(
            new ForcePush(deploymentEntry.newNodeId, serverEntry.ip, serverEntry.cassandraPort),
            PathStoreProperties.getInstance().registryIP)
        .deleteNodeHistory(deploymentEntry.newNodeId, deploymentEntry.parentNodeId)
        .build();
  }
}

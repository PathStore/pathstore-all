package pathstore.system.deployment.utilities;

import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.authentication.CredentialCache;
import pathstore.common.Constants;
import pathstore.common.Role;
import pathstore.system.deployment.commands.*;
import pathstore.common.tables.DeploymentEntry;
import pathstore.common.tables.ServerEntry;
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

    String childSuperuserUsername = Constants.PATHSTORE_SUPERUSER_USERNAME;
    String childSuperuserPassword = CassandraAuthenticationUtil.generateAlphaNumericPassword();

    String childDaemonUsername = Constants.PATHSTORE_DAEMON_USERNAME;
    String childDaemonPassword = CassandraAuthenticationUtil.generateAlphaNumericPassword();

    return new DeploymentBuilder<>(sshUtil)
        .init()
        .createRemoteDirectory(DeploymentConstants.REMOTE_PATHSTORE_LOGS_SUB_DIR)
        .copyAndLoad(
            DeploymentConstants.COPY_AND_LOAD.LOCAL_CASSANDRA_TAR,
            DeploymentConstants.COPY_AND_LOAD.REMOTE_CASSANDRA_TAR)
        .copyAndLoad(
            DeploymentConstants.COPY_AND_LOAD.LOCAL_PATHSTORE_TAR,
            DeploymentConstants.COPY_AND_LOAD.REMOTE_PATHSTORE_TAR)
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
            DeploymentConstants.GENERATE_PROPERTIES.LOCAL_TEMP_PROPERTIES_FILE,
            DeploymentConstants.GENERATE_PROPERTIES.REMOTE_PATHSTORE_PROPERTIES_FILE,
            childSuperuserUsername,
            childSuperuserPassword)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.CASSANDRA_RUN, new WaitForCassandra(ip, cassandraPort))
        .createRole(
            Constants.DEFAULT_CASSANDRA_USERNAME,
            Constants.DEFAULT_CASSANDRA_PASSWORD,
            ip,
            cassandraPort,
            childSuperuserUsername,
            childSuperuserPassword,
            true)
        .dropRole(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            Constants.DEFAULT_CASSANDRA_USERNAME)
        .loadKeyspace(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            PathStoreSchemaLoaderUtils::loadApplicationSchema,
            Constants.PATHSTORE_APPLICATIONS)
        .createRole(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            childDaemonUsername,
            childDaemonPassword,
            false)
        .grantReadAndWriteAccess(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            childDaemonUsername,
            Constants.PATHSTORE_APPLICATIONS)
        .createRole(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            CredentialCache.getNodeAuth().getCredential(-1).username,
            CredentialCache.getNodeAuth().getCredential(-1).password,
            true)
        .writeChildAccountToCassandra(nodeID, childDaemonUsername, childDaemonPassword)
        .writeCredentialsToChildNode( // Writes parent credentials to child node
            parentNodeId, childSuperuserUsername, childSuperuserPassword, ip, cassandraPort)
        .writeCredentialsToChildNode( // Writes daemon account to child node
            nodeID, childSuperuserUsername, childSuperuserPassword, ip, cassandraPort)
        .writeCredentialsToChildNode( // Writes network admin account
            -1, childSuperuserUsername, childSuperuserPassword, ip, cassandraPort)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.PATHSTORE_RUN,
            new WaitForPathStore(childSuperuserUsername, childSuperuserPassword, ip, cassandraPort))
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
        .remove(new ForcePush(deploymentEntry.newNodeId, serverEntry.ip, serverEntry.cassandraPort))
        .deleteNodeHistory(deploymentEntry.newNodeId, deploymentEntry.parentNodeId)
        .build();
  }
}

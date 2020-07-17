package pathstore.system.deployment.utilities;

import pathstore.common.PathStoreProperties;
import pathstore.common.Role;
import pathstore.system.deployment.commands.*;

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
   * @param rmiRegistryIP new node's local rmi registry ip
   * @param rmiRegistryPort new node's local rmi registry port
   * @param rmiRegistryParentIP new node's parent rmi registry ip
   * @param rmiRegistryParentPort new node's parent rmi registry port
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
      final String rmiRegistryIP,
      final int rmiRegistryPort,
      final String rmiRegistryParentIP,
      final int rmiRegistryParentPort,
      final String cassandraIP,
      final int cassandraPort,
      final String cassandraParentIP,
      final int cassandraParentPort) {

    String childUserName = "cassandra";
    String childPassword = "cassandra";

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
            rmiRegistryIP,
            rmiRegistryPort,
            rmiRegistryParentIP,
            rmiRegistryParentPort,
            cassandraIP,
            cassandraPort,
            cassandraParentIP,
            cassandraParentPort,
            DeploymentConstants.GENERATE_PROPERTIES.LOCAL_TEMP_PROPERTIES_FILE,
            DeploymentConstants.GENERATE_PROPERTIES.REMOTE_PATHSTORE_PROPERTIES_FILE,
            childUserName,
            childPassword)
        .writeChildUserAccountToCassandra(nodeID, childUserName, childPassword)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.CASSANDRA_RUN,
            new WaitForCassandra(childUserName, childPassword, ip, cassandraPort))
        .writeUserAccountToChild(
            PathStoreProperties.getInstance().credential,
            childUserName,
            childPassword,
            ip,
            cassandraPort)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.PATHSTORE_RUN,
            new WaitForPathStore(childUserName, childPassword, ip, cassandraPort))
        .build();
  }

  /**
   * This function generate the list of commands to remove a node
   *
   * @param sshUtil how to connect
   * @return list of removal commands
   */
  public static List<ICommand> initUnDeploymentList(
      final SSHUtil sshUtil, final String ip, final int cassandraPort, final int newNodeId) {
    return new DeploymentBuilder<>(sshUtil)
        .remove(new ForcePush(newNodeId, ip, cassandraPort))
        .build();
  }
}

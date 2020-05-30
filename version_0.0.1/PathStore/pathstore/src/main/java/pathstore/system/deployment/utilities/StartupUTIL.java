package pathstore.system.deployment.utilities;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;
import pathstore.common.Constants;
import pathstore.common.Role;
import pathstore.system.deployment.commands.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static pathstore.common.Constants.PROPERTIES_CONSTANTS.*;

/** Things related to cassandra for startup that can't rely on pathstore properties file */
public class StartupUTIL {

  /** Denotes the dir where all installation related files are stored */
  private static final String STARTING_DIR =
      System.getProperty("user.dir") + "/src/main/resources/";

  /** Where the properties file will be stored locally. */
  public static final String DESTINATION_TO_STORE = STARTING_DIR + "pathstore.properties";

  /**
   * Used to create a cluster connection with an ip and port
   *
   * @param ip ip of cassandra server
   * @param port port cassandra is running on
   * @return created cluster
   */
  public static Cluster createCluster(final String ip, final int port) {
    return new Cluster.Builder()
        .addContactPoints(ip)
        .withPort(port)
        .withSocketOptions((new SocketOptions()).setTcpNoDelay(true).setReadTimeoutMillis(15000000))
        .withQueryOptions(
            (new QueryOptions())
                .setRefreshNodeIntervalMillis(0)
                .setRefreshNodeListIntervalMillis(0)
                .setRefreshSchemaIntervalMillis(0))
        .build();
  }

  /**
   * @param nodeID node id of node
   * @param ip public ip of node
   * @param parentNodeId parent node id of node (-1 if root)
   * @param role role of server (ROOTSERVER, SERVER)
   * @param rmiRegistryIP rmi ip of local node (should be localhost)
   * @param rmiRegistryPort rmi port for local rmi connection
   * @param rmiRegistryParentIP rmi ip of parent node (only of role is SERVER)
   * @param rmiRegistryParentPort rmi port of parent node (only if role is SERVER)
   * @param cassandraIP cassandra ip of attached cassandra instance
   * @param cassandraPort cassandra port of attached cassandra instance
   * @param cassandraParentIP cassandra ip of parent cassandra instance (only if role is SERVER)
   * @param cassandraParentPort cassandra port of parent cassandra instance (only if role is SERVER)
   * @return generate properties file (You need to write it to {@link Constants#PROPERTIESFILE})
   */
  public static Properties generatePropertiesFile(
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
      final int cassandraParentPort) {

    Properties properties = new Properties();

    properties.put(NODE_ID, String.valueOf(nodeID));
    properties.put(EXTERNAL_ADDRESS, ip);
    properties.put(PARENT_ID, String.valueOf(parentNodeId));
    properties.put(ROLE, role.toString());
    properties.put(RMI_REGISTRY_IP, rmiRegistryIP);
    properties.put(RMI_REGISTRY_PORT, String.valueOf(rmiRegistryPort));
    properties.put(RMI_REGISTRY_PARENT_IP, rmiRegistryParentIP);
    properties.put(RMI_REGISTRY_PARENT_PORT, String.valueOf(rmiRegistryParentPort));
    properties.put(CASSANDRA_IP, cassandraIP);
    properties.put(CASSANDRA_PORT, String.valueOf(cassandraPort));
    properties.put(CASSANDRA_PARENT_IP, cassandraParentIP);
    properties.put(CASSANDRA_PARENT_PORT, String.valueOf(cassandraParentPort));
    properties.put(PUSH_SLEEP, String.valueOf(1000));
    properties.put(PULL_SLEEP, String.valueOf(1000));

    return properties;
  }

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

    String modifiedDestinationToStore = String.format("%s-%s", DESTINATION_TO_STORE, ip);

    List<ICommand> commands = new ArrayList<>();

    // Check for docker access and that docker is online
    commands.add(new Exec(sshUtil, "docker ps", 0));
    // Potentially kill old cassandra container
    commands.add(new Exec(sshUtil, "docker kill cassandra && docker rm cassandra", -1));
    // Potentially remove old cassandra image
    commands.add(new Exec(sshUtil, "docker image rm cassandra", -1));
    // Potentially kill old pathstore container
    commands.add(new Exec(sshUtil, "docker kill pathstore && docker rm pathstore", -1));
    // Potentially remove old pathstore image
    commands.add(new Exec(sshUtil, "docker image rm pathstore", -1));
    // Potentially remove old file associated with install
    commands.add(new Exec(sshUtil, "rm -rf pathstore-install", -1));
    // Create pathstore install dir and logs dir
    commands.add(new Exec(sshUtil, "mkdir -p pathstore-install/logs", 0));
    // Generate pathstore properties file
    commands.add(
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
            modifiedDestinationToStore));
    // Transfer properties file
    commands.add(
        new FileTransfer(
            sshUtil, modifiedDestinationToStore, "pathstore-install/pathstore.properties"));
    // Remove properties file
    commands.add(new RemoveGeneratedPropertiesFile(modifiedDestinationToStore));
    // Transfer cassandra image
    commands.add(
        new FileTransfer(
            sshUtil, "/etc/pathstore/cassandra.tar", "pathstore-install/cassandra.tar"));
    // Load cassandra
    commands.add(new Exec(sshUtil, "docker load -i pathstore-install/cassandra.tar", 0));
    // Start cassandra
    commands.add(
        new Exec(
            sshUtil,
            "docker run --network=host -dit --restart always --user $(id -u):$(id -g) --name cassandra cassandra",
            0));
    // Wait for cassandra to start
    commands.add(new WaitForCassandra(ip, cassandraPort));
    // Transfer pathstore image
    commands.add(
        new FileTransfer(
            sshUtil, "/etc/pathstore/pathstore.tar", "pathstore-install/pathstore.tar"));
    // Load pathstore
    commands.add(new Exec(sshUtil, "docker load -i pathstore-install/pathstore.tar", 0));
    // Start pathstore
    commands.add(
        new Exec(
            sshUtil,
            "docker run --network=host -dit --restart always -v ~/pathstore-install:/etc/pathstore --user $(id -u):$(id -g) --name pathstore pathstore",
            0));
    // Wait for pathstore to come online
    commands.add(new WaitForPathStore(ip, cassandraPort));

    return commands;
  }

  /**
   * This function generate the list of commands to remove a node
   *
   * @param sshUtil how to connect
   * @return list of removal commands
   */
  public static List<ICommand> initUnDeploymentList(final SSHUtil sshUtil) {
    List<ICommand> commands = new ArrayList<>();

    // Check for docker access and that docker is online
    commands.add(new Exec(sshUtil, "docker ps", 0));
    // Potentially kill old pathstore container
    commands.add(new Exec(sshUtil, "docker kill pathstore && docker rm pathstore", 0));
    // Potentially remove old pathstore image
    commands.add(new Exec(sshUtil, "docker image rm pathstore", 0));
    // Potentially kill old cassandra container
    commands.add(new Exec(sshUtil, "docker kill cassandra && docker rm pathstore", 0));
    // Potentially remove old cassandra image
    commands.add(new Exec(sshUtil, "docker image rm cassandra", 0));
    // Potentially remove old file associated with install
    commands.add(new Exec(sshUtil, "rm -rf pathstore-install", 0));

    return commands;
  }
}

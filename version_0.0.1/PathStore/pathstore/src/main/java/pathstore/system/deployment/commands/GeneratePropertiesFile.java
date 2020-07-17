package pathstore.system.deployment.commands;

import pathstore.common.Role;
import pathstore.system.deployment.utilities.StartupUTIL;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import static pathstore.common.Constants.PROPERTIES_CONSTANTS.*;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.PULL_SLEEP;

/**
 * This command is used to generate a pathstore properties file and have it available to be able to
 * load it into the docker container
 */
public class GeneratePropertiesFile implements ICommand {

  /** Node id of new node */
  private final int nodeID;

  /** Public ip of server */
  private final String ip;

  /** New nodes parent id */
  private final int parentNodeId;

  /** Role of new node */
  private final Role role;

  /** Rmi registry ip of new node */
  private final String rmiRegistryIP;

  /** Rmi registry port of new ndoe */
  private final int rmiRegistryPort;

  /** Rmi registry ip of new node's parent */
  private final String rmiRegistryParentIP;

  /** Rmi registry port of new node's parent */
  private final int rmiRegistryParentPort;

  /** Cassandra ip of new node */
  private final String cassandraIP;

  /** Cassandra port of new node */
  private final int cassandraPort;

  /** Cassandra ip of new node's parent */
  private final String cassandraParentIP;

  /** Cassandra port of new nodes' parent */
  private final int cassandraParentPort;

  /** Where to store the generate pathstore file */
  private final String destinationToStore;

  /** Super user account for cassandra */
  private final String username;

  /** Super user account for cassandra */
  private final String password;

  /**
   * @param nodeID {@link #nodeID}
   * @param ip {@link #ip}
   * @param parentNodeId {@link #parentNodeId}
   * @param role {@link #role}
   * @param rmiRegistryIP {@link #rmiRegistryIP}
   * @param rmiRegistryPort {@link #rmiRegistryPort}
   * @param rmiRegistryParentIP {@link #rmiRegistryParentIP}
   * @param rmiRegistryParentPort {@link #rmiRegistryParentPort}
   * @param cassandraIP {@link #cassandraIP}
   * @param cassandraPort {@link #cassandraPort}
   * @param cassandraParentIP {@link #cassandraParentIP}
   * @param cassandraParentPort {@link #cassandraParentPort}
   * @param destinationToStore {@link #destinationToStore}
   */
  public GeneratePropertiesFile(
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
      final String destinationToStore,
      final String username,
      final String password) {
    this.nodeID = nodeID;
    this.ip = ip;
    this.parentNodeId = parentNodeId;
    this.role = role;
    this.rmiRegistryIP = rmiRegistryIP;
    this.rmiRegistryPort = rmiRegistryPort;
    this.rmiRegistryParentIP = rmiRegistryParentIP;
    this.rmiRegistryParentPort = rmiRegistryParentPort;
    this.cassandraIP = cassandraIP;
    this.cassandraPort = cassandraPort;
    this.cassandraParentIP = cassandraParentIP;
    this.cassandraParentPort = cassandraParentPort;
    this.destinationToStore = destinationToStore;
    this.username = username;
    this.password = password;
  }

  /**
   * This command will generate a properties file for a new node to be loaded into the docker
   * container for pathstore.
   *
   * @throws CommandError contains a message to denote what went wrong
   */
  @Override
  public void execute() throws CommandError {
    Properties properties = new Properties();

    properties.put(NODE_ID, String.valueOf(this.nodeID));
    properties.put(EXTERNAL_ADDRESS, this.ip);
    properties.put(PARENT_ID, String.valueOf(this.parentNodeId));
    properties.put(ROLE, this.role.toString());
    properties.put(RMI_REGISTRY_IP, this.rmiRegistryIP);
    properties.put(RMI_REGISTRY_PORT, String.valueOf(this.rmiRegistryPort));
    properties.put(RMI_REGISTRY_PARENT_IP, this.rmiRegistryParentIP);
    properties.put(RMI_REGISTRY_PARENT_PORT, String.valueOf(this.rmiRegistryParentPort));
    properties.put(CASSANDRA_IP, this.cassandraIP);
    properties.put(CASSANDRA_PORT, String.valueOf(this.cassandraPort));
    properties.put(CASSANDRA_PARENT_IP, this.cassandraParentIP);
    properties.put(CASSANDRA_PARENT_PORT, String.valueOf(this.cassandraParentPort));
    properties.put(PUSH_SLEEP, String.valueOf(1000));
    properties.put(PULL_SLEEP, String.valueOf(1000));
    properties.put(USERNAME, this.username);
    properties.put(PASSWORD, this.password);

    try {
      OutputStream outputStream =
          new FileOutputStream(
              StartupUTIL.getAbsolutePathFromRelativePath(this.destinationToStore));
      properties.store(outputStream, null);
    } catch (IOException e) {
      throw new CommandError(
          String.format("Could not write the properties file to %s", this.destinationToStore));
    }
  }

  /** @return displays to user what the contents of the properties file will have */
  @Override
  public String toString() {
    return String.format(
        "Generating properties file with the following parameters: NodeID: %d, IP: %s, ParentNodeId: %d, Role: %s, RMIRegistryIP: %s, RMIRegistryPort: %d, RMIRegistryParentIP: %s, RMIRegistryParentPort: %d, CassandraIP: %s, CassandraPort: %d, CassandraParentIP: %s, CassandraParentPort: %d, Destination: %s",
        this.nodeID,
        this.ip,
        this.parentNodeId,
        this.role.toString(),
        this.rmiRegistryIP,
        this.rmiRegistryPort,
        this.rmiRegistryParentIP,
        this.rmiRegistryParentPort,
        this.cassandraIP,
        this.cassandraPort,
        this.cassandraParentIP,
        this.cassandraParentPort,
        this.destinationToStore);
  }
}

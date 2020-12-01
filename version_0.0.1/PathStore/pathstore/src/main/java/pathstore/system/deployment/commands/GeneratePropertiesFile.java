package pathstore.system.deployment.commands;

import static pathstore.common.Constants.PROPERTIES_CONSTANTS.CASSANDRA_IP;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.CASSANDRA_PARENT_IP;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.CASSANDRA_PARENT_PORT;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.CASSANDRA_PORT;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.EXTERNAL_ADDRESS;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.GRPC_IP;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.GRPC_PARENT_IP;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.GRPC_PARENT_PORT;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.GRPC_PORT;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.NODE_ID;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.PARENT_ID;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.PASSWORD;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.PATHSTORE_VERSION;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.PULL_SLEEP;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.PUSH_SLEEP;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.REGISTRY_IP;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.ROLE;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.USERNAME;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import pathstore.common.Role;
import pathstore.system.deployment.utilities.StartupUTIL;

/**
 * This command is used to generate a pathstore properties file and have it available to be able to
 * load it into the docker container
 */
@RequiredArgsConstructor
public class GeneratePropertiesFile implements ICommand {

  /** Node id of new node */
  private final int nodeID;

  /** Public ip of server */
  private final String ip;

  /** New nodes parent id */
  private final int parentNodeId;

  /** Role of new node */
  private final Role role;

  /** grpc ip of new node */
  private final String grpcIP;

  /** grpc port of new ndoe */
  private final int grpcPort;

  /** grpc ip of new node's parent */
  private final String grpcParentIP;

  /** grpc port of new node's parent */
  private final int grpcParentPort;

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

  /** Registry ip to pull containers from */
  private final String registryIP;

  /** PathStore version */
  private final String pathstoreVersion;

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
    properties.put(GRPC_IP, this.grpcIP);
    properties.put(GRPC_PORT, String.valueOf(this.grpcPort));
    properties.put(GRPC_PARENT_IP, this.grpcParentIP);
    properties.put(GRPC_PARENT_PORT, String.valueOf(this.grpcParentPort));
    properties.put(CASSANDRA_IP, this.cassandraIP);
    properties.put(CASSANDRA_PORT, String.valueOf(this.cassandraPort));
    properties.put(CASSANDRA_PARENT_IP, this.cassandraParentIP);
    properties.put(CASSANDRA_PARENT_PORT, String.valueOf(this.cassandraParentPort));
    properties.put(PUSH_SLEEP, String.valueOf(1000));
    properties.put(PULL_SLEEP, String.valueOf(1000));
    properties.put(USERNAME, this.username);
    properties.put(PASSWORD, this.password);
    properties.put(REGISTRY_IP, this.registryIP);
    properties.put(PATHSTORE_VERSION, this.pathstoreVersion);

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
        "Generating properties file with the following parameters: NodeID: %d, IP: %s, ParentNodeId: %d, Role: %s, GRPCIP: %s, GRPCPort: %d, GRPCParentIP: %s, GRPCParentPort: %d, CassandraIP: %s, CassandraPort: %d, CassandraParentIP: %s, CassandraParentPort: %d, Destination: %s",
        this.nodeID,
        this.ip,
        this.parentNodeId,
        this.role.toString(),
        this.grpcIP,
        this.grpcPort,
        this.grpcParentIP,
        this.grpcParentPort,
        this.cassandraIP,
        this.cassandraPort,
        this.cassandraParentIP,
        this.cassandraParentPort,
        this.destinationToStore);
  }
}

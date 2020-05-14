package pathstore.system.deployment.utilities;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;
import pathstore.common.Constants;
import pathstore.common.Role;
import pathstore.system.deployment.commands.*;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static pathstore.common.Constants.PROPERTIES_CONSTANTS.*;

/** Things related to cassandra for startup that can't rely on pathstore properties file */
public class StartupUTIL {

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
}

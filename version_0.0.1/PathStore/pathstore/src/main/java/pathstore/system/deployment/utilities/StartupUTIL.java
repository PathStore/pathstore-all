package pathstore.system.deployment.utilities;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.Constants;
import pathstore.common.Role;
import pathstore.system.deployment.commands.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

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
   * This function rights the recorded to the server table to disallow multiple deployments to the
   * same node and drops startup keyspace once finished
   *
   * @param ip ip address of root node
   * @param cassandraPort cassandra port
   * @param username username to connect to root node
   * @param password password for root node
   */
  public static void writeServerRecordAndDropKeyspace(
      final String ip, final int cassandraPort, final String username, final String password) {

    System.out.println("Writing server record to root's table");

    Cluster cluster = createCluster(ip, cassandraPort);
    Session session = cluster.connect();

    Insert insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS)
            .value("pathstore_version", QueryBuilder.now())
            .value("pathstore_parent_timestamp", QueryBuilder.now())
            .value("pathstore_dirty", true)
            .value(Constants.SERVERS_COLUMNS.SERVER_UUID, UUID.randomUUID().toString())
            .value(Constants.SERVERS_COLUMNS.IP, ip)
            .value(Constants.SERVERS_COLUMNS.USERNAME, username)
            .value(Constants.SERVERS_COLUMNS.PASSWORD, password)
            .value(Constants.SERVERS_COLUMNS.NAME, "Root Node");

    session.execute(insert);

    // TODO: This may not be needed if the local keyspace is used more
    session.execute("drop keyspace if exists " + Constants.LOCAL_KEYSPACE);

    session.close();
    cluster.close();
  }

  /**
   * TODO: Add Constant values to {@link Constants}
   *
   * @param nodeID node id of node
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

    properties.put("NodeID", String.valueOf(nodeID));
    properties.put("ParentID", String.valueOf(parentNodeId));
    properties.put("Role", role.toString());
    properties.put("RMIRegistryIP", rmiRegistryIP);
    properties.put("RMIRegistryPort", String.valueOf(rmiRegistryPort));
    properties.put("RMIRegistryParentIP", rmiRegistryParentIP);
    properties.put("RMIRegistryParentPort", String.valueOf(rmiRegistryParentPort));
    properties.put("CassandraIP", cassandraIP);
    properties.put("CassandraPort", String.valueOf(cassandraPort));
    properties.put("CassandraParentIP", cassandraParentIP);
    properties.put("CassandraParentPort", String.valueOf(cassandraParentPort));

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
   * @param branch branch from github to build from
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
   * @param destinationToStore where to store the new node's properties file
   * @return list of deployment commands to execute
   */
  public static List<ICommand> initList(
      final SSHUtil sshUtil,
      final String ip,
      final String branch,
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
      final int cassandraParentPort,
      final String destinationToStore) {

    List<ICommand> commands = new ArrayList<>();

    commands.add(new Exec(sshUtil, "docker ps", 0));
    commands.add(new Exec(sshUtil, "docker kill cassandra", -1));
    commands.add(new Exec(sshUtil, "docker image rm cassandra", -1));
    commands.add(new Exec(sshUtil, "docker kill pathstore", -1));
    commands.add(new Exec(sshUtil, "docker image rm pathstore", -1));
    commands.add(new Exec(sshUtil, "rm -rf pathstore-install", -1));
    commands.add(new Exec(sshUtil, "mkdir -p pathstore-install", 0));
    commands.add(new Exec(sshUtil, "mkdir -p pathstore-install/cassandra", 0));
    commands.add(new Exec(sshUtil, "mkdir -p pathstore-install/pathstore", 0));
    commands.add(
        new GeneratePropertiesFile(
            nodeID,
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
            destinationToStore));
    commands.add(
        new FileTransfer(
            sshUtil, destinationToStore, "pathstore-install/pathstore/pathstore.properties"));
    commands.add(new RemoveGeneratedPropertiesFile(destinationToStore));
    commands.add(
        new FileTransfer(sshUtil, "../docker-files/deploy_key", "pathstore-install/deploy_key"));
    commands.add(
        new FileTransfer(
            sshUtil,
            "../docker-files/cassandra/Dockerfile",
            "pathstore-install/cassandra/Dockerfile"));
    commands.add(
        new FileTransfer(
            sshUtil,
            "../docker-files/pathstore/Dockerfile",
            "pathstore-install/pathstore/Dockerfile"));
    commands.add(
        new Exec(
            sshUtil,
            "docker build -t cassandra --build-arg key=\"$(cat pathstore-install/deploy_key)\" --build-arg branch=\""
                + branch
                + "\" pathstore-install/cassandra",
            0));
    commands.add(
        new Exec(sshUtil, "docker run --network=host -dit --rm --name cassandra cassandra", 0));
    commands.add(new WaitForCassandra(ip, cassandraPort));
    commands.add(
        new Exec(
            sshUtil,
            "docker build -t pathstore --build-arg key=\"$(cat pathstore-install/deploy_key)\" --build-arg branch=\""
                + branch
                + "\" pathstore-install/pathstore",
            0));
    commands.add(
        new Exec(sshUtil, "docker run --network=host -dit --rm --name pathstore pathstore", 0));
    commands.add(new WaitForPathStore(ip, cassandraPort));

    return commands;
  }
}
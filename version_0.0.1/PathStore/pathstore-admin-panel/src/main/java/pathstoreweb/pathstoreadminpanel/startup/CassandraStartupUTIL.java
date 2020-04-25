package pathstoreweb.pathstoreadminpanel.startup;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import pathstore.common.Constants;

import java.util.UUID;

/** Things related to cassandra for startup that can't rely on pathstore properties file */
public class CassandraStartupUTIL {

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
}

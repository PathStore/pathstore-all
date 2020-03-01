package pathstore.util;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.PathStoreProperties;

import java.util.LinkedList;
import java.util.List;

/**
 * Simple utility that allows you to reset an entire node. The purpose of this utility is to simply
 * to decrease time between tests as I've found a lot of test cases require complete node reset
 */
public class ResetNode {

  private static List<String> get_keyspaces(final Session session) {
    List<String> keyspaces = new LinkedList<>();

    for (Row row :
        session.execute(QueryBuilder.select("keyspace_name").from("system_schema", "keyspaces"))) {
      String keyspace_name = row.getString("keyspace_name");
      if (keyspace_name.startsWith("pathstore_")) keyspaces.add(keyspace_name);
    }

    return keyspaces;
  }

  private static void remove_keyspaces(final Session session, final List<String> keyspaces) {
    for (String s : keyspaces) {
      session.execute("drop keyspace if exists " + s);
    }
  }

  public static void main(String[] args) {
    Cluster cluster =
        new Cluster.Builder()
            .addContactPoints(PathStoreProperties.getInstance().CassandraIP)
            .withPort(PathStoreProperties.getInstance().CassandraPort)
            .withSocketOptions(
                new SocketOptions().setTcpNoDelay(true).setReadTimeoutMillis(15000000))
            .withQueryOptions(
                new QueryOptions()
                    .setRefreshNodeIntervalMillis(0)
                    .setRefreshNodeListIntervalMillis(0)
                    .setRefreshSchemaIntervalMillis(0))
            .build();
    Session session = cluster.connect();

    remove_keyspaces(session, get_keyspaces(session));

    session.close();
    cluster.close();
  }
}

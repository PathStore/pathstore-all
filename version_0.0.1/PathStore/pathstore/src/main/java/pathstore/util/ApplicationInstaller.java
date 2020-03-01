package pathstore.util;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pathstore.common.PathStoreProperties;
import pathstore.system.ApplicationEntry;
import pathstore.system.ProccessStatus;

/**
 * The point of this class is to calculate the path from one node to another and write to
 * pathstore_applications.node_schemas to either install an application along the path or delete an
 * application between a node and all its children.
 *
 * <p>Disclaimer: This should only ever be ran on the root node otherwise you most likely will get
 * unexpected behaviour
 */
public class ApplicationInstaller {

  // TODO: Modify hashmaps to list of integers as values
  private static void install_application(
      final Session session, final int nodeid, final String keyspace_name) {

    System.out.println("Installing for Node: " + nodeid + " with application " + keyspace_name);

    // TODO: Check if keyspace_name is a valid keyspace

    Map<Integer, Integer> node_to_parent_node = new HashMap<>();

    // Creates map from current nodeid to parent's node id
    for (Row row :
        session.execute(QueryBuilder.select().all().from("pathstore_applications", "topology")))
      node_to_parent_node.put(row.getInt("nodeid"), row.getInt("parent_nodeid"));

    System.out.println(node_to_parent_node);

    List<ApplicationEntry> applicationEntryList = new LinkedList<>();

    int current_nodeid = nodeid;

    while (current_nodeid != -1) {
      int parent_nodeid = node_to_parent_node.get(current_nodeid);
      ApplicationEntry new_entry =
          new ApplicationEntry(current_nodeid, ProccessStatus.WAITING_INSTALL, parent_nodeid);
      applicationEntryList.add(new_entry);
      System.out.println(new_entry);
      current_nodeid = parent_nodeid;
    }

    BatchStatement batchStatement = new BatchStatement();

    for (ApplicationEntry entry : applicationEntryList)
      batchStatement.add(
          QueryBuilder.insertInto("pathstore_applications", "node_schemas")
              .value("pathstore_version", QueryBuilder.now())
              .value("pathstore_parent_timestamp", QueryBuilder.now())
              .value("keyspace_name", keyspace_name)
              .value("nodeid", entry.node_id)
              .value("process_status", entry.proccess_status.toString())
              .value("wait_for", entry.waiting_for));

    session.execute(batchStatement);
  }

  private static void remove_application(
      final Session session, final int nodeid, final String keyspace_name) {
    System.out.println("Removing for Node: " + nodeid + " with application " + keyspace_name);

    // TODO: Check if keyspace_name is a valid keyspace

    Map<Integer, Integer> parent_to_node = new HashMap<>();

    // Creates map from current nodeid to parent's node id
    for (Row row :
        session.execute(QueryBuilder.select().all().from("pathstore_applications", "topology")))
      parent_to_node.put(row.getInt("parent_nodeid"), row.getInt("nodeid"));

    System.out.println(parent_to_node);

    List<ApplicationEntry> applicationEntryList = new LinkedList<>();

    Integer current_nodeid = nodeid;

    while (current_nodeid != null) {
      Integer child_nodeid = parent_to_node.get(current_nodeid);
      ApplicationEntry new_entry =
          new ApplicationEntry(
              current_nodeid,
              ProccessStatus.WAITING_REMOVE,
              child_nodeid == null ? -1 : child_nodeid);
      applicationEntryList.add(new_entry);
      System.out.println(new_entry);
      current_nodeid = child_nodeid;
    }

    BatchStatement batchStatement = new BatchStatement();

    for (ApplicationEntry entry : applicationEntryList)
      batchStatement.add(
          QueryBuilder.insertInto("pathstore_applications", "node_schemas")
              .value("pathstore_version", QueryBuilder.now())
              .value("pathstore_parent_timestamp", QueryBuilder.now())
              .value("keyspace_name", keyspace_name)
              .value("nodeid", entry.node_id)
              .value("process_status", entry.proccess_status.toString())
              .value("wait_for", entry.waiting_for));

    session.execute(batchStatement);
  }

  public static void main(String[] args) {
    switch (args[0]) {
      case "install":
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

        install_application(session, Integer.parseInt(args[1]), args[2]);

        session.close();
        cluster.close();
        break;
      case "remove":
        cluster =
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
        session = cluster.connect();

        remove_application(session, Integer.parseInt(args[1]), args[2]);

        session.close();
        cluster.close();
        break;
      default:
        System.err.println(args[0] + " is not an option");
        break;
    }
  }
}

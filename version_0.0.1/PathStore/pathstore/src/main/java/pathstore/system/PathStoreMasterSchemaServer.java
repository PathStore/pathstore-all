package pathstore.system;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The daemon is run on the ROOTSERVER. It will handle how the installation process of a singular
 * application.
 *
 * <p>Take the network of 1 <- 2 <-3 where 1 is the root server, 2 is the child of 1 and 3 is the
 * child of 2
 *
 * <p>Our topology table will look as follows
 *
 * <p>Nodeid Parentid
 *
 * <p>1 NULL
 *
 * <p>2 1
 *
 * <p>3 2
 *
 * <p>The process of installing an application called "Pathstore_demo" will look as follows in the
 * node_schemas table
 *
 * <p>nodeid keyspace_name status waiting_for
 *
 * <p>1 pathstore_demo init -1
 *
 * <p>2 pathstore_demo waiting 1
 *
 * <p>3 pathstore_demo waiting 2
 *
 * <p>Then once node 1 changes from init to running the master will change node 2 to init and the
 * slave will start the installation process
 */
public class PathStoreMasterSchemaServer extends Thread {

  private static class Node {
    final int node_id;
    final ProccessStatus proccess_status;
    final int waiting_for;

    Node(final int node_id, final ProccessStatus proccess_status, final int waiting_for) {
      this.node_id = node_id;
      this.proccess_status = proccess_status;
      this.waiting_for = waiting_for;
    }

    @Override
    public String toString() {
      return "Node{"
          + "node_id="
          + node_id
          + ", proccess_status="
          + proccess_status
          + ", waiting_for="
          + waiting_for
          + '}';
    }
  }

  @Override
  public void run() {
    while (true) {

      // First we will query the database to read from pathstore_applications.node_schemas

      Session privileged_session = PathStorePriviledgedCluster.getInstance().connect();

      Select select = QueryBuilder.select().all().from("pathstore_applications", "node_schemas");

      Map<String, List<Node>> data = new HashMap<>();

      for (Row row : privileged_session.execute(select)) {
        String keyspace = row.getString("keyspace_name");

        if (!data.containsKey(keyspace)) data.put(keyspace, new ArrayList<>());

        data.get(keyspace)
            .add(
                new Node(
                    row.getInt("nodeid"),
                    ProccessStatus.valueOf(row.getString("process_status")),
                    row.getInt("wait_for")));
      }

      // Can we make any assumptions about the node id? Currently my assumption is we can't
      for (String keyspace : data.keySet()) {
        System.out.println("Checking for schema: " + keyspace);
        List<Node> nodes = data.get(keyspace);
        System.out.println(nodes);

        Set<Node> running =
            nodes.stream()
                .filter(i -> i.proccess_status == ProccessStatus.RUNNING)
                .collect(Collectors.toSet());

        Set<Node> waiting =
            nodes.stream()
                .filter(i -> i.proccess_status == ProccessStatus.WAITING)
                .collect(Collectors.toSet());

        // Maybe break here?
        for (Node node : waiting) {
          System.out.println(node.node_id + " " + node.waiting_for);
          if (node.waiting_for == -1 || running.contains(node))
            this.initiate_application(node.node_id, keyspace);
        }
      }

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void initiate_application(final int nodeid, final String keyspace_name) {
    System.out.println(
        "Initiating application for: " + nodeid + " with application: " + keyspace_name);
    Session client_session = PathStoreCluster.getInstance().connect();

    Update update = QueryBuilder.update("pathstore_applications", "node_schemas");
    update
        .where(QueryBuilder.eq("nodeid", nodeid))
        .with(QueryBuilder.set("process_status", ProccessStatus.INIT.toString()));

    client_session.execute(update);
  }
}

package pathstore.system;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

  enum ProccessStatus {
    INIT,
    WAITING,
    RUNNING,
    STOPPED
  }

  private static class Node {
    final int node_id;
    final ProccessStatus proccess_status;
    final int waiting_for;

    Node(final int node_id, final ProccessStatus proccess_status, final int waiting_for) {
      this.node_id = node_id;
      this.proccess_status = proccess_status;
      this.waiting_for = waiting_for;
    }
  }

  @Override
  public void run() {
    while (true) {

      // First we will query the database to read from pathstore_applications.node_schemas

      Session client_session = PathStoreCluster.getInstance().connect();

      Select select = QueryBuilder.select().all().from("pathstore_applications", "node_schemas");

      HashMap<String, List<Node>> data = new HashMap<>();

      for (Row row : client_session.execute(select)) {
        String keyspace = row.getString("keyspace_name");
        if (!data.containsKey(keyspace)) data.put(keyspace, new ArrayList<>());
        else
          data.get(keyspace)
              .add(
                  new Node(
                      row.getInt("nodeid"),
                      ProccessStatus.valueOf(row.getString("process_status")),
                      row.getInt("waiting_for")));
      }

      for (String keyspace : data.keySet()) {
        List<Node> nodes = data.get(keyspace);
        List<Node> init =
            nodes.stream()
                .filter(i -> i.proccess_status != ProccessStatus.INIT)
                .collect(Collectors.toList());
        List<Node> waiting =
            nodes.stream()
                .filter(i -> i.proccess_status != ProccessStatus.WAITING)
                .collect(Collectors.toList());
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}

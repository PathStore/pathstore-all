package pathstore.system;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;
import pathstore.client.PathStoreResultSet;

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
  @Override
  public void run() {
    while (true) {

      // First we will query the database to read from pathstore_applications.node_schemas

      Session privileged_session = PathStorePriviledgedCluster.getInstance().connect();

      Select select = QueryBuilder.select().all().from("pathstore_applications", "node_schemas");

      Map<String, List<ApplicationEntry>> data = new HashMap<>();

      for (Row row :
          new PathStoreResultSet(
              privileged_session.execute(select), "pathstore_applications", "node_schemas")) {
        String keyspace = row.getString("keyspace_name");

        if (!data.containsKey(keyspace)) data.put(keyspace, new ArrayList<>());

        data.get(keyspace)
            .add(
                new ApplicationEntry(
                    row.getInt("nodeid"),
                    ProccessStatus.valueOf(row.getString("process_status")),
                    row.getInt("wait_for")));
      }

      // TODO: Clean this up. Lots of duplicate code
      // Can we make any assumptions about the node id? Currently my assumption is we can't
      for (String keyspace : data.keySet()) {
        System.out.println("Checking for schema: " + keyspace);
        List<ApplicationEntry> nodes = data.get(keyspace);
        System.out.println(nodes);

        Set<Integer> running =
            nodes.stream()
                .filter(i -> i.proccess_status == ProccessStatus.INSTALLED)
                .map(i -> i.node_id)
                .collect(Collectors.toSet());

        Set<ApplicationEntry> waiting =
            nodes.stream()
                .filter(i -> i.proccess_status == ProccessStatus.WAITING_INSTALL)
                .collect(Collectors.toSet());

        // Maybe break here?
        for (ApplicationEntry node : waiting) {
          if (node.waiting_for == -1 || running.contains(node.waiting_for))
            this.install_application(node.node_id, keyspace);
        }

        Set<Integer> removed =
            nodes.stream()
                .filter(i -> i.proccess_status == ProccessStatus.REMOVED)
                .map(i -> i.node_id)
                .collect(Collectors.toSet());

        Set<ApplicationEntry> waiting_remove =
            nodes.stream()
                .filter(i -> i.proccess_status == ProccessStatus.WAITING_REMOVE)
                .collect(Collectors.toSet());

        // Maybe break here?
        for (ApplicationEntry node : waiting_remove) {
          if (node.waiting_for == -1 || removed.contains(node.waiting_for))
            this.remove_application(node.node_id, keyspace);
        }
      }

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void install_application(final int nodeid, final String keyspace_name) {
    System.out.println(
        "Initiating application for: " + nodeid + " with application: " + keyspace_name);
    Session client_session = PathStoreCluster.getInstance().connect();

    Update update = QueryBuilder.update("pathstore_applications", "node_schemas");
    update
        .where(QueryBuilder.eq("nodeid", nodeid))
        .with(QueryBuilder.set("process_status", ProccessStatus.INSTALLING.toString()));

    client_session.execute(update);
  }

  private void remove_application(final int nodeid, final String keyspace_name) {
    System.out.println(
        "Removing application for: " + nodeid + " with application: " + keyspace_name);
    Session client_session = PathStoreCluster.getInstance().connect();

    Update update = QueryBuilder.update("pathstore_applications", "node_schemas");
    update
        .where(QueryBuilder.eq("nodeid", nodeid))
        .with(QueryBuilder.set("process_status", ProccessStatus.REMOVING.toString()));

    client_session.execute(update);
  }
}
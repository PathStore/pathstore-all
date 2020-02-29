package pathstore.system;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreCluster;
import pathstore.common.PathStoreProperties;

public class PathStoreSlaveSchemaServer extends Thread {

  @Override
  public void run() {
    while (true) {
      Session session = PathStoreCluster.getInstance().connect();
      Select select = QueryBuilder.select().all().from("pathstore_applications", "node_schemas");
      select.where(QueryBuilder.eq("nodeid", PathStoreProperties.getInstance().NodeID));

      for (Row row : session.execute(select)) {
        ProccessStatus current_process_status =
            ProccessStatus.valueOf(row.getString("process_status"));
        switch (current_process_status) {
          case INSTALLING:
            this.install_application(session, row.getString("keyspace_name"));
            break;
          case REMOVING:
            this.remove_application(session, row.getString("keyspace_name"));
            break;
        }
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void install_application(final Session session, final String keyspace) {
    // Query application, if not exist then just continue and wait for it to exist
    Select select = QueryBuilder.select().all().from("pathstore_applications", "apps");
    select.where(QueryBuilder.eq("keyspace_name", keyspace));

    for (Row row : session.execute(select)) {
      System.out.println("Loading application: " + keyspace);
      PathStoreSchemaLoader.parseSchema(row.getString("augmented_schema"))
          .forEach(PathStorePriviledgedCluster.getInstance().connect()::execute);

      Update update = QueryBuilder.update("pathstore_applications", "node_schemas");
      update.where(QueryBuilder.eq("nodeid", PathStoreProperties.getInstance().NodeID));
      update.with(QueryBuilder.set("process_status", ProccessStatus.INSTALLED.toString()));

      session.execute(update);
    }
  }

  private void remove_application(final Session session, final String keyspace) {
    System.out.println("Removing application " + keyspace);
    PathStorePriviledgedCluster.getInstance()
        .connect()
        .execute("drop keyspace if exists " + keyspace);

    Update update = QueryBuilder.update("pathstore_applications", "node_schemas");
    update.where(QueryBuilder.eq("nodeid", PathStoreProperties.getInstance().NodeID));
    update.with(QueryBuilder.set("process_status", ProccessStatus.REMOVED.toString()));

    session.execute(update);
  }
}

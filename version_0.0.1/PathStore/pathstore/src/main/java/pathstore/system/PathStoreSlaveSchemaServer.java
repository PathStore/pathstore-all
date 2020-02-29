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
    Session session = PathStoreCluster.getInstance().connect();
    Select select = QueryBuilder.select().all().from("pathstore_applications", "node_schemas");
    select.where(QueryBuilder.eq("nodeid", PathStoreProperties.getInstance().NodeID));

    for (Row row : session.execute(select)) {
      if (row.getString("process_status").equals(ProccessStatus.INIT.toString()))
        this.instantiate_application(session, row.getString("keyspace_name"));
    }

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void instantiate_application(final Session session, final String keyspace) {
    // Query application, if not exist then just continue and wait for it to exist
    Select select = QueryBuilder.select().all().from("pathstore_applications", "apps");
    select.where(QueryBuilder.eq("keyspace_name", keyspace));

    for (Row row : session.execute(select)) {
      System.out.println("Loading application: " + keyspace);
      PathStoreSchemaLoader.parseSchema(row.getString("augmented_schema"))
          .forEach(PathStorePriviledgedCluster.getInstance().connect()::execute);

      Update update = QueryBuilder.update("pathstore_applications", "node_schemas");
      update.where(QueryBuilder.eq("nodeid", PathStoreProperties.getInstance().NodeID));
      update.with(QueryBuilder.set("process_status", ProccessStatus.RUNNING));

      session.execute(update);
    }
  }
}

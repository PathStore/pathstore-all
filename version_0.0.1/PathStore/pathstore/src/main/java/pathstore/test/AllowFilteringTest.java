package pathstore.test;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;

public class AllowFilteringTest {
  public static void main(String[] args) {

    Session psSession = PathStoreCluster.getInstance().connect();

    Select select = QueryBuilder.select().all().from("pathstore_applications", "node_schemas");
    select
        .where(QueryBuilder.eq("node_id", 1))
        .and(QueryBuilder.eq("keyspace_name", "pathstore_demo"))
        .and(QueryBuilder.eq("process_status", "INSTALLING"));
    select.allowFiltering();

    for (Row row : psSession.execute(select)) {
      System.err.println("Test failed");
    }

    System.out.println("Test complete");
    psSession.close();
  }
}

package pathstore.test;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;

/**
 * Steps to test allow filtering logic.
 *
 * <p>1) Deploy a new node as a child to the root node
 *
 * <p>2) Register an application and deploy it on both nodes.
 *
 * <p>3) After deployment has finished run this test with args INSTALLING on both the root node and
 * the child node. This should print 0 results. Then run it on both nodes with INSTALLED. This
 * should print 2 records. If both that occurs, the test has passed.
 */
public class AllowFilteringTest {
  public static void main(String[] args) {

    Session psSession = PathStoreCluster.getDaemonInstance().connect();

    Select select = QueryBuilder.select().all().from("pathstore_applications", "node_schemas");
    select.where(QueryBuilder.eq("process_status", args[0]));
    select.allowFiltering();

    for (Row row : psSession.execute(select)) {
      System.out.println(
          String.format(
              "%d %s %s",
              row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID),
              row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME),
              row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS)));
    }

    System.out.println("Test complete");
    psSession.close();
  }
}

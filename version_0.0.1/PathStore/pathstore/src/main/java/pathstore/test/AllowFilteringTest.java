package pathstore.test;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.common.Constants;
import pathstore.system.PathStorePrivilegedCluster;

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

    Session psSession = PathStorePrivilegedCluster.getSuperUserInstance().psConnect();

    Select select =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    select.where(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS, args[0]));
    select.allowFiltering();

    System.out.println(String.format("Executing query %s", select.toString()));

    for (Row row : psSession.execute(select)) {
      System.out.println(
          String.format(
              "Parent Node Id: %d Node id: %d Server UUID: %s Process Status: %s Who to wait for: %s",
              row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID),
              row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID),
              row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID),
              row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS),
              row.getList(Constants.DEPLOYMENT_COLUMNS.WAIT_FOR, Integer.class)));
    }

    PathStorePrivilegedCluster.getSuperUserInstance().close();
    System.out.println("Test complete");
  }
}

package pathstore.test;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;

/**
 * This class is used to test the functionality of client authentication.
 *
 * <p>It is assumed that the cassandra ip/port is defined in the properties file. It is also assumed
 * that the grpc ip/port is defined in the properties file.
 *
 * @implNote You must pass the application name and master password as params, respectively
 */
public class ClientAuthenticationTest {

  public static void main(String[] args) {

    try {
      PathStoreClientAuthenticatedCluster cluster =
          PathStoreClientAuthenticatedCluster.getInstance();

      Session session = cluster.connect();

      try {
        Select select =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

        session.execute(select);

      } catch (Exception e) {
        System.out.println("Proper permissions are assigned to the role");
      }

      cluster.close();

      System.out.println("Test complete, close completed successfully");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

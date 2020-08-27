package pathstore.test;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.client.PathStoreSession;
import pathstore.sessions.PathStoreSessionManager;

public class SessionMigrationTest {
  public static void main(String[] args) throws Exception {
    PathStoreClientAuthenticatedCluster.initInstance("pathstore_demo", "pathstore_demo");

    PathStoreSessionManager.init("/home/myles/Documents/sessionFile.txt");

    PathStoreSession session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Insert insert =
        QueryBuilder.insertInto("pathstore_demo", "users")
            .value("name", "myles")
            .value("sport", "golf");

    session.execute(insert, PathStoreSessionManager.getInstance().getKeyspaceToken("demo-session"));

    Select select = QueryBuilder.select().all().from("pathstore_demo", "users");

    for (Row row :
        session.execute(
            select, PathStoreSessionManager.getInstance().getKeyspaceToken("demo-session"))) {
      System.out.println(String.format("%s %s", row.getString("name"), row.getString("sport")));
    }

    PathStoreClientAuthenticatedCluster.getInstance().close();

    PathStoreSessionManager.getInstance().close();
  }
}

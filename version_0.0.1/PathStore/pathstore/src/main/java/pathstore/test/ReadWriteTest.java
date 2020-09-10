package pathstore.test;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.client.PathStoreSession;
import pathstore.sessions.PathStoreSessionManager;

public class ReadWriteTest {
  public static void main(String[] args) {
    PathStoreSession session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    if (args[0] == null) {
      Insert insert =
          QueryBuilder.insertInto("pathstore_demo", "users")
              .value("name", "myles")
              .value("sport", "golf");

      session.execute(
          insert, PathStoreSessionManager.getInstance().getKeyspaceToken("demo-session"));

      Update update = QueryBuilder.update("pathstore_demo", "users");

      update.where(QueryBuilder.eq("name", "myles"));
      update.with(QueryBuilder.set("years", 20));
    }

    Select select = QueryBuilder.select().all().from("pathstore_demo", "users");

    for (Row row : session.execute(select))
      System.out.println(
          String.format(
              "%s %s %d", row.getString("name"), row.getString("sport"), row.getInt("years")));

    PathStoreClientAuthenticatedCluster.getInstance().close();
  }
}
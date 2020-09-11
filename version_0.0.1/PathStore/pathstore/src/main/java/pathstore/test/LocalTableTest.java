package pathstore.test;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.client.PathStoreSession;

public class LocalTableTest {
  public static void main(String[] args) {
    PathStoreSession session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Insert insert =
        QueryBuilder.insertInto("pathstore_demo", "local_table")
            .value("name", "myles")
            .value("number", 5);

    session.execute(insert);

    read(session);

    Update update = QueryBuilder.update("pathstore_demo", "local_table");
    update.where(QueryBuilder.eq("name", "myles"));
    update.with(QueryBuilder.set("number", 6));

    session.execute(update);

    read(session);

    PathStoreClientAuthenticatedCluster.getInstance().close();
  }

  private static void read(final PathStoreSession session) {
    Select select = QueryBuilder.select().all().from("pathstore_demo", "local_table");

    for (Row row : session.execute(select)) {
      System.out.println(
          String.format("Name: %s, number: %d", row.getString("name"), row.getInt("number")));
    }
  }
}

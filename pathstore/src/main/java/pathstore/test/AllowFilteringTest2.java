package pathstore.test;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.client.PathStoreSession;

public class AllowFilteringTest2 {
  public static void main(String[] args) throws InterruptedException {
    PathStoreSession session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    try {
      Insert insert = QueryBuilder.insertInto("pathstore_demo", "users").value("name", "myles");

      session.execute(insert);

      Update sport = QueryBuilder.update("pathstore_demo", "users");
      sport.where(QueryBuilder.eq("name", "myles"));
      sport.with(QueryBuilder.set("sport", "golf"));

      session.execute(sport);

      Update age = QueryBuilder.update("pathstore_demo", "users");
      age.where(QueryBuilder.eq("name", "myles"));
      age.with(QueryBuilder.set("years", 20));

      session.execute(age);

      Update vegetarian = QueryBuilder.update("pathstore_demo", "users");
      vegetarian.where(QueryBuilder.eq("name", "myles"));
      vegetarian.with(QueryBuilder.set("vegetarian", true));

      session.execute(vegetarian);

      Select queryAll = QueryBuilder.select().all().from("pathstore_demo", "users");

      System.out.println(String.format("\nFiltering by: %s\n", "nothing"));

      for (Row row : session.execute(queryAll)) System.out.println(rowParse(row));

      Select querySports = QueryBuilder.select().all().from("pathstore_demo", "users");
      querySports.where(QueryBuilder.eq("sport", "golf"));
      querySports.allowFiltering();

      System.out.println(String.format("\nFiltering by: %s\n", "Sport=golf"));

      for (Row row : session.execute(querySports)) System.out.println(rowParse(row));

      Select queryAge = QueryBuilder.select().all().from("pathstore_demo", "users");
      queryAge.where(QueryBuilder.eq("years", 20));
      queryAge.allowFiltering();

      System.out.println(String.format("\nFiltering by: %s\n", "years=20"));

      for (Row row : session.execute(queryAge)) System.out.println(rowParse(row));

      Select queryVegetarian = QueryBuilder.select().all().from("pathstore_demo", "users");
      queryVegetarian.where(QueryBuilder.eq("vegetarian", true));
      queryVegetarian.allowFiltering();

      System.out.println(String.format("\nFiltering by: %s\n", "vegetarian=true"));

      for (Row row : session.execute(queryVegetarian)) System.out.println(rowParse(row));

    } finally {
      PathStoreClientAuthenticatedCluster.getInstance().close();
    }
  }

  private static String rowParse(final Row row) {
    return String.format(
        "%s %s %d %b",
        row.getString("name"),
        row.getString("sport"),
        row.getInt("years"),
        row.getBool("vegetarian"));
  }
}

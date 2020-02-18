package pathstore.test;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.PathStoreProperties;
import pathstore.common.Role;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class CLIDemo {

  private static class Event extends Thread {
    private volatile boolean run = true;

    @Override
    public void run() {
      PathStoreCluster pathStoreCluster = PathStoreCluster.getInstance();
      Session session = pathStoreCluster.connect();
      Scanner sc = new Scanner(System.in);
      while (this.run) {
        System.out.println("Please inset an option from [I, D, U, Exit]");
        switch (sc.next()) {
          case "I":
            getInsertInfo(sc, session);
            break;
          case "D":
            display(session);
            break;  
          case "U":
            break;
          case "Exit":
            this.exit();
            break;
          default:
            System.out.println("That option does not exist please use [I, D, U, Exit]");
            break;
        }
      }
      pathStoreCluster.close();
    }

    private void getInsertInfo(final Scanner sc, final Session session) {
      String username, sport;
      int age;
      boolean vegetarian;
      System.out.println("User's name: ");
      username = sc.next();
      System.out.println("Sport: ");
      sport = sc.next();
      System.out.println("age: ");
      age = sc.nextInt();
      System.out.println("vegetarian: ");
      vegetarian = sc.nextBoolean();
      insertUser(session, username, sport, Arrays.asList(0, 0, 0), age, vegetarian);
    }

    private void insertUser(
        final Session session,
        final String name,
        final String sport,
        final List<Integer> rgb,
        final int years,
        final boolean vegetarian) {
      Insert insert = QueryBuilder.insertInto("pathstore_demo", "users");
      insert.value("name", name);
      insert.value("color", rgb);
      insert.value("sport", sport);
      insert.value("years", years);
      insert.value("vegetarian", vegetarian);
      session.execute(insert);
    }

    private void display(final Session session) {
      Select select = QueryBuilder.select().all().from("pathstore_demo", "users");
      for (Row row : session.execute(select)) {
        System.out.format(
            "%s %s %s %d %d",
            row.getString("name"),
            row.getString("sport"),
            ((List<Integer>) row.getObject("rgb")).toString(),
            row.getInt("years"),
            row.getBool("vegetarian") ? 1 : 0);
      }
    }

    private void exit() {
      this.run = false;
    }
  }

  public static void main(final String[] args) throws InterruptedException {
    PathStoreProperties.getInstance().role = Role.CLIENT;
    Event event = new Event();
    event.start();
    event.join();
  }
}

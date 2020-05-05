package pathstore.system.deployment.commands;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.common.Constants;
import pathstore.system.deployment.utilities.StartupUTIL;

import java.util.HashMap;
import java.util.Map;

/** This class is used to wait for pathstore to start up */
public class WaitForPathStore implements ICommand {

  /** Tasks that need to be completed before we can consider pathstore to be officially online */
  private final Map<Integer, String> neededRecords;

  /** Cluster created on initialization */
  private final Cluster cluster;

  /** Connects once this command is called, not local to avoid multiple connections */
  private Session session = null;

  /**
   * Creates cluster
   *
   * @param ip ip of new root
   * @param port cassandra port
   */
  public WaitForPathStore(final String ip, final int port) {
    this.cluster = StartupUTIL.createCluster(ip, port);
    this.neededRecords = new HashMap<>();
    this.neededRecords.put(0, "RMI Server started");
    this.neededRecords.put(1, "Pathstore Application keyspace loaded");
    this.neededRecords.put(2, "Daemons started");
  }

  /**
   * Every second check to see if rows have been added and remove them from neededRecords if not
   * already done so and print out what their task was to stdout so user knows whats happening
   *
   * @throws CommandError contains a message to denote what went wrong
   */
  @Override
  public void execute() throws CommandError {

    if (this.session == null) this.session = this.cluster.connect();

    Select tasks = QueryBuilder.select().all().from(Constants.LOCAL_KEYSPACE, Constants.STARTUP);

    for (Row row : session.execute(tasks)) {
      int task = row.getInt(Constants.STARTUP_COLUMNS.TASK_DONE);

      if (neededRecords.containsKey(task)) {
        System.out.println(neededRecords.get(task));
        neededRecords.remove(task);
      }
    }

    if (neededRecords.size() > 0) {
      try {
        Thread.sleep(1000);
        this.execute();
      } catch (InterruptedException e) {
        throw new CommandError("Sleep was interrupted while waiting for pathstore to come online");
      }
    } else {
      this.session.close();
      this.cluster.close();
    }
  }

  /**
   * Inform user that pathstore is currently waiting to come online
   *
   * @return msg
   */
  @Override
  public String toString() {
    return "Waiting for PathStore to startup";
  }
}

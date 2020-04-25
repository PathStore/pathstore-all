package pathstoreweb.pathstoreadminpanel.startup.commands;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.startup.CassandraStartupUTIL;
import pathstoreweb.pathstoreadminpanel.startup.SSHUtil;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.InternalException;

import java.util.HashMap;
import java.util.Map;

/** This class is used to wait for pathstore to start up */
public class WaitForPathStore implements ICommand {

  /** Tasks that need to be completed before we can consider pathstore to be officially online */
  private static final Map<Integer, String> neededRecords = new HashMap<>();

  static {
    neededRecords.put(0, "RMI Server started");
    neededRecords.put(1, "Pathstore Application keyspace loaded");
    neededRecords.put(2, "Topology Table written to");
    neededRecords.put(3, "Daemons started");
  }

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
    this.cluster = CassandraStartupUTIL.createCluster(ip, port);
  }

  /**
   * Every second check to see if rows have been added and remove them from neededRecords if not
   * already done so and print out what their task was to stdout so user knows whats happening
   *
   * @param sshUtil ssh utility to access the remote host
   */
  @Override
  public void execute(final SSHUtil sshUtil) throws InternalException {

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
        this.execute(sshUtil);
      } catch (InterruptedException e) {
        throw new InternalException();
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

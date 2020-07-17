package pathstorestartup.commands;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.Constants;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.deployment.commands.ICommand;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstore.system.deployment.utilities.StartupUTIL;

import java.util.Collections;
import java.util.LinkedList;
import java.util.UUID;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;
import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.WAIT_FOR;
import static pathstore.common.Constants.PATHSTORE_COLUMNS.*;
import static pathstore.common.Constants.PATHSTORE_COLUMNS.PATHSTORE_DIRTY;
import static pathstore.common.Constants.SERVERS_COLUMNS.*;
import static pathstore.common.Constants.SERVERS_COLUMNS.SERVER_UUID;

/**
 * This command will write the server record for the root node and the deployment record for the
 * root node
 */
public class FinalizeRootInstallation implements ICommand {

  /** Ip of root node */
  private final String ip;

  /** Cassandra port for root node */
  private final int cassandraPort;

  /** Username to server */
  private final String username;

  /** Password to server */
  private final String password;

  /** Ssh port to server */
  private final int sshPort;

  /** Rmi port to server */
  private final int rmiPort;

  /**
   * @param ip {@link #ip}
   * @param cassandraPort {@link #cassandraPort}
   * @param username {@link #username}
   * @param password {@link #password}
   * @param sshPort {@link #sshPort}
   * @param rmiPort {@link #rmiPort}
   */
  public FinalizeRootInstallation(
      final String ip,
      final int cassandraPort,
      final String username,
      final String password,
      final int sshPort,
      final int rmiPort) {
    this.ip = ip;
    this.cassandraPort = cassandraPort;
    this.username = username;
    this.password = password;
    this.sshPort = sshPort;
    this.rmiPort = rmiPort;
  }

  /**
   * This command will write the root node server record to the table and write the root node
   * deployment record
   */
  @Override
  public void execute() {

    System.out.println("Writing server record to root's table");

    PathStorePrivilegedCluster cluster =
        PathStorePrivilegedCluster.getChildInstance(
            "cassandra", "cassandra", this.ip, this.cassandraPort);
    Session session = cluster.connect();

    UUID serverUUID = UUID.randomUUID();

    Insert insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS)
            .value(PATHSTORE_VERSION, QueryBuilder.now())
            .value(PATHSTORE_PARENT_TIMESTAMP, QueryBuilder.now())
            .value(PATHSTORE_DIRTY, true)
            .value(SERVER_UUID, serverUUID.toString())
            .value(IP, ip)
            .value(USERNAME, username)
            .value(PASSWORD, password)
            .value(SSH_PORT, sshPort)
            .value(RMI_PORT, rmiPort)
            .value(NAME, "Root Node");

    session.execute(insert);

    insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT)
            .value(PATHSTORE_VERSION, QueryBuilder.now())
            .value(PATHSTORE_PARENT_TIMESTAMP, QueryBuilder.now())
            .value(PATHSTORE_DIRTY, true)
            .value(NEW_NODE_ID, 1)
            .value(PARENT_NODE_ID, -1)
            .value(PROCESS_STATUS, DeploymentProcessStatus.DEPLOYED.toString())
            .value(WAIT_FOR, new LinkedList<>(Collections.singleton(-1)))
            .value(SERVER_UUID, serverUUID.toString());

    session.execute(insert);

    cluster.close();
  }
}

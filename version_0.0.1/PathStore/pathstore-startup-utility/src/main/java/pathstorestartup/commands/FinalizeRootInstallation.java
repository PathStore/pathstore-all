package pathstorestartup.commands;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstore.common.tables.ServerAuthType;
import pathstore.common.tables.ServerIdentity;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.deployment.commands.ICommand;
import pathstorestartup.constants.BootstrapDeploymentConstants;

import java.util.Collections;
import java.util.LinkedList;
import java.util.UUID;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;
import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.WAIT_FOR;
import static pathstore.common.Constants.PATHSTORE_META_COLUMNS.*;
import static pathstore.common.Constants.SERVERS_COLUMNS.*;
import static pathstore.common.Constants.SERVERS_COLUMNS.SERVER_UUID;

/**
 * This command will write the server record for the root node and the deployment record for the
 * root node
 */
public class FinalizeRootInstallation implements ICommand {

  /** Cassandra account username */
  private final String cassandraUsername;

  /** Cassandra account password */
  private final String cassandraPassword;

  /** Ip of root node */
  private final String ip;

  /** Cassandra port for root node */
  private final int cassandraPort;

  /** Username to server */
  private final String username;

  /** Method of authentication used to install the root node */
  private final String authType;

  /** Null if password auth was used, else */
  private final ServerIdentity serverIdentity;

  /** Password to server */
  private final String password;

  /** Ssh port to server */
  private final int sshPort;

  /** Grpc port to server */
  private final int grpcPort;

  /**
   * @param cassandraUsername {@link #cassandraUsername}
   * @param cassandraPassword {@link #cassandraPassword}
   * @param ip {@link #ip}
   * @param cassandraPort {@link #cassandraPort}
   * @param username {@link #username}
   * @param password {@link #password}
   * @param sshPort {@link #sshPort}
   * @param grpcPort {@link #grpcPort}
   */
  public FinalizeRootInstallation(
      final String cassandraUsername,
      final String cassandraPassword,
      final String ip,
      final int cassandraPort,
      final String username,
      final String authType,
      final ServerIdentity serverIdentity,
      final String password,
      final int sshPort,
      final int grpcPort) {
    this.cassandraUsername = cassandraUsername;
    this.cassandraPassword = cassandraPassword;
    this.ip = ip;
    this.cassandraPort = cassandraPort;
    this.username = username;
    this.authType = authType;
    this.serverIdentity = serverIdentity;
    this.password = password;
    this.sshPort = sshPort;
    this.grpcPort = grpcPort;
  }

  /**
   * This command will write the root node server record to the table and write the root node
   * deployment record
   */
  @Override
  public void execute() {

    PathStorePrivilegedCluster cluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.cassandraUsername, this.cassandraPassword, this.ip, this.cassandraPort);
    Session session = cluster.connect();

    UUID serverUUID = UUID.randomUUID();

    Insert insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS)
            .value(PATHSTORE_VERSION, QueryBuilder.now())
            .value(PATHSTORE_PARENT_TIMESTAMP, QueryBuilder.now())
            .value(PATHSTORE_DIRTY, true)
            .value(SERVER_UUID, serverUUID.toString())
            .value(IP, this.ip)
            .value(USERNAME, this.username)
            .value(SSH_PORT, this.sshPort)
            .value(GRPC_PORT, this.grpcPort)
            .value(NAME, "Root Node");

    if (this.authType.equals(BootstrapDeploymentConstants.AUTH_TYPES.PASSWORD))
      insert.value(AUTH_TYPE, ServerAuthType.PASSWORD.toString()).value(PASSWORD, this.password);
    else
      insert
          .value(AUTH_TYPE, ServerAuthType.IDENTITY.toString())
          .value(SERVER_IDENTITY, this.serverIdentity.serialize());

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

  /** @return info message */
  @Override
  public String toString() {
    return "Writing server and deployment record to roots table";
  }
}

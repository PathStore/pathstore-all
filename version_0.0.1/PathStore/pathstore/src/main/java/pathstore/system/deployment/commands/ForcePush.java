package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;
import pathstore.system.PathStorePriviledgedCluster;
import pathstore.system.PathStorePushServer;
import pathstore.system.deployment.utilities.StartupUTIL;
import pathstore.util.SchemaInfo;

/** This command is used to force push all dirty data from a client on shutdown */
public class ForcePush implements ICommand {

  /** Logger for this class */
  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(ForcePush.class);

  /** Ip of child */
  private final String ip;

  /** Cassandra port to connect with */
  private final int cassandraPort;

  /** Child node id for the push server */
  private final int newNodeId;

  /**
   * @param ip {@link #ip}
   * @param cassandraPort {@link #cassandraPort}
   * @param newNodeId {@link #newNodeId}
   */
  public ForcePush(final String ip, final int cassandraPort, final int newNodeId) {
    this.ip = ip;
    this.cassandraPort = cassandraPort;
    this.newNodeId = newNodeId;
  }

  /**
   * Connect to the child, and create a connection to the local database. Then call {@link
   * PathStorePushServer#push(Session, Session, SchemaInfo, int)} to force push all data
   */
  @Override
  public void execute() {
    Session child = StartupUTIL.createCluster(this.ip, this.cassandraPort, "cassandra", "cassandra").connect();

    this.logger.info("Successfully connected to child cassandra");

    PathStorePushServer.push(
        child,
        PathStorePriviledgedCluster.getInstance().connect(),
        new SchemaInfo(child),
        this.newNodeId);

    child.close();

    this.logger.info("Finished pushing all dirty data");
  }

  /** @return inform the user what command is happening */
  @Override
  public String toString() {
    return String.format("Starting the push of all dirty data from child with ip %s", this.ip);
  }
}

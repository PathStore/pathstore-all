package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.PathStorePushServer;
import pathstore.util.SchemaInfo;

/** This command is used to force push all dirty data from a client on shutdown */
public class ForcePush implements ICommand {

  /** Logger for this class */
  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(ForcePush.class);

  /** Child cluster */
  private final PathStorePrivilegedCluster cluster;

  /** Node id of child */
  private final int nodeId;

  /**
   * @param nodeId node of child
   * @param ip ip of child
   * @param cassandraPort port of child
   */
  public ForcePush(final int nodeId, final String ip, final int cassandraPort) {
    this.cluster = PathStorePrivilegedCluster.getChildInstance(nodeId, ip, cassandraPort);
    this.nodeId = nodeId;
  }

  /**
   * Connect to the child, and create a connection to the local database. Then call {@link
   * PathStorePushServer#push(Session, Session, SchemaInfo, int)} to force push all data
   */
  @Override
  public void execute() {
    Session child = this.cluster.connect();

    PathStorePushServer.push(
        child,
        PathStorePrivilegedCluster.getDaemonInstance().connect(),
        new SchemaInfo(child),
        this.nodeId);

    this.cluster.close();

    this.logger.info("Finished pushing all dirty data");
  }

  /** @return inform the user what command is happening */
  @Override
  public String toString() {
    return String.format("Starting the push of all dirty data from child node %d", this.nodeId);
  }
}

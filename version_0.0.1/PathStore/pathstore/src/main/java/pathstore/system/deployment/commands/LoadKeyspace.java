package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.function.Consumer;

/** This command is used to load a keyspace onto the child node */
public class LoadKeyspace implements ICommand {
  /**
   * User to connect to cassandra with. This account needs access to create keyspaces, tables,
   * UDT's, and secondary indexes
   */
  private final String connectionUsername;

  /** Password to connect to cassandra with */
  private final String connectionPassword;

  /** Ip of cassandra instance */
  private final String ip;

  /** Cassandra native transport port */
  private final int port;

  /**
   * Function that will take a session object as a param and load a keyspace to it
   *
   * @see pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils#loadApplicationSchema(Session)
   * @see pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils#loadLocalKeyspace(Session)
   */
  private final Consumer<Session> loadKeyspaceFunction;

  /**
   * Name of the keyspace, this is solely used to inform the user of what keyspace is being loaded
   */
  private final String keyspaceName;

  /**
   * @param connectionUsername {@link #connectionUsername}
   * @param connectionPassword {@link #connectionPassword}
   * @param ip {@link #ip}
   * @param port {@link #port}
   * @param loadKeyspaceFunction {@link #loadKeyspaceFunction}
   * @param keyspaceName {@link #keyspaceName}
   */
  public LoadKeyspace(
      final String connectionUsername,
      final String connectionPassword,
      final String ip,
      final int port,
      final Consumer<Session> loadKeyspaceFunction,
      final String keyspaceName) {
    this.connectionUsername = connectionUsername;
    this.connectionPassword = connectionPassword;
    this.ip = ip;
    this.port = port;
    this.loadKeyspaceFunction = loadKeyspaceFunction;
    this.keyspaceName = keyspaceName;
  }

  /** Connect to child cassandra, call the load function and close the cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.connectionUsername, this.connectionPassword, this.ip, this.port);

    this.loadKeyspaceFunction.accept(childCluster.connect());

    childCluster.close();
  }

  /** @return inform the user which keyspace is being loaded */
  @Override
  public String toString() {
    return "Loading keyspace " + this.keyspaceName;
  }
}

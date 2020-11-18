package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.function.Consumer;

/** This command is used to load a keyspace onto the child node */
public class LoadKeyspace implements ICommand {
  /** Cassandra credentials to connect with */
  private final DeploymentCredential cassandraCredentials;

  /**
   * Function that will take a session object as a param and load a keyspace to it
   *
   * @see pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils#loadApplicationSchema(Session)
   */
  private final Consumer<Session> loadKeyspaceFunction;

  /**
   * Name of the keyspace, this is solely used to inform the user of what keyspace is being loaded
   */
  private final String keyspaceName;

  /**
   * @param cassandraCredentials {@link #cassandraCredentials}
   * @param loadKeyspaceFunction {@link #loadKeyspaceFunction}
   * @param keyspaceName {@link #keyspaceName}
   */
  public LoadKeyspace(
      final DeploymentCredential cassandraCredentials,
      final Consumer<Session> loadKeyspaceFunction,
      final String keyspaceName) {
    this.cassandraCredentials = cassandraCredentials;
    this.loadKeyspaceFunction = loadKeyspaceFunction;
    this.keyspaceName = keyspaceName;
  }

  /** Connect to child cassandra, call the load function and close the cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredentials);

    this.loadKeyspaceFunction.accept(childCluster.rawConnect());

    childCluster.close();
  }

  /** @return inform the user which keyspace is being loaded */
  @Override
  public String toString() {
    return "Loading keyspace " + this.keyspaceName;
  }
}

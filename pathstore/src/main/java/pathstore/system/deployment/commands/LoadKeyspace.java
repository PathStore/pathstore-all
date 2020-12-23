package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import lombok.RequiredArgsConstructor;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.function.Consumer;

/** This command is used to load a keyspace onto the child node */
@RequiredArgsConstructor
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

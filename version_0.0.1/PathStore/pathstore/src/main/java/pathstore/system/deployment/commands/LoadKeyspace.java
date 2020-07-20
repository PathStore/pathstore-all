package pathstore.system.deployment.commands;

import com.datastax.driver.core.Session;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.function.Consumer;

public class LoadKeyspace implements ICommand {
  private final String username;

  private final String password;

  private final String ip;

  private final int port;

  private final Consumer<Session> loadKeyspaceFunction;

  private final String keyspaceName;

  public LoadKeyspace(
      final String username,
      final String password,
      final String ip,
      final int port,
      final Consumer<Session> loadKeyspaceFunction,
      final String keyspaceName) {
    this.username = username;
    this.password = password;
    this.ip = ip;
    this.port = port;
    this.loadKeyspaceFunction = loadKeyspaceFunction;
    this.keyspaceName = keyspaceName;
  }

  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(
            this.username, this.password, this.ip, this.port);

    this.loadKeyspaceFunction.accept(childCluster.connect());

    childCluster.close();
  }

  @Override
  public String toString() {
    return "Loading keyspace " + this.keyspaceName;
  }
}

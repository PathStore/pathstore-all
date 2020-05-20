package pathstoreweb.pathstoreadminpanel.services.servers.payload;

import pathstoreweb.pathstoreadminpanel.services.servers.Server;
import pathstoreweb.pathstoreadminpanel.services.servers.validator.NameUnique;
import pathstoreweb.pathstoreadminpanel.services.servers.validator.ServerConnectionTest;
import pathstoreweb.pathstoreadminpanel.services.servers.validator.ServerUnique;

import java.util.UUID;

/**
 * This payload is used when a user makes a request to add a server to the {@link
 * pathstore.common.Constants#SERVERS} table
 */
public final class AddServerPayload {

  /** Ip is stored separably to check to see if that ip already exists in the table */
  @ServerUnique(message = "Server with that ip already exists")
  public final String ip;

  /** Human identifiable name of server */
  @NameUnique(message = "Name used to identify server must be unique")
  public final String name;

  /**
   * TODO: Check if docker is installed and accessible with the credentials given
   *
   * <p>Store an instance of the server class, check to see if the information allows you to connect
   * to a server
   */
  @ServerConnectionTest(
      message =
          "Could not connect to server, make sure that you have properly inputted the connection information")
  public final Server server;

  public AddServerPayload(
      final String ip,
      final String username,
      final String password,
      final int sshPort,
      final int rmiPort,
      final String name) {
    this.ip = ip;
    this.name = name;
    this.server =
        new Server(UUID.randomUUID(), ip, username, password, sshPort, rmiPort, name);
  }
}

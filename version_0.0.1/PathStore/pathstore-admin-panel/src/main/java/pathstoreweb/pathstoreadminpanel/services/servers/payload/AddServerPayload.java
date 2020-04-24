package pathstoreweb.pathstoreadminpanel.services.servers.payload;

import pathstoreweb.pathstoreadminpanel.services.servers.Server;
import pathstoreweb.pathstoreadminpanel.services.servers.validator.ServerConnectionTest;
import pathstoreweb.pathstoreadminpanel.services.servers.validator.ServerUnique;

import java.util.UUID;

public final class AddServerPayload {

  @ServerUnique(message = "Server with that ip already exists")
  public final String ip;

  // TODO: check if server has docker
  @ServerConnectionTest(
      message =
          "Could not connect to server, make sure that you have properly inputted the connection information")
  public final Server server;

  public AddServerPayload(final String ip, final String username, final String password) {
    this.ip = ip;
    this.server = new Server(UUID.randomUUID(), ip, username, password);
  }
}

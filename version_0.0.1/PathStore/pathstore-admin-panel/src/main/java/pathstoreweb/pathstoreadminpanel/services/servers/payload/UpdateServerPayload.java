package pathstoreweb.pathstoreadminpanel.services.servers.payload;

import pathstoreweb.pathstoreadminpanel.services.servers.Server;
import pathstoreweb.pathstoreadminpanel.services.servers.validator.*;

import java.util.UUID;

/**
 * This payload is used when a user makes a request to add a server to the {@link
 * pathstore.common.Constants#SERVERS} table
 *
 * <p>TODO: Figure out a way to validate the name and ip uniqueness without creating new validators
 * (Enum exclude list?)
 */
public final class UpdateServerPayload {

  /** Server UUID passed by user to remove */
  @ServerUUIDExistence(message = "You must pass a valid server UUID in order to delete")
  @ServerDetached(message = "You cannot update a server that is already linked to a pathstore node")
  private final UUID serverUUID;

  /**
   * Store an instance of the server class, check to see if the information allows you to connect to
   * a server
   */
  @ServerConnectionTest(
      message =
          "Could not connect to server, make sure that you have properly inputted the connection information")
  public final Server server;

  /**
   * @param serverUUID {@link #serverUUID}
   * @param ip ip of server
   * @param username username to connect
   * @param password password to connect
   * @param sshPort what port ssh is running on
   * @param rmiPort what rmi port you want that server to use
   * @param name what is the human readable name
   */
  public UpdateServerPayload(
      final String serverUUID,
      final String ip,
      final String username,
      final String password,
      final int sshPort,
      final int rmiPort,
      final String name) {
    this.serverUUID = UUID.fromString(serverUUID);
    this.server = new Server(this.serverUUID, ip, username, password, sshPort, rmiPort, name);
  }
}

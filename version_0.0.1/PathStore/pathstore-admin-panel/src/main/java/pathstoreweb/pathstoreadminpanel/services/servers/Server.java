package pathstoreweb.pathstoreadminpanel.services.servers;

import java.util.UUID;

/** This class represents a server object with relevant connection and identification information */
public class Server {

  /** Random uuid to denote this server in other tables */
  public final UUID serverUUID;

  /** Public ip of server (port 22, 9052, 1099 are open) */
  public final String ip;

  /** Username to ssh into server */
  public final String username;

  /** TODO: Maybe don't query? */
  public final String password;

  /** Human readable name for this server */
  public final String name;

  /**
   * @param serverUUID {@link #serverUUID}
   * @param ip {@link #ip}
   * @param username {@link #username}
   * @param password {@link #password}
   * @param name {@link #name}
   */
  public Server(
      final String serverUUID,
      final String ip,
      final String username,
      final String password,
      final String name) {
    this.serverUUID = UUID.fromString(serverUUID);
    this.ip = ip;
    this.username = username;
    this.password = password;
    this.name = name;
  }

  /**
   * @param serverUUID {@link #serverUUID}
   * @param ip {@link #ip}
   * @param username {@link #username}
   * @param password {@link #password}
   * @param name {@link #name}
   */
  public Server(
      final UUID serverUUID,
      final String ip,
      final String username,
      final String password,
      final String name) {
    this.serverUUID = serverUUID;
    this.ip = ip;
    this.username = username;
    this.password = password;
    this.name = name;
  }
}
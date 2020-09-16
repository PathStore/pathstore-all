package pathstore.common.tables;

import com.datastax.driver.core.Row;
import pathstore.util.BlobObject;

import java.util.UUID;

import static pathstore.common.Constants.SERVERS_COLUMNS.*;

/**
 * This is a in-memory representation of a row in the {@link pathstore.common.Constants#SERVERS}
 * table.
 *
 * <p>This is used to deploy children nodes
 *
 * <p>TODO: Finish comments
 *
 * <p>TODO: Make sure every reference to the servers table uses the build from row
 */
public class ServerEntry {
  public static ServerEntry fromRow(final Row row) {
    ServerAuthType authType = ServerAuthType.valueOf(row.getString(AUTH_TYPE));

    return new ServerEntry(
        UUID.fromString(row.getString(SERVER_UUID)),
        row.getString(IP),
        row.getString(USERNAME),
        authType,
        authType == ServerAuthType.PASSWORD ? row.getString(PASSWORD) : null,
        authType == ServerAuthType.IDENTITY
            ? (ServerIdentity) BlobObject.deserialize(row.getBytes(SERVER_IDENTITY))
            : null,
        row.getInt(SSH_PORT),
        row.getInt(RMI_PORT),
        row.getString(NAME));
  }

  public final UUID serverUUID;

  public final String ip;

  public final String username;

  public final ServerAuthType authType;

  public final String password;

  public final ServerIdentity serverIdentity;

  public final int sshPort;

  public final int rmiPort;

  /** Default cassandraPort. */
  public final int cassandraPort = 9052;

  public final String name;

  private ServerEntry(
      final UUID serverUUID,
      final String ip,
      final String username,
      final ServerAuthType authType,
      final String password,
      final ServerIdentity serverIdentity,
      final int sshPort,
      final int rmiPort,
      final String name) {
    this.serverUUID = serverUUID;
    this.ip = ip;
    this.username = username;
    this.authType = authType;
    this.password = password;
    this.serverIdentity = serverIdentity;
    this.sshPort = sshPort;
    this.rmiPort = rmiPort;
    this.name = name;
  }
}

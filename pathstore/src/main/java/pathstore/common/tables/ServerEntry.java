package pathstore.common.tables;

import com.datastax.driver.core.Row;
import pathstore.util.BlobObject;

import java.util.UUID;

import static pathstore.common.Constants.SERVERS_COLUMNS.*;

/**
 * This is a in-memory representation of a row in the {@link pathstore.common.Constants#SERVERS}
 * table.
 *
 * <p>This is used to deploy nodes using {@link
 * pathstore.system.deployment.utilities.SSHUtil#buildFromServerEntry(ServerEntry)}
 */
public class ServerEntry {
  /**
   * @param row {@link pathstore.common.Constants#SERVERS} row
   * @return built server entry with values associated with authentication type
   */
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
        row.getInt(GRPC_PORT),
        row.getString(NAME));
  }

  /** Server uuid */
  public final UUID serverUUID;

  /** Ip of server */
  public final String ip;

  /** Username */
  public final String username;

  /** Auth type of server */
  public final ServerAuthType authType;

  /** Password if authtype is {@link ServerAuthType#PASSWORD} */
  public final String password;

  /** Identity if authtype is {@link ServerAuthType#IDENTITY} */
  public final ServerIdentity serverIdentity;

  /** Ssh port to connect on */
  public final int sshPort;

  /** grpc port of server */
  public final int grpcPort;

  /** Default cassandraPort. */
  public final int cassandraPort = 9052;

  /** Human readable name of server */
  public final String name;

  /**
   * @param serverUUID {@link #serverUUID}
   * @param ip {@link #ip}
   * @param username {@link #username}
   * @param authType {@link #authType}
   * @param password {@link #password}
   * @param serverIdentity {@link #serverIdentity}
   * @param sshPort {@link #sshPort}
   * @param grpcPort {@link #grpcPort}
   * @param name {@link #name}
   */
  private ServerEntry(
      final UUID serverUUID,
      final String ip,
      final String username,
      final ServerAuthType authType,
      final String password,
      final ServerIdentity serverIdentity,
      final int sshPort,
      final int grpcPort,
      final String name) {
    this.serverUUID = serverUUID;
    this.ip = ip;
    this.username = username;
    this.authType = authType;
    this.password = password;
    this.serverIdentity = serverIdentity;
    this.sshPort = sshPort;
    this.grpcPort = grpcPort;
    this.name = name;
  }
}

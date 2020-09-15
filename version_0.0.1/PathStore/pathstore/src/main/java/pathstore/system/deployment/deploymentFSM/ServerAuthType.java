package pathstore.system.deployment.deploymentFSM;

/**
 * Used to flip the auth type between password authentication and private key with optional
 * passphrase.
 *
 * <p>stored in {@link pathstore.common.Constants.SERVERS_COLUMNS#AUTH_TYPE}
 *
 * <p>points to either {@link pathstore.common.Constants.SERVERS_COLUMNS#PASSWORD} if PASSWORD or if
 * IDENTITY points to {@link pathstore.common.Constants.SERVERS_COLUMNS#SERVER_IDENTITY}
 */
public enum ServerAuthType {
  PASSWORD,
  IDENTITY;
}

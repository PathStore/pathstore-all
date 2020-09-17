package pathstore.common.tables;

import pathstore.util.BlobObject;

/**
 * This class is used to denote a server identity for a server object. This specifically represents
 * those server objects who need RSA authentication
 *
 * <p>This class gets serialized to {@link
 * pathstore.common.Constants.SERVERS_COLUMNS#SERVER_IDENTITY}
 */
public class ServerIdentity implements BlobObject {
  /** Private key in bytes */
  public final byte[] privateKey;

  /** Optional passphrase */
  public final String passphrase;

  /** @param privateKey private key to give, if passphrase is not present */
  public ServerIdentity(final byte[] privateKey) {
    this(privateKey, null);
  }

  /**
   * @param privateKey private key to give
   * @param passphrase passphrase (can also be null)
   */
  public ServerIdentity(final byte[] privateKey, final String passphrase) {
    this.privateKey = privateKey;
    this.passphrase = passphrase;
  }
}

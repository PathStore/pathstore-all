package pathstore.authentication;

import com.datastax.driver.core.Session;
import pathstore.authentication.datalayerimpls.LocalAuth;
import pathstore.authentication.datalayerimpls.LocalClientAuth;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.concurrent.ConcurrentMap;

/**
 * This class is used to cache all credentials in the pathstore_appliactions.local_auth table into
 * memory.
 *
 * <p>All writes to that table should be done through this class if they're related to this node
 * specifically.
 *
 * @see pathstore.client.PathStoreCluster
 * @see PathStorePrivilegedCluster
 */
public final class CredentialCache<T> {

  /** Instance of the cache */
  private static CredentialCache<Integer> nodeAuth = null;

  /** @return returns an instance of the cache. Creates one if not already created */
  public static synchronized CredentialCache<Integer> getNodeAuth() {
    if (nodeAuth == null) nodeAuth = new CredentialCache<>(new LocalAuth());
    return nodeAuth;
  }

  /** Instance of the client auth cache */
  private static CredentialCache<String> clientAuth = null;

  /** @return returns an instance of the cache. Creates one if not already created */
  public static synchronized CredentialCache<String> getClientAuth() {
    if (clientAuth == null) clientAuth = new CredentialCache<>(new LocalClientAuth());
    return clientAuth;
  }

  /** Session used to modify the local database. */
  private final Session privSession = PathStorePrivilegedCluster.getSuperUserInstance().connect();

  /**
   * How to perform database operations on the given credential type
   *
   * @apiNote This is public because {@link
   *     pathstore.system.deployment.commands.WriteCredentialsToChildNode}
   */
  public final CredentialDataLayer<T> credentialDataLayer;

  /** Internal cache of credentials based on node id */
  private final ConcurrentMap<T, Credential<T>> credentials;

  /**
   * Loads all existing credentials from the local table into memory
   *
   * @param credentialDataLayer how to readAndWrite
   */
  private CredentialCache(final CredentialDataLayer<T> credentialDataLayer) {
    this.credentialDataLayer = credentialDataLayer;
    this.credentials = this.credentialDataLayer.load(this.privSession);
  }

  /**
   * @param primaryKey primary key of credential
   * @param username username of account
   * @param password password of account
   */
  public void add(final T primaryKey, final String username, final String password) {
    this.credentials.put(
        primaryKey,
        this.credentialDataLayer.write(
            this.privSession, new Credential<>(primaryKey, username, password)));
  }

  /**
   * This will remove a specific node id from the internal map and from the table
   *
   * @param primaryKey node id to remove
   * @return true if deletion occurred
   */
  public boolean remove(final T primaryKey) {
    Credential<T> credential = this.getCredential(primaryKey);

    if (credential == null) return false;

    this.credentials.remove(primaryKey);

    this.credentialDataLayer.delete(this.privSession, credential);

    return true;
  }

  /**
   * @param primaryKey node id to get credential from
   * @return credential object, may be null
   */
  public Credential<T> getCredential(final T primaryKey) {
    return this.credentials.get(primaryKey);
  }
}

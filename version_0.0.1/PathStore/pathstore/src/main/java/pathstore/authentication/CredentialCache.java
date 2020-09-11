package pathstore.authentication;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.Constants;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
public final class CredentialCache {

  /** Instance of the cache */
  private static CredentialCache instance = null;

  /** @return returns an instance of the cache. Creates one if not already created */
  public static synchronized CredentialCache getInstance() {
    if (instance == null) instance = new CredentialCache();
    return instance;
  }

  /** Internal cache of credentials based on node id */
  private final ConcurrentMap<Integer, Credential> credentials;

  /** Session used to modify the local database. */
  private final Session privSession = PathStorePrivilegedCluster.getSuperUserInstance().connect();

  /** Load all credentials on object creation */
  private CredentialCache() {
    this.credentials = this.load();
  }

  /**
   * Read all records from auth table, transform them to credential objects and build a concurrent
   * map from it
   *
   * @return return concurrent built map of existing credentials in the database
   */
  private ConcurrentMap<Integer, Credential> load() {
    return StreamSupport.stream(
            this.privSession
                .execute(
                    QueryBuilder.select()
                        .all()
                        .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_AUTH))
                .spliterator(),
            true)
        .map(Credential::buildFromRow)
        .collect(Collectors.toConcurrentMap(credential -> credential.node_id, Function.identity()));
  }

  /**
   * @param nodeId node id of credential
   * @param username username of account
   * @param password password of account
   */
  public void add(final int nodeId, final String username, final String password) {
    this.credentials.put(
        nodeId,
        Credential.writeCredentialToRow(
            this.privSession, new Credential(nodeId, username, password)));
  }

  /**
   * This will remove a specific node id from the internal map and from the table
   *
   * @param nodeId node id to remove
   */
  public void remove(final int nodeId) {
    Credential credential = this.getCredential(nodeId);

    if (credential == null) return;

    this.credentials.remove(nodeId);

    this.privSession.execute(
        QueryBuilder.delete()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_AUTH)
            .where(QueryBuilder.eq(Constants.LOCAL_AUTH_COLUMNS.NODE_ID, nodeId)));
  }

  /**
   * @param node_id node id to get credential from
   * @return credential object, may be null
   */
  public Credential getCredential(final int node_id) {
    return this.credentials.get(node_id);
  }
}

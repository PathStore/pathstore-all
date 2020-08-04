package pathstore.util;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;
import pathstore.authentication.Credential;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class is used by {@link pathstore.client.PathStoreCluster} and {@link
 * PathStorePrivilegedCluster} to cache their clusters based on credentials.
 *
 * @param <T>
 */
public class ClusterCache<T> {
  private final ConcurrentMap<Credential, T> cache = new ConcurrentHashMap<>();

  private final DoubleConsumerFunction<Credential, Cluster, T> buildFunction;

  public ClusterCache(final DoubleConsumerFunction<Credential, Cluster, T> buildFunction) {
    this.buildFunction = buildFunction;
  }

  public T getInstance(final Credential credential, final String ip, final int port) {
    T object = this.cache.get(credential);

    if (object == null) {
      object =
          this.buildFunction.apply(
              credential, createCluster(ip, port, credential.username, credential.password));
      this.cache.put(credential, object);
    }

    return object;
  }

  public void remove(final Credential credential) {
    this.cache.remove(credential);
  }

  /**
   * Create a cluster connection
   *
   * @param ip ip to connect to
   * @param port port to connect to
   * @param username username to login with
   * @param password password to login with
   * @return connected cluster (potentially throws {@link
   *     com.datastax.driver.core.exceptions.NoHostAvailableException})
   */
  public static Cluster createCluster(
      final String ip, final int port, final String username, final String password) {
    return new Cluster.Builder()
        .addContactPoints(ip)
        .withPort(port)
        .withCredentials(username, password)
        .withSocketOptions((new SocketOptions()).setTcpNoDelay(true).setReadTimeoutMillis(15000000))
        .withQueryOptions(
            (new QueryOptions())
                .setRefreshNodeIntervalMillis(0)
                .setRefreshNodeListIntervalMillis(0)
                .setRefreshSchemaIntervalMillis(0))
        .build();
  }
}

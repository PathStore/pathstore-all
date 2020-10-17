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
  /** Where clusters are cached */
  private final ConcurrentMap<Credential<Integer>, T> cache = new ConcurrentHashMap<>();

  /** How to build a cluster not present in the cache */
  private final DoubleConsumerFunction<Credential<Integer>, Cluster, T> buildFunction;

  /** @param buildFunction {@link #buildFunction} */
  public ClusterCache(final DoubleConsumerFunction<Credential<Integer>, Cluster, T> buildFunction) {
    this.buildFunction = buildFunction;
  }

  /**
   * This function is used to gather a cluster from the cache, if not already present it will create
   * one, store it, and return it. Else it will just return the existing cluster
   *
   * @param credential credential object
   * @param ip ip of server
   * @param port port of server
   * @return cluster object
   */
  public T getInstance(final Credential<Integer> credential, final String ip, final int port) {
    T object = this.cache.get(credential);

    if (object == null) {
      object =
          this.buildFunction.apply(
              credential, createCluster(ip, port, credential.username, credential.password));
      this.cache.put(credential, object);
    }

    return object;
  }

  /** @param credential credential to remove from cluster */
  public void remove(final Credential<Integer> credential) {
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

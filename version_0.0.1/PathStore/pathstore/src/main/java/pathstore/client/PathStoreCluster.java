/**
 * ********
 *
 * <p>Copyright 2019 Eyal de Lara, Seyed Hossein Mortazavi, Mohammad Salehe
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*********
 */
package pathstore.client;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;
import pathstore.authentication.Credential;
import pathstore.common.PathStoreProperties;
import pathstore.util.ClusterCache;

public class PathStoreCluster {

  private static final ClusterCache<PathStoreCluster> clusterCache =
      new ClusterCache<>(PathStoreCluster::new);

  public static synchronized PathStoreCluster getInstance() {
    return clusterCache.getInstance(
        PathStoreProperties.getInstance().credential,
        PathStoreProperties.getInstance().CassandraIP,
        PathStoreProperties.getInstance().CassandraPort);
  }

  public static synchronized PathStoreCluster getInstance(final Credential credential) {
    return clusterCache.getInstance(
        credential,
        PathStoreProperties.getInstance().CassandraIP,
        PathStoreProperties.getInstance().CassandraPort);
  }

  private final Credential credential;

  private final Cluster cluster;

  private final PathStoreSession session;

  public PathStoreCluster(final Credential credential, final Cluster cluster) {
    this.credential = credential;
    this.cluster = cluster;
    this.session = new PathStoreSession(this.cluster);
  }

  // todo: remove
  public PathStoreCluster(PathStoreProperties custom) {
    this.credential = null;
    this.cluster =
        new Cluster.Builder()
            .addContactPoints(custom.CassandraIP)
            .withPort(custom.CassandraPort)
            .withCredentials("cassandra", "cassandra")
            .withSocketOptions(
                new SocketOptions().setTcpNoDelay(true).setReadTimeoutMillis(15000000))
            .withQueryOptions(
                new QueryOptions()
                    .setRefreshNodeIntervalMillis(0)
                    .setRefreshNodeListIntervalMillis(0)
                    .setRefreshSchemaIntervalMillis(0))
            .build();

    session = new PathStoreSession(this.cluster);
  }

  public PathStoreSession connect() {
    return this.session;
  }

  public void close() {
    this.session.close();
    this.cluster.close();
    clusterCache.remove(this.credential);
  }
}

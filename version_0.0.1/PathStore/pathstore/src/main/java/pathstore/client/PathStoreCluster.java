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
import pathstore.authentication.CredentialCache;
import pathstore.common.PathStoreProperties;
import pathstore.util.ClusterCache;

/**
 * This class represents a connection to the database with log compression.
 *
 * <p>This connection should be used if you plan to read or write data in the pathstore context.
 * This is specifically used for daemons that utilize pathstore to control how the system works. An
 * example of this is the {@link pathstore.system.schemaFSM.PathStoreSlaveSchemaServer}
 *
 * <p>All authentication is determined by the {@link CredentialCache}
 */
public class PathStoreCluster {

  /** Cache of pathstore cluster objects based on credential */
  private static final ClusterCache<PathStoreCluster> clusterCache =
      new ClusterCache<>(PathStoreCluster::new);

  /**
   * This will return the a pathstore cluster with super user privileges if being used on a node. As
   * the credentials provided in the properties file will have super user access.
   *
   * @return super user privileged pathstore cluster
   */
  public static PathStoreCluster getSuperUserInstance() {
    PathStoreProperties.getInstance().verifyCassandraSuperUserCredentials();
    PathStoreProperties.getInstance().verifyCassandraConnectionDetails();

    return clusterCache.getInstance(
        PathStoreProperties.getInstance().credential,
        PathStoreProperties.getInstance().CassandraIP,
        PathStoreProperties.getInstance().CassandraPort);
  }

  /**
   * This will get the credential information from the credential cache with the current node id.
   *
   * @return daemon user privileged pathstore cluster
   * @see CredentialCache
   */
  public static PathStoreCluster getDaemonInstance() {
    Credential daemonCredentials =
        CredentialCache.getInstance().getCredential(PathStoreProperties.getInstance().NodeID);

    if (daemonCredentials == null)
      throw new RuntimeException("Daemon credentials are not present within the local auth table");

    PathStoreProperties.getInstance().verifyCassandraConnectionDetails();

    return clusterCache.getInstance(
        daemonCredentials,
        PathStoreProperties.getInstance().CassandraIP,
        PathStoreProperties.getInstance().CassandraPort);
  }

  /** Credential object used to create the cluster, this is only used for disconnection */
  private final Credential credential;

  /** Cluster object, used to close the cluster */
  private final Cluster cluster;

  /** PathStoreSession, used to compress the responses from the database */
  private final PathStoreSession session;

  /**
   * @param credential {@link #credential}
   * @param cluster {@link #cluster}
   */
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

  /** @return ps session */
  public PathStoreSession connect() {
    return this.session;
  }

  /**
   * This should be used to disconnect the cluster. Once this is called the object you have will be
   * disconnected.
   */
  public void close() {
    this.session.close();
    this.cluster.close();
    clusterCache.remove(this.credential);
  }
}

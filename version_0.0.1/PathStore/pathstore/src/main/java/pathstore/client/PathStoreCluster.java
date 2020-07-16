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

import pathstore.common.PathStoreProperties;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;

/**
 * This is the main class that represents the Cluster as a whole. This class allows you to interact
 * with the local Path store session through {@link PathStoreSession}
 *
 * @see #getInstance()
 * @see PathStoreSession
 */
public class PathStoreCluster {

  /**
   * Stores a static reference to the current instance of path store cluster.
   *
   * @see #getInstance() for how this value is initalized
   */
  private static PathStoreCluster pathStoreCluster = null;

  /** TODO: Document meaning of {@link Cluster} */
  private final Cluster cluster;

  /**
   * Represents a session that is tied to the current cluster instance
   *
   * @see #getInstance()
   * @see #PathStoreCluster()
   */
  private final PathStoreSession session;

  /**
   * TODO: Implement a get instance function with a custom pathstore properties location
   *
   * <p>Creates an instance of a path store cluster to be used throughout the program. This allows
   * for calling this function to get the instance instead of passing the instance around
   *
   * @return either a new copy of {@link PathStoreCluster} or the current saved instance.
   */
  public static synchronized PathStoreCluster getInstance() {
    if (PathStoreCluster.pathStoreCluster == null)
      PathStoreCluster.pathStoreCluster = new PathStoreCluster();
    return PathStoreCluster.pathStoreCluster;
  }

  /**
   * TODO: Make private for public implementation. As we have {@link #getInstance()} This is used
   * under the assumption that /etc/pathstore/pathstore.properties is where your
   * pathstore.properties file is located. If it isn't see {@link
   * #PathStoreCluster(PathStoreProperties)}
   *
   * <p>First we create a cluster TODO: explain the cluster and instantiate {@link #session}
   */
  public PathStoreCluster() {
    //		System.out.println("Cluster IP " +
    //				PathStoreProperties.getInstance().CassandraIP + " Port " +
    //				PathStoreProperties.getInstance().CassandraPort);
    this.cluster =
        new Cluster.Builder()
            .addContactPoints(PathStoreProperties.getInstance().CassandraIP)
            .withPort(PathStoreProperties.getInstance().CassandraPort)
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

  /**
   * TODO: Make private for public implementation. As we have {@link #getInstance()} also need to
   * add an instance for custom properties Similar to above but you can pass a custom properties
   * file.
   *
   * @param custom custom properties file
   */
  public PathStoreCluster(PathStoreProperties custom) {
    //		System.out.println("Cluster IP " +
    //				custom.CassandraIP + " Port " +
    //				custom.CassandraPort);
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

  /**
   * TODO: Explain cluster's metadata
   *
   * @return current clusters meta data
   */
  public Metadata getMetadata() {
    return cluster.getMetadata();
  }

  /**
   * Return node id from properties file
   *
   * @return node id
   * @see PathStoreProperties
   */
  public int getClusterId() {
    return PathStoreProperties.getInstance().NodeID;
  }

  /** @return current path store session */
  public PathStoreSession connect() {
    return this.session;
  }

  /** Allows the user to close their connection to pathstore */
  public void close() {
    this.session.close();
    this.cluster.close();
  }
}

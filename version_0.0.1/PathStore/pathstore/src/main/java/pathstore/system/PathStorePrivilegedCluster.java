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
package pathstore.system;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;
import pathstore.authentication.Credential;
import pathstore.authentication.CredentialCache;
import pathstore.common.PathStoreProperties;
import pathstore.util.ClusterCache;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This cluster is used to create a raw connection to the local node's database. This is used to
 * read data from local tables and to read raw pathstore data. Some reasons to do this are, push and
 * pull operations, and gc.
 */
public class PathStorePrivilegedCluster {

  /** Cache of connections based on credential objects */
  private static final ClusterCache<PathStorePrivilegedCluster> clusterCache =
      new ClusterCache<>(PathStorePrivilegedCluster::new);

  /**
   * Credentials of super user account gathered from properties file
   *
   * @return super user instance
   */
  public static PathStorePrivilegedCluster getSuperUserInstance() {
    return clusterCache.getInstance(
        PathStoreProperties.getInstance().credential,
        PathStoreProperties.getInstance().CassandraIP,
        PathStoreProperties.getInstance().CassandraPort);
  }

  /**
   * Credentials from Credential Cache for this node id.
   *
   * @return daemon instance
   * @see CredentialCache
   * @see pathstore.system.deployment.commands.WriteCredentialsToChildNode for how these credentials
   *     are boot strapped
   */
  public static PathStorePrivilegedCluster getDaemonInstance() {
    return clusterCache.getInstance(
        CredentialCache.getInstance().getCredential(PathStoreProperties.getInstance().NodeID),
        PathStoreProperties.getInstance().CassandraIP,
        PathStoreProperties.getInstance().CassandraPort);
  }

  /**
   * Credentials from Credential Cache for parent node.
   *
   * @return parent database instance
   * @see CredentialCache
   * @see pathstore.system.deployment.commands.WriteCredentialsToChildNode for how the parent
   *     credentials are boot strapped
   */
  public static PathStorePrivilegedCluster getParentInstance() {
    return clusterCache.getInstance(
        CredentialCache.getInstance().getCredential(PathStoreProperties.getInstance().ParentID),
        PathStoreProperties.getInstance().CassandraParentIP,
        PathStoreProperties.getInstance().CassandraParentPort);
  }

  /**
   * Credentials to a child node that is in the cluster cache (Used during un-deployment)
   *
   * @param childNodeId child node id
   * @param ip ip of child
   * @param port child port
   * @return child connection
   */
  public static PathStorePrivilegedCluster getChildInstance(
      final int childNodeId, final String ip, final int port) {
    return clusterCache.getInstance(
        CredentialCache.getInstance().getCredential(childNodeId), ip, port);
  }

  /**
   * Credentials to a child node that is not in the cluster cache (Used during deployment)
   *
   * @param username username
   * @param password password
   * @param ip child ip
   * @param port child port
   * @return child connection
   */
  public static PathStorePrivilegedCluster getChildInstance(
      final String username, final String password, final String ip, final int port) {
    return clusterCache.getInstance(new Credential(ipToInt(ip), username, password), ip, port);
  }

  /**
   * Converts an ipv4 address to an integer. This avoids collisions in the cluster cache
   *
   * @param ip ip to convert
   * @return converted ip
   */
  private static int ipToInt(final String ip) {
    try {
      int result = 0;

      for (byte b : InetAddress.getByName(ip).getAddress()) result = result << 8 | (b & 0xFF);

      return result;
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    return -1;
  }

  /** Credential used */
  private final Credential credential;

  /** Cluster */
  private final Cluster cluster;

  /** Session */
  private final Session session;

  /**
   * @param credential {@link #credential}
   * @param cluster {@link #cluster}
   */
  public PathStorePrivilegedCluster(final Credential credential, final Cluster cluster) {
    this.credential = credential;
    this.cluster = cluster;
    this.session = cluster.connect();
  }

  /**
   * Metadata is used to understand the composition of the database
   *
   * @return cluster metadata
   */
  public Metadata getMetadata() {
    return this.cluster.getMetadata();
  }

  /** @return session object */
  public Session connect() {
    return this.session;
  }

  /** Close session and remove from cache */
  public void close() {
    this.session.close();
    this.cluster.close();
    clusterCache.remove(this.credential);
  }
}

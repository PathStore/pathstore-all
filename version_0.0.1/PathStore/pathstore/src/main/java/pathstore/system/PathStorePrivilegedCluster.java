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
import pathstore.authentication.CredentialInfo;
import pathstore.common.PathStoreProperties;
import pathstore.util.ClusterCache;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PathStorePrivilegedCluster {

  private static final ClusterCache<PathStorePrivilegedCluster> clusterCache =
      new ClusterCache<>(PathStorePrivilegedCluster::new);

  public static synchronized PathStorePrivilegedCluster getSuperUserInstance() {
    return clusterCache.getInstance(
        PathStoreProperties.getInstance().credential,
        PathStoreProperties.getInstance().CassandraIP,
        PathStoreProperties.getInstance().CassandraPort);
  }

  public static synchronized PathStorePrivilegedCluster getDaemonInstance() {
    return clusterCache.getInstance(
        CredentialInfo.getInstance().getCredential(PathStoreProperties.getInstance().NodeID),
        PathStoreProperties.getInstance().CassandraIP,
        PathStoreProperties.getInstance().CassandraPort);
  }

  public static synchronized PathStorePrivilegedCluster getParentInstance() {
    return clusterCache.getInstance(
        CredentialInfo.getInstance().getCredential(PathStoreProperties.getInstance().ParentID),
        PathStoreProperties.getInstance().CassandraParentIP,
        PathStoreProperties.getInstance().CassandraParentPort);
  }

  // Query ip and port of child node? Seems to be easier. For now we will leave it without
  public static synchronized PathStorePrivilegedCluster getChildInstance(
      final int childNodeId, final String ip, final int port) {
    return clusterCache.getInstance(
        CredentialInfo.getInstance().getCredential(childNodeId), ip, port);
  }

  // used during deployment
  public static PathStorePrivilegedCluster getChildInstance(
      final String username, final String password, final String ip, final int port) {
    return clusterCache.getInstance(new Credential(ipToInt(ip), username, password), ip, port);
  }

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

  private final Credential credential;

  private final Cluster cluster;

  private final Session session;

  public PathStorePrivilegedCluster(final Credential credential, final Cluster cluster) {
    this.credential = credential;
    this.cluster = cluster;
    this.session = cluster.connect();
  }

  public Metadata getMetadata() {
    return this.cluster.getMetadata();
  }

  public Session connect() {
    return this.session;
  }

  public void close() {
    this.session.close();
    this.cluster.close();
    clusterCache.remove(this.credential);
  }
}

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

import authentication.Credential;
import authentication.CredentialInfo;
import pathstore.common.PathStoreProperties;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;
import pathstore.util.ClusterCache;

public class PathStorePrivilegedCluster {

  private static final ClusterCache<PathStorePrivilegedCluster> clusterCache =
      new ClusterCache<>(PathStorePrivilegedCluster::new);

  private final Credential credential;

  private final Cluster cluster;

  private final Session session;

  public static synchronized PathStorePrivilegedCluster getInstance() {
    return clusterCache.getInstance(
        PathStoreProperties.getInstance().credential,
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
    System.out.println("Called child instance");
    return clusterCache.getInstance(
        CredentialInfo.getInstance().getCredential(childNodeId), ip, port);
  }

  // used during deployment
  public static synchronized PathStorePrivilegedCluster getChildInstance(
      final String username, final String password, final String ip, final int port) {
    return clusterCache.getInstance(new Credential(-1, username, password), ip, port);
  }

  public PathStorePrivilegedCluster(final Credential credential, final Cluster cluster) {
    this.credential = credential;
    this.cluster = cluster;
    this.session = cluster.connect();
  }

  public Metadata getMetadata() {
    return cluster.getMetadata();
  }

  public Session connect() {
    return session;
  }

  public void close() {
    this.session.close();
    this.cluster.close();
    clusterCache.remove(this.credential);
  }
}

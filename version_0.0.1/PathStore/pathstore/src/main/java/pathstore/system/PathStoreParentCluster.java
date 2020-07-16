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

import pathstore.common.PathStoreProperties;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;

/** TODO: Copy of {@link pathstore.client.PathStoreCluster} */
public class PathStoreParentCluster {

  static PathStoreParentCluster pathStoreCluster = null;

  private Cluster cluster = null;
  private Session session = null;

  public static PathStoreParentCluster getInstance() {
    if (PathStoreParentCluster.pathStoreCluster == null)
      PathStoreParentCluster.pathStoreCluster = new PathStoreParentCluster();
    return PathStoreParentCluster.pathStoreCluster;
  }

  public PathStoreParentCluster() {

    //			System.out.println("Connecting to Parent:" +
    //					PathStoreProperties.getInstance().CassandraParentIP + " " +
    //					PathStoreProperties.getInstance().CassandraParentPort);

    this.cluster =
        new Cluster.Builder()
            .addContactPoints(PathStoreProperties.getInstance().CassandraParentIP)
            .withPort(PathStoreProperties.getInstance().CassandraParentPort)
            .withCredentials("cassandra", "cassandra")
            .withSocketOptions(
                new SocketOptions().setTcpNoDelay(true).setReadTimeoutMillis(15000000))
            .withQueryOptions(
                new QueryOptions()
                    .setRefreshNodeIntervalMillis(0)
                    .setRefreshNodeListIntervalMillis(0)
                    .setRefreshSchemaIntervalMillis(0))
            .build();
    this.session = cluster.connect();
  }

  public Session connect() {
    // return this.cluster.connect();
    return session;
  }
}

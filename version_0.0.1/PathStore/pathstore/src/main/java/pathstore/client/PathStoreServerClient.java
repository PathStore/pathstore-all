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
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCacheEntry;
import pathstore.common.Role;
import pathstore.exception.PathStoreRemoteException;
import pathstore.sessions.SessionToken;
import pathstore.util.SchemaInfo;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Will comment this class when merger occurs to grpc */
public class PathStoreServerClient {

  private static PathStoreServerClient instance = null;

  private final PathStoreServer stub;

  public static synchronized PathStoreServerClient getInstance() {
    if (PathStoreServerClient.instance == null)
      PathStoreServerClient.instance = new PathStoreServerClient();
    return PathStoreServerClient.instance;
  }

  public static PathStoreServerClient getCustom(final String ip, final int port) {
    return new PathStoreServerClient(ip, port);
  }

  public PathStoreServerClient(final String ip, final int port) {
    try {
      Registry registry = LocateRegistry.getRegistry(ip, port);
      this.stub = (PathStoreServer) registry.lookup("PathStoreServer");
    } catch (Exception e) {
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  public PathStoreServerClient() {
    try {
      Registry registry = null;

      if (PathStoreProperties.getInstance().role == Role.SERVER)
        registry =
            LocateRegistry.getRegistry(
                PathStoreProperties.getInstance().RMIRegistryParentIP,
                PathStoreProperties.getInstance().RMIRegistryParentPort);
      else {
        registry =
            LocateRegistry.getRegistry(
                PathStoreProperties.getInstance().RMIRegistryIP,
                PathStoreProperties.getInstance().RMIRegistryPort);
      }
      stub = (PathStoreServer) registry.lookup("PathStoreServer");
      System.out.println("creating new stub to parent");
    } catch (Exception e) {
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  public void addQueryEntry(QueryCacheEntry entry) {
    try {
      long t = System.nanoTime();

      String result =
          stub.addQueryEntry(
              entry.getKeyspace(), entry.getTable(), entry.getClausesSerialized(), entry.limit);
      // System.out.println("time took to add to parent: " + Timer.getTime(t));
      result = "";
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  public UUID cretateQueryDelta(QueryCacheEntry entry) {
    try {
      return stub.createQueryDelta(
          entry.getKeyspace(),
          entry.getTable(),
          entry.getClausesSerialized(),
          entry.getParentTimeStamp(),
          PathStoreProperties.getInstance().NodeID,
          entry.limit);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  public SchemaInfo getSchemaInfo(final String keyspace) {
    try {
      return this.stub.getSchemaInfo(keyspace);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Optional<String> registerApplication(final String applicationName, final String password) {
    try {
      return Optional.ofNullable(this.stub.registerApplication(applicationName, password));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  public boolean validateSession(final SessionToken sessionToken) {
    try {
      return this.stub.validateSession(sessionToken);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    return false;
  }

  public void forcePush(final List<SchemaInfo.Table> tablesToPush, final int lca) {
    try {
      this.stub.forcePush(tablesToPush, lca);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  public int getLocalNodeId() {
    try {
      return this.stub.getLocalNodeId();
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    return -1;
  }
}

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

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCacheEntry;
import pathstore.common.Role;
import pathstore.common.Timer;
import pathstore.exception.PathMigrateAlreadyGoneException;
import pathstore.exception.PathStoreRemoteException;

/** TODO: Comment */
public class PathStoreServerClient {

  private static PathStoreServerClient instance = null;

  private PathStoreServer stub;

  public static PathStoreServerClient getInstance() {
    if (PathStoreServerClient.instance == null)
      PathStoreServerClient.instance = new PathStoreServerClient();
    return PathStoreServerClient.instance;
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
      // TODO Auto-generated catch block
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
      System.out.println(e);
      throw new PathStoreRemoteException();
    }
  }

  public void addCommandEntry(String device, QueryCacheEntry entry)
      throws PathMigrateAlreadyGoneException, PathStoreRemoteException {
    // try {
    String result;
    try {
      result =
          stub.addUserCommandEntry(
              device,
              entry.getKeyspace(),
              entry.getTable(),
              entry.getClausesSerialized(),
              entry.limit);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      if (e.getMessage().equals("PathMigrateAlreadyGoneException"))
        throw new PathMigrateAlreadyGoneException();
      else throw new PathStoreRemoteException();
    } catch (PathMigrateAlreadyGoneException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new PathMigrateAlreadyGoneException();
    }

    result = "";
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
}

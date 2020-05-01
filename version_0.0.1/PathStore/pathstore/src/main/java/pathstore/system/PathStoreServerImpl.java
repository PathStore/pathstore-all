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

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import com.datastax.driver.core.Session;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreServer;
import pathstore.common.Role;

import org.apache.commons.cli.*;
import pathstore.system.deployment.deploymentFSM.PathStoreDeploymentUtils;
import pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils;
import pathstore.util.SchemaInfo;

public class PathStoreServerImpl {

  /**
   * Startup tasks:
   *
   * <p>0: setup rmi server 1: load applications keyspace 2: start
   * daemons
   *
   * @param args
   */
  public static void main(final String args[]) {
    try {

      Session local = PathStorePriviledgedCluster.getInstance().connect();

      PathStoreServerImplRMI obj = new PathStoreServerImplRMI();
      PathStoreServer stub = (PathStoreServer) UnicastRemoteObject.exportObject(obj, 0);

      System.out.println(PathStoreProperties.getInstance().RMIRegistryIP);

      System.setProperty(
          "java.rmi.server.hostname", PathStoreProperties.getInstance().ExternalAddress);
      Registry registry =
          LocateRegistry.createRegistry(PathStoreProperties.getInstance().RMIRegistryPort);

      try {
        System.out.println("Binding rmi connection");
        registry.bind("PathStoreServer", stub);
        PathStoreDeploymentUtils.writeTaskDone(local, 0);
      } catch (Exception ex) {
        System.out.println("Could not bind, trying again");
        registry.rebind("PathStoreServer", stub);
        PathStoreDeploymentUtils.writeTaskDone(local, 0);
      }

      if (!SchemaInfo.getInstance().getSchemaInfo().containsKey(Constants.PATHSTORE_APPLICATIONS)) {
        PathStoreSchemaLoaderUtils.loadApplicationSchema(local);
        PathStoreDeploymentUtils.writeTaskDone(local, 1);
      }

      SchemaInfo.getInstance().reset();

      System.err.println("PathStoreServer ready");
      System.out.println(PathStoreProperties.getInstance());

      PathStoreDeploymentUtils.writeTaskDone(local, 2);
      obj.startDaemons();

    } catch (Exception e) {
      System.err.println("PathStoreServer exception: " + e.toString());
      e.printStackTrace();
    }
  }
}

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

import com.datastax.driver.core.Session;
import pathstore.common.*;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.system.deployment.deploymentFSM.PathStoreDeploymentUtils;
import pathstore.system.deployment.deploymentFSM.PathStoreMasterDeploymentServer;
import pathstore.system.deployment.deploymentFSM.PathStoreSlaveDeploymentServer;
import pathstore.system.logging.PathStoreLoggerDaemon;
import pathstore.system.schemaFSM.PathStoreMasterSchemaServer;
import pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils;
import pathstore.system.schemaFSM.PathStoreSlaveSchemaServer;
import pathstore.util.SchemaInfo;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class PathStoreServerImpl {

  /** Logger for {@link PathStoreServerImpl} */
  private static final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreServerImpl.class);

  /**
   * Startup tasks:
   *
   * <p>0: setup rmi server 1: load applications keyspace 2: start daemons
   *
   * @param args
   */
  public static void main(final String args[]) {
    try {

      logger.info(
          String.format(
              "PathStore has started up with the external ip of %s",
              PathStoreProperties.getInstance().ExternalAddress));

      logger.debug(String.format("Properties info:\n%s", PathStoreProperties.getInstance()));

      logger.info("Initial connect to database");

      Session local = PathStorePrivilegedCluster.getSuperUserInstance().connect();

      logger.info("Connected");

      PathStoreServer stub =
          (PathStoreServer)
              UnicastRemoteObject.exportObject(PathStoreServerImplRMI.getInstance(), 0);

      System.out.println(PathStoreProperties.getInstance().ExternalAddress);

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

      logger.info("Binded to java RMI");

      if (!SchemaInfo.getInstance().isKeyspaceLoaded(Constants.PATHSTORE_APPLICATIONS)) {
        logger.info("Application keyspace not detected, attempting to load");
        PathStoreSchemaLoaderUtils.loadApplicationSchema(local);
        SchemaInfo.getInstance().loadKeyspace(Constants.PATHSTORE_APPLICATIONS);
      } else logger.info("Application keyspace already loaded");

      PathStoreDeploymentUtils.writeTaskDone(local, 1);

      logger.info("Application keyspace successfully loaded");

      logger.info("PathStore Ready");

      PathStoreDeploymentUtils.writeTaskDone(local, 2);

      spawnDaemons();
    } catch (Exception e) {
      logger.error(e);
    }
  }

  /** Spawns daemons based on server role */
  private static void spawnDaemons() {
    PathStoreThreadManager daemonManager = PathStoreThreadManager.getDaemonInstance();
    daemonManager
        .spawn(new PathStoreLoggerDaemon())
        .spawn(new PathStoreSlaveDeploymentServer())
        .spawn(new PathStoreSlaveSchemaServer());

    if (PathStoreProperties.getInstance().role != Role.ROOTSERVER)
      daemonManager.spawn(new PathStorePushServer()).spawn(new PathStorePullServer());
    else
      daemonManager
          .spawn(new PathStoreMasterDeploymentServer())
          .spawn(new PathStoreMasterSchemaServer());
  }
}

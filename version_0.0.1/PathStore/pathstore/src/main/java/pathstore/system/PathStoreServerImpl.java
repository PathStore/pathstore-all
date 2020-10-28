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
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pathstore.authentication.CredentialCache;
import pathstore.authentication.grpc.AuthManager;
import pathstore.authentication.grpc.AuthServerInterceptor;
import pathstore.common.*;
import pathstore.system.deployment.deploymentFSM.PathStoreDeploymentUtils;
import pathstore.system.deployment.deploymentFSM.PathStoreMasterDeploymentServer;
import pathstore.system.deployment.deploymentFSM.PathStoreSlaveDeploymentServer;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerDaemon;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.system.network.*;
import pathstore.system.schemaFSM.PathStoreMasterSchemaServer;
import pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils;
import pathstore.system.schemaFSM.PathStoreSlaveSchemaServer;
import pathstore.util.SchemaInfo;

/**
 * This is the starting point of a pathstore server.
 *
 * <p>It is used to spawn daemon threads and verify that all connections that are needed are valid.
 *
 * <p>It will also write to its startup table that is finished certain steps to allow the deployer
 * of this process to autonomically verify the node is started up
 *
 * <p>TODO: Add shutdown hook for grpc server + close communication to parent + close all cluster
 * connections to local / parent / children cassandra and serialize qc into db
 */
public class PathStoreServerImpl {

  /** Logger for {@link PathStoreServerImpl} */
  private static final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreServerImpl.class);

  private static Server server;

  /**
   * Startup tasks:
   *
   * <p>0: setup grpc server 1: load applications keyspace 2: start daemons
   *
   * @param args
   */
  public static void main(final String args[]) {
    try {

      logger.info(Constants.ASCII_ART);

      logger.info(
          String.format(
              "PathStore has started up with the external ip of %s",
              PathStoreProperties.getInstance().ExternalAddress));

      logger.debug(String.format("Properties info:\n%s", PathStoreProperties.getInstance()));

      if (PathStoreProperties.getInstance().credential != null)
        logger.info("Loaded super user account successfully");
      else logger.error("Couldn't load super user account");

      Session local = PathStorePrivilegedCluster.getSuperUserInstance().connect();

      logger.info("Super User connection was initialized successfully");

      if (CredentialCache.getNodeAuth().getCredential(PathStoreProperties.getInstance().NodeID)
          != null) logger.info("Loaded daemon account successfully");
      else logger.error("Couldn't load daemon account");

      PathStorePrivilegedCluster.getDaemonInstance().connect();

      logger.info("Daemon connection was initialized successfully");

      System.out.println(PathStoreProperties.getInstance().ExternalAddress);

      // start grpc
      server =
          ServerBuilder.forPort(PathStoreProperties.getInstance().GRPCPort)
              .addService(new CommonServiceImpl())
              .addService(new ClientOnlyServiceImpl())
              .addService(new ServerOnlyServiceImpl())
              .addService(new NetworkWideServiceImpl())
              .addService(new UnAuthenticatedServiceImpl())
              .intercept(new AuthServerInterceptor(new AuthManager()))
              .build();

      server.start();

      logger.info(
          String.format("Started GRPC on port %d", PathStoreProperties.getInstance().GRPCPort));

      PathStoreDeploymentUtils.writeTaskDone(local, 0);

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

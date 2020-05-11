package pathstore.system;

import pathstore.common.PathStoreProperties;
import pathstore.common.Role;
import pathstore.system.deployment.deploymentFSM.PathStoreMasterDeploymentServer;
import pathstore.system.deployment.deploymentFSM.PathStoreSlaveDeploymentServer;
import pathstore.system.logging.PathStoreLoggerDaemon;
import pathstore.system.schemaFSM.PathStoreMasterSchemaServer;
import pathstore.system.schemaFSM.PathStoreSlaveSchemaServer;

/**
 * This class is used to manage all daemons that are spawned after initial setup.
 *
 * <p>These daemons represent all pathstore based features and functionality
 */
public class DaemonManager {
  private PathStoreMasterSchemaServer masterSchemaServer = null;
  private PathStoreSlaveSchemaServer slaveSchemaServer;
  private PathStoreMasterDeploymentServer masterDeploymentServer = null;
  private PathStoreSlaveDeploymentServer slaveDeploymentServer;
  private PathStorePullServer pullServer = null;
  private PathStorePushServer pushServer = null;
  private PathStoreLoggerDaemon loggerDaemon = null;

  /**
   * All Nodes:
   *
   * <p>loggerDaemon
   *
   * <p>slaveSchemaServer
   *
   * <p>slaveDeploymentServer
   *
   * <p>Server:
   *
   * <p>pullServer
   *
   * <p>pushServer
   *
   * <p>Root Server:
   *
   * <p>masterSchemaServer
   *
   * <p>masterDeploymentServer
   */
  public DaemonManager() {
    this.loggerDaemon = new PathStoreLoggerDaemon();
    this.slaveSchemaServer = new PathStoreSlaveSchemaServer();
    this.slaveDeploymentServer = new PathStoreSlaveDeploymentServer();
    if (PathStoreProperties.getInstance().role != Role.ROOTSERVER) {
      this.pullServer = new PathStorePullServer();
      this.pushServer = new PathStorePushServer();
    } else {
      this.masterSchemaServer = new PathStoreMasterSchemaServer();
      this.masterDeploymentServer = new PathStoreMasterDeploymentServer();
    }
  }

  /** Starts all daemons based on role */
  void startDaemons() {
    try {
      this.loggerDaemon.start();
      this.slaveSchemaServer.start();
      this.slaveDeploymentServer.start();
      if (PathStoreProperties.getInstance().role == Role.ROOTSERVER) {
        this.masterSchemaServer.start();
        this.masterDeploymentServer.start();
        this.masterDeploymentServer.join();
      } else {
        this.pushServer.start();
        this.pullServer.start();
        this.pullServer.join();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}

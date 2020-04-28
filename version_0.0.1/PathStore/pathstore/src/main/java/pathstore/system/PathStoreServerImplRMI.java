package pathstore.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCache;
import pathstore.common.Role;
import pathstore.exception.PathMigrateAlreadyGoneException;
import pathstore.system.deployment.deploymentFSM.PathStoreMasterDeploymentServer;
import pathstore.system.deployment.deploymentFSM.PathStoreSlaveDeploymentServer;
import pathstore.system.schemaFSM.PathStoreMasterSchemaServer;
import pathstore.system.schemaFSM.PathStoreSlaveSchemaServer;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.UUID;

/** TODO: Comment */
public class PathStoreServerImplRMI implements PathStoreServer {
  private final Logger logger = LoggerFactory.getLogger(PathStoreServerImplRMI.class);

  private PathStoreMasterSchemaServer masterSchemaServer = null;
  private PathStoreSlaveSchemaServer slaveSchemaServer;
  private PathStoreMasterDeploymentServer masterDeploymentServer = null;
  private PathStoreSlaveDeploymentServer slaveDeploymentServer;
  private PathStorePullServer pullServer = null;
  private PathStorePushServer pushServer = null;

  public PathStoreServerImplRMI() {
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



  void startDaemons() {
    try {
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

  @Override // child calls this (maybe client or child node)
  public String addUserCommandEntry(
      String user, String keyspace, String table, byte[] clauses, int limit)
      throws RemoteException, PathMigrateAlreadyGoneException {

    // System.out.println("In addUserCommandEntry " + user + ":"+ keyspace + ":" + table + " " +
    // clauses);

    try {
      QueryCache.getInstance().updateDeviceCommandCache(user, keyspace, table, clauses, limit);
    } catch (Exception e) {
      if (e instanceof PathMigrateAlreadyGoneException)
        throw new RemoteException("PathMigrateAlreadyGoneException");
      else throw new RemoteException(e.getMessage());
    }

    return "server says hello! in user command entry";
  }

  public String addQueryEntry(String keyspace, String table, byte[] clauses, int limit)
      throws RemoteException {
    logger.info("In addQueryEntry " + keyspace + ":" + table + " " + clauses);

    long d = System.nanoTime();
    try {
      QueryCache.getInstance().updateCache(keyspace, table, clauses, limit);
      //			System.out.println("^^^^^^^^^^^^^^^^ time to reply took: " + Timer.getTime(d));

    } catch (ClassNotFoundException | IOException e) {
      throw new RemoteException(e.getMessage());
    }

    return "server says hello!";
  }

  @Override
  public UUID createQueryDelta(
      String keyspace, String table, byte[] clauses, UUID parentTimestamp, int nodeID, int limit)
      throws RemoteException {
    logger.info(
        "In createQueryDelta "
            + keyspace
            + ":"
            + table
            + " "
            + clauses
            + " pts:"
            + parentTimestamp.timestamp()
            + " "
            + nodeID);

    try {
      return QueryCache.getInstance()
          .createDelta(keyspace, table, clauses, parentTimestamp, nodeID, limit);
    } catch (ClassNotFoundException | IOException e) {
      throw new RemoteException(e.getMessage());
    }
  }
}

package pathstore.system;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pathstore.client.PathStoreServerClient;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCache;
import pathstore.common.Role;
import pathstore.exception.PathMigrateAlreadyGoneException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.UUID;

public class PathStoreServerImplRMI implements PathStoreServer {
  private final Logger logger = LoggerFactory.getLogger(PathStoreServerImplRMI.class);

  private PathStoreSchemaLoader schemaLoader = null;
  private PathStorePullServer pullServer = null;
  private PathStorePushServer pushServer = null;

  public PathStoreServerImplRMI() {
    if (PathStoreProperties.getInstance().role != Role.ROOTSERVER) {
      this.schemaLoader =
          new PathStoreSchemaLoader(
              (nodeid, current_values) -> {
                try {
                  this.getNodeSchemas(nodeid, current_values);
                } catch (RemoteException e) {
                  e.printStackTrace();
                }
              },
              (keyspace) -> {
                try {
                  this.getSchema(keyspace);
                } catch (RemoteException e) {
                  e.printStackTrace();
                }
              });
      this.pullServer = new PathStorePullServer();
      this.pushServer = new PathStorePushServer();
    }
  }

  void startDaemons() {
    if (PathStoreProperties.getInstance().role != Role.ROOTSERVER)
      try {
        this.schemaLoader.start();
        this.pushServer.start();
        this.pullServer.start();
        this.pullServer.join();
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

  /**
   * Calls to parent only occur when the server is of the SERVER role. As ROOTSERVER has no parent
   * after the call is made we can assume that the parent has data we are looking for. We then query
   * the data that is relevant to us from the parent and then insert said data into our local
   * database. We also update current_values to include the keyspace we inserted.
   *
   * @param node_id node_id to query if there is available schemas
   * @param current_values set of schemas that need to be loaded. This is specific to the calling
   *     server.
   * @throws RemoteException JAVA RMI Requirement
   */
  @Override
  public void getNodeSchemas(final Integer node_id, final Set<String> current_values)
      throws RemoteException {
    if (PathStoreProperties.getInstance().role != Role.ROOTSERVER) {
      PathStoreServerClient.getInstance().getNodeSchemas(node_id, current_values);

      Session parent = PathStoreParentCluster.getInstance().connect();
      Session local = PathStorePriviledgedCluster.getInstance().connect();

      ResultSet set =
          parent.execute(
              QueryBuilder.select()
                  .all()
                  .from("pathstore_applications", "node_schemas")
                  .where(QueryBuilder.eq("nodeid", node_id)));

      for (Row row : set) {
        String keyspace = row.getString("keyspace_name");
        if (!current_values.contains(keyspace)) {
          local.execute(
              QueryBuilder.insertInto("pathstore_applications", "node_schemas")
                  .value("nodeid", node_id)
                  .value("keyspace_name", keyspace)
                  .value("pathstore_version", row.getUUID("pathstore_version"))
                  .value("pathstore_parent_timestamp", row.getUUID("pathstore_parent_timestamp")));
          current_values.add(keyspace);
        }
      }
    }
  }

  /**
   * This class only calls the parent class if the server has the role SERVER. We first check if we
   * currently have the schema we are looking for in our local database. If we do, do nothing. Else
   * we query the parent instance to see if they have the data (This can go as far as the root
   * node). We then query the schema based on keyspace name and insert it into our local database.
   * The schema gets initialized inside {@link PathStoreSchemaLoader}
   *
   * @param keyspace keyspace to query
   * @see pathstore.system.PathStoreSchemaLoader
   * @throws RemoteException JAVA RMI Requirement
   */
  @Override
  public void getSchema(final String keyspace) throws RemoteException {
    if (PathStoreProperties.getInstance().role != Role.ROOTSERVER) {

      if (this.schemaLoader.getAvailableSchemas().containsKey(keyspace)) return;

      PathStoreServerClient.getInstance().getSchema(keyspace);

      Session parent = PathStoreParentCluster.getInstance().connect();
      Session local = PathStorePriviledgedCluster.getInstance().connect();

      ResultSet set =
          parent.execute(
              QueryBuilder.select()
                  .all()
                  .from("pathstore_applications", "apps")
                  .where(QueryBuilder.eq("keyspace_name", keyspace)));

      for (Row row : set) {
        String schema = row.getString("augmented_schema");
        local.execute(
            QueryBuilder.insertInto("pathstore_applications", "apps")
                .value("appid", row.getInt("appid"))
                .value("keyspace_name", keyspace)
                .value("augmented_schema", schema)
                .value("pathstore_version", row.getUUID("pathstore_version"))
                .value("pathstore_parent_timestamp", row.getUUID("pathstore_parent_timestamp")));
        this.schemaLoader.getAvailableSchemas().put(keyspace, schema);
      }
    }
  }
}

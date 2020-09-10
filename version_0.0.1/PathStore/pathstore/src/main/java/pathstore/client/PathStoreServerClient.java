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

import com.datastax.driver.core.Statement;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCacheEntry;
import pathstore.common.Role;
import pathstore.exception.PathStoreRemoteException;
import pathstore.sessions.SessionToken;
import pathstore.system.PathStoreServerImplRMI;
import pathstore.util.SchemaInfo;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Optional;
import java.util.UUID;

/**
 * This class is used in three different instances. One where the client communicates with its local
 * node, two when a node communicates with its parent and three when a node needs to communicate
 * with another node in the network for Session Consistency.
 *
 * @see PathStoreServer for the definition of communication options
 * @implNote This will be migrated to GRPC in the future.
 */
public class PathStoreServerClient {

  /** Instance of class, only one per runtime */
  private static PathStoreServerClient instance = null;

  /** @return instance of server client. Either to local node or to parent */
  public static synchronized PathStoreServerClient getInstance() {
    if (PathStoreServerClient.instance == null)
      PathStoreServerClient.instance = new PathStoreServerClient();
    return PathStoreServerClient.instance;
  }

  /**
   * Custom Server client, this is used during session migration
   *
   * @param ip ip of server to connect to
   * @param port port of server to connect on
   * @return new instance if valid
   */
  public static PathStoreServerClient getCustom(final String ip, final int port) {
    return new PathStoreServerClient(ip, port);
  }

  /** Stub to execute on */
  private final PathStoreServer stub;

  /**
   * Constructor for {@link #getCustom(String, int)}
   *
   * @param ip ip to connect to
   * @param port port to connect on
   */
  public PathStoreServerClient(final String ip, final int port) {
    try {
      Registry registry = LocateRegistry.getRegistry(ip, port);
      this.stub = (PathStoreServer) registry.lookup("PathStoreServer");
    } catch (Exception e) {
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  /**
   * Creates connection to local node or to parent depending on role. Errors will be throw if any
   * information required is missing or if the connection cannot be created as the information that
   * was provided is pointing to an invalid source
   */
  public PathStoreServerClient() {
    try {
      Registry registry;

      if (PathStoreProperties.getInstance().role == Role.SERVER) {
        if (PathStoreProperties.getInstance().RMIRegistryParentIP == null)
          throw new RuntimeException(
              "Could not connect to parent registry as the ip specified is null");
        if (PathStoreProperties.getInstance().RMIRegistryParentPort == -1)
          throw new RuntimeException("Could not connect to parent registry as the port is -1");

        registry =
            LocateRegistry.getRegistry(
                PathStoreProperties.getInstance().RMIRegistryParentIP,
                PathStoreProperties.getInstance().RMIRegistryParentPort);
      } else {
        if (PathStoreProperties.getInstance().RMIRegistryIP == null)
          throw new RuntimeException(
              "Could not connect to local node registry as the ip specified is null");
        if (PathStoreProperties.getInstance().RMIRegistryPort == -1)
          throw new RuntimeException("Could not connect to local node registry as the port is -1");

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

  /**
   * This function is used to call {@link pathstore.common.QueryCache#updateCache(String, String,
   * byte[], int)} on the parent node or local node
   *
   * @param entry entry pass to parent or local node
   * @see pathstore.system.PathStoreServerImplRMI#updateCache(String, String, byte[], int)
   */
  public void updateCache(final QueryCacheEntry entry) {
    try {
      stub.updateCache(entry.keyspace, entry.table, entry.getClausesSerialized(), entry.limit);
    } catch (Exception e) {
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  /**
   * This function is used to create a delta for a given qc entry. This will call {@link
   * pathstore.common.QueryCache#createDelta(String, String, byte[], UUID, int, int)} on the parent
   * node.
   *
   * <p>This is only used for server -> server communication.
   *
   * @param entry entry to create delta for.
   * @return delta UUID if new rows exist, else null.
   * @see pathstore.system.PathStoreServerImplRMI#createQueryDelta(String, String, byte[], UUID,
   *     int, int)
   */
  public UUID createQueryDelta(final QueryCacheEntry entry) {
    try {
      return stub.createQueryDelta(
          entry.keyspace,
          entry.table,
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

  /**
   * This function is used to get a partition of the schema info from the local node.
   *
   * <p>This is only used one in {@link PathStoreClientAuthenticatedCluster#getInstance()} on
   * initial creation
   *
   * @param keyspace keyspace to partition by
   * @return partitioned schema info
   * @see SchemaInfo#getSchemaPartition(String)
   * @see pathstore.system.PathStoreServerImplRMI#getSchemaInfo(String)
   */
  public SchemaInfo getSchemaInfo(final String keyspace) {
    try {
      return this.stub.getSchemaInfo(keyspace);
    } catch (RemoteException e) {
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  /**
   * This function is used to register an application client. You must pass the application name and
   * the master password.
   *
   * @param applicationName application name
   * @param password master password for that application
   * @return response string
   * @see pathstore.system.PathStoreServerImplRMI#registerApplicationClient(String, String)
   */
  public Optional<String> registerApplicationClient(
      final String applicationName, final String password) {
    try {
      return Optional.ofNullable(this.stub.registerApplicationClient(applicationName, password));
    } catch (Exception e) {
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  /**
   * This function is used during session migration and is used to validate a session.
   *
   * <p>The caller for this function is {@link PathStoreSession#execute(Statement, SessionToken)}
   *
   * <p>This will only be called when a client has a session, has re-spawned and has loaded in
   * session from their session file. The cases are that the session to validate was generated on
   * their local node in which only data validation occurs, and the other is when the session was
   * produced on another node, this is when migration occurs.
   *
   * @param sessionToken session token to validate
   * @return true if validated else false
   * @see pathstore.system.PathStoreServerImplRMI#validateSession(SessionToken)
   */
  public boolean validateSession(final SessionToken sessionToken) {
    try {
      return this.stub.validateSession(sessionToken);
    } catch (RemoteException e) {
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  /**
   * This function is used to force push all data encompassed within a session token to some node
   * denoted as lca (Lowest common ancestor)
   *
   * @param sessionToken session token to get what data needs to pushed
   * @param lca where to push to.
   * @see pathstore.system.PathStoreServerImplRMI#forcePush(SessionToken, int)
   */
  public void forcePush(final SessionToken sessionToken, final int lca) {
    try {
      this.stub.forcePush(sessionToken, lca);
    } catch (RemoteException e) {
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  /**
   * This function is used to force pull all local qc entries along a path from n_d -> n_a that are
   * encompassed within a given session token.
   *
   * <p>This function is what ensures session consistency for a node.
   *
   * @param sessionToken session token to pull on
   * @param lca where to pull from.
   * @see pathstore.system.PathStoreServerImplRMI#forceSynchronize(SessionToken, int)
   * @implNote Ensuring session consistency is not synonymous with session consistency + data
   *     locality. When you migrate your session from n_s -> n_d we ensure that the data you can
   *     read is the same but we do not guarantee that all session related data is present in n_d.
   *     It very well may occur that n_d has all session data locally available after a force
   *     synchronization but this is dependent on what n_d's local clients are interested in. As if
   *     they're interested in all data associated with your session then it will be locally
   *     present, else you will need to move up the path to gain said session related data. For info
   *     see the complete document on github.
   */
  public void forceSynchronize(final SessionToken sessionToken, final int lca) {
    try {
      this.stub.forceSynchronize(sessionToken, lca);
    } catch (RemoteException e) {
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }

  /**
   * This function is used for all clients to retrieve the node id of its local client on startup.
   * As we cannot trust the node_id provided by the client at startup as this is used to make
   * decisions related to session consistency.
   *
   * @return local node id
   * @see PathStoreServerImplRMI#getLocalNodeId()
   */
  public int getLocalNodeId() {
    try {
      return this.stub.getLocalNodeId();
    } catch (RemoteException e) {
      e.printStackTrace();
      throw new PathStoreRemoteException();
    }
  }
}

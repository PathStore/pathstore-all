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
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import lombok.NonNull;
import pathstore.authentication.CredentialCache;
import pathstore.authentication.grpc.PathStoreClientInterceptor;
import pathstore.authentication.grpc.PathStoreServerInterceptor;
import pathstore.common.PathStoreProperties;
import pathstore.common.QueryCacheEntry;
import pathstore.common.Role;
import pathstore.grpc.*;
import pathstore.grpc.pathStoreProto.*;
import pathstore.sessions.SessionToken;
import pathstore.system.network.NetworkImpl;
import pathstore.system.network.NetworkUtil;
import pathstore.util.Pair;
import pathstore.util.SchemaInfo;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * This class is used in three different instances. One where the client communicates with its local
 * node, two when a node communicates with its parent and three when a node needs to communicate
 * with another node in the network for Session Consistency.
 *
 * @see NetworkImpl
 * @implNote This will be migrated to GRPC in the future.
 */
public class PathStoreServerClient {

  /** Instance of class, only one per runtime */
  @Getter(lazy = true)
  private static final PathStoreServerClient instance = new PathStoreServerClient();

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

  /**
   * Channel for connection. Used to shutdown
   *
   * <p>TODO: Call shutdown in {@link PathStoreCluster} and {@link
   * PathStoreClientAuthenticatedCluster}
   *
   * @see #shutdown()
   */
  private final ManagedChannel channel;

  /** Stub for {@link pathstore.system.network.CommonServiceImpl} */
  private final CommonServiceGrpc.CommonServiceBlockingStub commonServiceBlockingStub;

  /** Stub for {@link pathstore.system.network.ClientOnlyServiceImpl} */
  private final ClientOnlyServiceGrpc.ClientOnlyServiceBlockingStub clientOnlyServiceBlockingStub;

  /** Stub for {@link pathstore.system.network.ServerOnlyServiceImpl} */
  private final ServerOnlyServiceGrpc.ServerOnlyServiceBlockingStub serverOnlyServiceBlockingStub;

  /** Stub for {@link pathstore.system.network.NetworkWideServiceImpl} */
  private final NetworkWideServiceGrpc.NetworkWideServiceBlockingStub
      networkWideServiceBlockingStub;

  /** Stub for {@link pathstore.system.network.UnAuthenticatedServiceImpl} */
  private final UnAuthenticatedServiceGrpc.UnAuthenticatedServiceBlockingStub
      unAuthenticatedServiceBlockingStub;

  /**
   * Creates connection to local node or to parent depending on role. Errors will be throw if any
   * information required is missing or if the connection cannot be created as the information that
   * was provided is pointing to an invalid source
   */
  private PathStoreServerClient() {
    this(
        PathStoreProperties.getInstance().role == Role.SERVER
            ? PathStoreProperties.getInstance().GRPCParentIP
            : PathStoreProperties.getInstance().GRPCIP,
        PathStoreProperties.getInstance().role == Role.SERVER
            ? PathStoreProperties.getInstance().GRPCParentPort
            : PathStoreProperties.getInstance().GRPCPort);
  }

  /**
   * Shutdown function to close grpc connection to local node / parent
   *
   * @throws InterruptedException if shutdown cannot be completed
   */
  public void shutdown() throws InterruptedException {
    this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * Constructor for {@link #getCustom(String, int)}
   *
   * <p>If the role is a client then create an auth client interceptor instance without an account.
   * As this will get called to attempt to authenticate before proper credentials exist.
   *
   * @param ip ip to connect to
   * @param port port to connect on
   */
  private PathStoreServerClient(@NonNull final String ip, @NonNull final int port) {
    this(
        ManagedChannelBuilder.forAddress(ip, port)
            .intercept(
                PathStoreProperties.getInstance().role == Role.SERVER
                    ? PathStoreServerInterceptor.getInstance(
                        CredentialCache.getNodeAuth()
                            .getCredential(PathStoreProperties.getInstance().ParentID))
                    : PathStoreClientInterceptor.getInstance())
            .usePlaintext(true));
  }

  /** @param channelBuilder channel build based on ip and port */
  private PathStoreServerClient(final ManagedChannelBuilder<?> channelBuilder) {
    this.channel = channelBuilder.build();
    this.commonServiceBlockingStub = CommonServiceGrpc.newBlockingStub(channel);
    this.clientOnlyServiceBlockingStub = ClientOnlyServiceGrpc.newBlockingStub(channel);
    this.serverOnlyServiceBlockingStub = ServerOnlyServiceGrpc.newBlockingStub(channel);
    this.networkWideServiceBlockingStub = NetworkWideServiceGrpc.newBlockingStub(channel);
    this.unAuthenticatedServiceBlockingStub = UnAuthenticatedServiceGrpc.newBlockingStub(channel);
  }

  /**
   * This function is used to call {@link pathstore.common.QueryCache#updateCache(String, String,
   * byte[], int)} on the parent node or local node
   *
   * @param entry entry pass to parent or local node
   * @see NetworkImpl#updateCache(String, String, byte[], int)
   */
  public void updateCache(final QueryCacheEntry entry) {
    try {
      QueryEntry queryEntry =
          QueryEntry.newBuilder()
              .setKeyspace(entry.keyspace)
              .setTable(entry.table)
              .setClauses(ByteString.copyFrom(entry.getClausesSerialized()))
              .setLimit(entry.limit)
              .build();

      this.commonServiceBlockingStub.updateCache(queryEntry);
    } catch (IOException e) {
      throw new RuntimeException(e);
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
   * @see pathstore.system.network.NetworkImpl#createQueryDelta(String, String, byte[], UUID, int,
   *     int)
   */
  public UUID createQueryDelta(final QueryCacheEntry entry) {
    try {
      QueryDeltaEntry queryDeltaEntry =
          QueryDeltaEntry.newBuilder()
              .setKeyspace(entry.keyspace)
              .setTable(entry.table)
              .setClauses(ByteString.copyFrom(entry.getClausesSerialized()))
              .setParentTimestamp(entry.getParentTimeStamp().toString())
              .setNodeID(PathStoreProperties.getInstance().NodeID)
              .setLimit(entry.limit)
              .build();

      String response =
          this.serverOnlyServiceBlockingStub.createQueryDelta(queryDeltaEntry).getUuid();

      return response != null && response.length() > 0 ? UUID.fromString(response) : null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This function is used to register an application client. You must pass the application name and
   * the master password.
   *
   * @param applicationName application name
   * @param password master password for that application
   * @return response string
   * @see pathstore.system.network.NetworkImpl#registerApplicationClient(String, String)
   */
  public Pair<Optional<String>, Optional<SchemaInfo>> registerApplicationClient(
      final String applicationName, final String password) {
    RegisterApplicationRequest registerApplicationRequest =
        RegisterApplicationRequest.newBuilder()
            .setApplicationName(applicationName)
            .setPassword(password)
            .build();

    RegisterApplicationResponse response =
        this.unAuthenticatedServiceBlockingStub.registerApplicationClient(
            registerApplicationRequest);

    return new Pair<>(
        Optional.ofNullable(response.getCredentials()),
        Optional.ofNullable((SchemaInfo) NetworkUtil.readObject(response.getSchemaInfo())));
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
   * @see pathstore.system.network.NetworkImpl#validateSession(SessionToken)
   */
  public boolean validateSession(final SessionToken sessionToken) {
    ValidateSessionRequest validateSessionRequest =
        ValidateSessionRequest.newBuilder()
            .setSessionToken(NetworkUtil.writeObject(sessionToken))
            .build();

    return this.clientOnlyServiceBlockingStub.validateSession(validateSessionRequest).getResponse();
  }

  /**
   * This function is used to force push all data encompassed within a session token to some node
   * denoted as lca (Lowest common ancestor)
   *
   * @param sessionToken session token to get what data needs to pushed
   * @param lca where to push to.
   * @see pathstore.system.network.NetworkImpl#forcePush(SessionToken, int)
   */
  public void forcePush(final SessionToken sessionToken, final int lca) {
    ForcePushRequest forcePushRequest =
        ForcePushRequest.newBuilder()
            .setSessionToken(NetworkUtil.writeObject(sessionToken))
            .setLca(lca)
            .build();

    this.networkWideServiceBlockingStub.forcePush(forcePushRequest);
  }

  /**
   * This function is used to force pull all local qc entries along a path from n_d -> n_a that are
   * encompassed within a given session token.
   *
   * <p>This function is what ensures session consistency for a node.
   *
   * @param sessionToken session token to pull on
   * @param lca where to pull from.
   * @see pathstore.system.network.NetworkImpl#forceSynchronize(SessionToken, int)
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
    ForceSynchronizationRequest forceSynchronizationRequest =
        ForceSynchronizationRequest.newBuilder()
            .setSessionToken(NetworkUtil.writeObject(sessionToken))
            .setLca(lca)
            .build();

    this.serverOnlyServiceBlockingStub.forceSynchronize(forceSynchronizationRequest);
  }

  /**
   * This function is used for all clients to retrieve the node id of its local client on startup.
   * As we cannot trust the node_id provided by the client at startup as this is used to make
   * decisions related to session consistency.
   *
   * @return local node id
   * @see NetworkImpl#getLocalNodeId()
   */
  public int getLocalNodeId() {
    return this.clientOnlyServiceBlockingStub.getLocalNodeId(Empty.newBuilder().build()).getNode();
  }
}

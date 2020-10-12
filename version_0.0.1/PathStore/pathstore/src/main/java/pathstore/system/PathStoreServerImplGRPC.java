package pathstore.system;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import pathstore.sessions.SessionToken;
import pathstore.system.network.NetworkImpl;
import pathstore.grpc.*;
import pathstore.system.network.NetworkUtil;
import pathstore.util.SchemaInfo;

import java.util.UUID;

/**
 * This class is used to provide the service between a PathStore Client and a local node or a
 * PathStore Server and its parent node. This is a transport layer and does not include and logic
 * for how each request gets handle. Just a input parser and output interpreter
 *
 * @see pathstore.client.PathStoreSession
 * @see pathstore.client.PathStoreServerClient
 * @see NetworkImpl
 */
public class PathStoreServerImplGRPC extends PathStoreServiceGrpc.PathStoreServiceImplBase {

  /** Network Impl, this is the logic for each class */
  private NetworkImpl network;

  /** Inits NetworkImpl */
  PathStoreServerImplGRPC() {
    this.network = new NetworkImpl();
  }

  /**
   * Updates local node / parents cache
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#updateCache(String, String, byte[], int)
   */
  @Override
  public void updateCache(
      pathStoreProto.QueryEntry request,
      StreamObserver<pathStoreProto.InfoFromServer> responseObserver) {

    String keyspace = request.getKeyspace();
    String table = request.getTable();
    byte[] clauses = request.getClauses().toByteArray();
    int limit = request.getLimit();

    String response = this.network.updateCache(keyspace, table, clauses, limit);

    responseObserver.onNext(pathStoreProto.InfoFromServer.newBuilder().setInfo(response).build());
    responseObserver.onCompleted();
  }

  /**
   * creates delta for a query on parent node
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#createQueryDelta(String, String, byte[], UUID, int, int)
   */
  @Override
  public void createQueryDelta(
      pathStoreProto.QueryDeltaEntry request,
      StreamObserver<pathStoreProto.UUIDInfo> responseObserver) {
    String keyspace = request.getKeyspace();
    String table = request.getTable();
    byte[] clauses = request.getClauses().toByteArray();
    UUID parentTimestamp = UUID.fromString(request.getParentTimestamp());
    int nodeId = request.getNodeID();
    int limit = request.getLimit();

    UUID response =
        this.network.createQueryDelta(keyspace, table, clauses, parentTimestamp, nodeId, limit);

    pathStoreProto.UUIDInfo.Builder uuidInfo = pathStoreProto.UUIDInfo.newBuilder();

    if (response != null) uuidInfo.setUuid(response.toString());

    responseObserver.onNext(uuidInfo.build());
    responseObserver.onCompleted();
  }

  /**
   * Register a client account for an application on the local node
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#registerApplicationClient(String, String)
   */
  @Override
  public void registerApplicationClient(
      pathStoreProto.RegisterApplicationRequest request,
      StreamObserver<pathStoreProto.RegisterApplicationResponse> responseObserver) {
    String applicationName = request.getApplicationName();
    String password = request.getPassword();

    String response = this.network.registerApplicationClient(applicationName, password);

    responseObserver.onNext(
        pathStoreProto.RegisterApplicationResponse.newBuilder().setResponse(response).build());
    responseObserver.onCompleted();
  }

  /**
   * Get schema info for client from local node after registration of application account
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#getSchemaInfo(String)
   */
  @Override
  public void getSchemaInfo(
      pathStoreProto.SchemaInfoRequest request,
      StreamObserver<pathStoreProto.SchemaInfoResponse> responseObserver) {
    String keyspace = request.getKeyspace();

    SchemaInfo response = this.network.getSchemaInfo(keyspace);

    responseObserver.onNext(
        pathStoreProto
            .SchemaInfoResponse
            .newBuilder()
            .setResponse(NetworkUtil.writeObject(response))
            .build());
    responseObserver.onCompleted();
  }

    /**
     * Validate session from client to local node
     *
     * @param request request send
     * @param responseObserver way to response
     * @see NetworkImpl#validateSession(SessionToken) 
     */
  @Override
  public void validateSession(
      pathStoreProto.ValidateSessionRequest request,
      StreamObserver<pathStoreProto.ValidateSessionResponse> responseObserver) {

    SessionToken sessionToken =
        (SessionToken) NetworkUtil.readObject(request.getSessionToken().toByteArray());

    boolean response = this.network.validateSession(sessionToken);

    responseObserver.onNext(
        pathStoreProto.ValidateSessionResponse.newBuilder().setResponse(response).build());
    responseObserver.onCompleted();
  }

    /**
     * Force push data from source to lca
     *
     * @param request request send
     * @param responseObserver way to response
     * @see NetworkImpl#forcePush(SessionToken, int) 
     */
  @Override
  public void forcePush(
      pathStoreProto.ForcePushRequest request, StreamObserver<Empty> responseObserver) {
    SessionToken sessionToken =
        (SessionToken) NetworkUtil.readObject(request.getSessionToken().toByteArray());
    int lca = request.getLca();

    this.network.forcePush(sessionToken, lca);

    responseObserver.onNext(Empty.newBuilder().build());
    responseObserver.onCompleted();
  }

    /**
     * Force sync caches from destination to lca
     *
     * @param request request send
     * @param responseObserver way to response
     * @see NetworkImpl#forceSynchronize(SessionToken, int) 
     */
  @Override
  public void forceSynchronize(
      pathStoreProto.ForceSynchronizationRequest request, StreamObserver<Empty> responseObserver) {
    SessionToken sessionToken =
        (SessionToken) NetworkUtil.readObject(request.getSessionToken().toByteArray());
    int lca = request.getLca();

    this.network.forceSynchronize(sessionToken, lca);

    responseObserver.onNext(Empty.newBuilder().build());
    responseObserver.onCompleted();
  }

    /**
     * Get local node id for client side
     *
     * @param request request send
     * @param responseObserver way to response
     * @see NetworkImpl#getLocalNodeId() 
     */
  @Override
  public void getLocalNodeId(
      Empty request, StreamObserver<pathStoreProto.GetLocalNodeResponse> responseObserver) {
    int nodeId = this.network.getLocalNodeId();

    responseObserver.onNext(
        pathStoreProto.GetLocalNodeResponse.newBuilder().setNode(nodeId).build());
    responseObserver.onCompleted();
  }
}

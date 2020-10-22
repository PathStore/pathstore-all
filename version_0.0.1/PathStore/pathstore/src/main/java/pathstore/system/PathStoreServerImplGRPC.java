package pathstore.system;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import pathstore.grpc.PathStoreServiceGrpc;
import pathstore.grpc.pathStoreProto.*;
import pathstore.sessions.SessionToken;
import pathstore.system.network.NetworkImpl;
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
  private final NetworkImpl network;

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
      final QueryEntry request, final StreamObserver<InfoFromServer> responseObserver) {

    String keyspace = request.getKeyspace();
    String table = request.getTable();
    byte[] clauses = request.getClauses().toByteArray();
    int limit = request.getLimit();

    String response = this.network.updateCache(keyspace, table, clauses, limit);

    responseObserver.onNext(InfoFromServer.newBuilder().setInfo(response).build());
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
      final QueryDeltaEntry request, final StreamObserver<UUIDInfo> responseObserver) {
    String keyspace = request.getKeyspace();
    String table = request.getTable();
    byte[] clauses = request.getClauses().toByteArray();
    UUID parentTimestamp = UUID.fromString(request.getParentTimestamp());
    int nodeId = request.getNodeID();
    int limit = request.getLimit();

    UUID response =
        this.network.createQueryDelta(keyspace, table, clauses, parentTimestamp, nodeId, limit);

    UUIDInfo.Builder uuidInfo = UUIDInfo.newBuilder();

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
      final RegisterApplicationRequest request,
      final StreamObserver<RegisterApplicationResponse> responseObserver) {
    String applicationName = request.getApplicationName();
    String password = request.getPassword();

    String credentials = this.network.registerApplicationClient(applicationName, password);

    SchemaInfo schemaInfo = this.network.getSchemaInfo(applicationName);

    responseObserver.onNext(
        RegisterApplicationResponse.newBuilder()
            .setCredentials(credentials)
            .setSchemaInfo(NetworkUtil.writeObject(schemaInfo))
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
      final ValidateSessionRequest request,
      final StreamObserver<ValidateSessionResponse> responseObserver) {

    SessionToken sessionToken = (SessionToken) NetworkUtil.readObject(request.getSessionToken());

    boolean response = this.network.validateSession(sessionToken);

    responseObserver.onNext(ValidateSessionResponse.newBuilder().setResponse(response).build());
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
      final ForcePushRequest request, final StreamObserver<Empty> responseObserver) {
    SessionToken sessionToken = (SessionToken) NetworkUtil.readObject(request.getSessionToken());
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
      final ForceSynchronizationRequest request, final StreamObserver<Empty> responseObserver) {
    SessionToken sessionToken = (SessionToken) NetworkUtil.readObject(request.getSessionToken());
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
      final Empty request, final StreamObserver<GetLocalNodeResponse> responseObserver) {
    int nodeId = this.network.getLocalNodeId();

    responseObserver.onNext(GetLocalNodeResponse.newBuilder().setNode(nodeId).build());
    responseObserver.onCompleted();
  }
}

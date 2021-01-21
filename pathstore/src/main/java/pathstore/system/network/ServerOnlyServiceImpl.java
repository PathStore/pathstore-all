package pathstore.system.network;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import pathstore.grpc.ServerOnlyServiceGrpc;
import pathstore.grpc.pathStoreProto;
import pathstore.sessions.SessionToken;

import java.util.UUID;

/**
 * This Service impl is specifically for endpoints that can be accessed only be child to parent
 * servers.
 */
public class ServerOnlyServiceImpl extends ServerOnlyServiceGrpc.ServerOnlyServiceImplBase {
  /** Network impl which has logic for each endpoint */
  private final NetworkImpl network = NetworkImpl.getInstance();

  /**
   * creates delta for a query on parent node
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#createQueryDelta(String, String, byte[], UUID, int, int)
   */
  @Override
  public void createQueryDelta(
      final pathStoreProto.QueryDeltaEntry request,
      final StreamObserver<pathStoreProto.UUIDInfo> responseObserver) {
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
   * Force sync caches from destination to lca
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#forceSynchronize(SessionToken, int)
   */
  @Override
  public void forceSynchronize(
      final pathStoreProto.ForceSynchronizationRequest request,
      final StreamObserver<Empty> responseObserver) {
    SessionToken sessionToken = SessionToken.fromGRPCSessionTokenObject(request.getSessionToken());
    int lca = request.getLca();

    this.network.forceSynchronize(sessionToken, lca);

    responseObserver.onNext(Empty.newBuilder().build());
    responseObserver.onCompleted();
  }
}

package pathstore.system.network;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import pathstore.grpc.ClientOnlyServiceGrpc;
import pathstore.grpc.pathStoreProto;
import pathstore.sessions.SessionToken;

/**
 * This Service impl is specifically for endpoints that can be accessed only by a client to a server
 */
public class ClientOnlyServiceImpl extends ClientOnlyServiceGrpc.ClientOnlyServiceImplBase {
  /** Network impl which has logic for each endpoint */
  private final NetworkImpl network = NetworkImpl.getInstance();

  /**
   * Validate session from client to local node
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#validateSession(SessionToken)
   */
  @Override
  public void validateSession(
      final pathStoreProto.ValidateSessionRequest request,
      final StreamObserver<pathStoreProto.ValidateSessionResponse> responseObserver) {

    SessionToken sessionToken = (SessionToken) NetworkUtil.readObject(request.getSessionToken());

    boolean response = this.network.validateSession(sessionToken);

    responseObserver.onNext(
        pathStoreProto.ValidateSessionResponse.newBuilder().setResponse(response).build());
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
      final Empty request,
      final StreamObserver<pathStoreProto.GetLocalNodeResponse> responseObserver) {
    int nodeId = this.network.getLocalNodeId();

    responseObserver.onNext(
        pathStoreProto.GetLocalNodeResponse.newBuilder().setNode(nodeId).build());
    responseObserver.onCompleted();
  }
}

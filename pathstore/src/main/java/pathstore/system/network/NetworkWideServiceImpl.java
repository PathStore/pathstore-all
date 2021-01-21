package pathstore.system.network;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import pathstore.grpc.NetworkWideServiceGrpc;
import pathstore.grpc.pathStoreProto;
import pathstore.sessions.SessionToken;

/**
 * This Service impl is specifically for endpoints that can be accessed on any node from any other
 * node.
 */
public class NetworkWideServiceImpl extends NetworkWideServiceGrpc.NetworkWideServiceImplBase {
  /** Network impl which has logic for each endpoint */
  private final NetworkImpl network = NetworkImpl.getInstance();

  /**
   * Force push data from source to lca
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#forcePush(SessionToken, int)
   */
  @Override
  public void forcePush(
      final pathStoreProto.ForcePushRequest request, final StreamObserver<Empty> responseObserver) {
    SessionToken sessionToken = SessionToken.fromGRPCSessionTokenObject(request.getSessionToken());
    int lca = request.getLca();

    this.network.forcePush(sessionToken, lca);

    responseObserver.onNext(Empty.newBuilder().build());
    responseObserver.onCompleted();
  }
}

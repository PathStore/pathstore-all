package pathstore.system.network;

import io.grpc.stub.StreamObserver;
import pathstore.grpc.CommonServiceGrpc;
import pathstore.grpc.pathStoreProto;

/**
 * This Service impl is for endpoints that can be accessed by both client to server and child to
 * parent server
 */
public class CommonServiceImpl extends CommonServiceGrpc.CommonServiceImplBase {
  /** Network impl which has logic for each endpoint */
  private final NetworkImpl network = NetworkImpl.getInstance();

  /**
   * Updates local node / parents cache
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#updateCache(String, String, byte[], int)
   */
  @Override
  public void updateCache(
      final pathStoreProto.QueryEntry request,
      final StreamObserver<pathStoreProto.InfoFromServer> responseObserver) {

    String keyspace = request.getKeyspace();
    String table = request.getTable();
    byte[] clauses = request.getClauses().toByteArray();
    int limit = request.getLimit();

    String response = this.network.updateCache(keyspace, table, clauses, limit);

    responseObserver.onNext(pathStoreProto.InfoFromServer.newBuilder().setInfo(response).build());
    responseObserver.onCompleted();
  }
}

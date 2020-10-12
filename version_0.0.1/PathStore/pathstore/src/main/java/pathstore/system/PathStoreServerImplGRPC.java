package pathstore.system;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import pathstore.sessions.SessionToken;
import pathstore.system.network.NetworkImpl;
import pathstore.grpc.*;
import pathstore.system.network.NetworkUtil;
import pathstore.util.SchemaInfo;

import java.util.UUID;

// TODO: Comment
public class PathStoreServerImplGRPC extends PathStoreServiceGrpc.PathStoreServiceImplBase {

  private NetworkImpl network;

  public PathStoreServerImplGRPC() {
    this.network = new NetworkImpl();
  }

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

    responseObserver.onNext(
        pathStoreProto
            .UUIDInfo
            .newBuilder()
            .setUuid(response != null ? response.toString() : null)
            .build());
    responseObserver.onCompleted();
  }

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

  @Override
  public void getLocalNodeId(
      Empty request, StreamObserver<pathStoreProto.GetLocalNodeResponse> responseObserver) {
    int nodeId = this.network.getLocalNodeId();

    responseObserver.onNext(
        pathStoreProto.GetLocalNodeResponse.newBuilder().setNode(nodeId).build());
    responseObserver.onCompleted();
  }
}

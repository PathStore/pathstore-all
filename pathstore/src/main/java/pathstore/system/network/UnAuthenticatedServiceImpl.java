package pathstore.system.network;

import io.grpc.stub.StreamObserver;
import pathstore.grpc.UnAuthenticatedServiceGrpc;
import pathstore.grpc.pathStoreProto.RegisterApplicationRequest;
import pathstore.grpc.pathStoreProto.RegisterApplicationResponse;
import pathstore.util.SchemaInfo;

/**
 * This class is used to provide the service between a PathStore Client and a local node or a
 * PathStore Server and its parent node. This is a transport layer and does not include and logic
 * for how each request gets handle. Just a input parser and output interpreter
 *
 * @see pathstore.client.PathStoreSession
 * @see pathstore.client.PathStoreServerClient
 * @see NetworkImpl
 */
public class UnAuthenticatedServiceImpl
    extends UnAuthenticatedServiceGrpc.UnAuthenticatedServiceImplBase {

  /** Network Impl, this is the logic for each class */
  private final NetworkImpl network = NetworkImpl.getInstance();

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

    RegisterApplicationResponse.Builder builder =
        RegisterApplicationResponse.newBuilder().setCredentials(credentials);

    if (schemaInfo != null) builder.setSchemaInfo(schemaInfo.toGRPCSchemaInfoObject());

    RegisterApplicationResponse response = builder.build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}

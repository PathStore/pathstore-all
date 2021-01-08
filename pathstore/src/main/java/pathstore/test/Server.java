package pathstore.test;

import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import pathstore.grpc.UnAuthenticatedServiceGrpc;
import pathstore.grpc.pathStoreProto;

import java.io.IOException;

public class Server {
  public static void main(String[] args) {
    io.grpc.Server server =
        ServerBuilder.forPort(1099)
            .addService(
                new UnAuthenticatedServiceGrpc.UnAuthenticatedServiceImplBase() {
                  @Override
                  public void registerApplicationClient(
                      pathStoreProto.RegisterApplicationRequest request,
                      StreamObserver<pathStoreProto.RegisterApplicationResponse> responseObserver) {
                    String applicationName = request.getApplicationName();
                    String password = request.getPassword();

                    responseObserver.onNext(
                        pathStoreProto.RegisterApplicationResponse.newBuilder().build());
                  }
                }) // nothing
            .build();

    try {
      server.start();
      server.awaitTermination();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}

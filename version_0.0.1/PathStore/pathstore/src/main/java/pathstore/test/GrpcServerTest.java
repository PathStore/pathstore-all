package pathstore.test;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import pathstore.authentication.grpc.AuthManager;
import pathstore.authentication.grpc.AuthServerInterceptor;
import pathstore.grpc.TestServiceGrpc;
import pathstore.grpc.pathStoreProto;

import java.io.IOException;
import java.util.Collection;

public class GrpcServerTest {

  public static void main(String[] args) throws IOException, InterruptedException {
    Server server =
        ServerBuilder.forPort(1099)
            .addService(
                new TestServiceGrpc.TestServiceImplBase() {
                  @Override
                  public void testEndpoint(
                      pathStoreProto.InfoFromServer request,
                      StreamObserver<pathStoreProto.InfoFromServer> responseObserver) {
                    responseObserver.onNext(
                        pathStoreProto
                            .InfoFromServer
                            .newBuilder()
                            .setInfo(String.format("%s-responseFromServer", request.getInfo()))
                            .build());
                    responseObserver.onCompleted();
                  }
                })
            .intercept(new AuthServerInterceptor(new AuthManager()))
            .build();

    server.getServices().stream()
        .map(ServerServiceDefinition::getMethods)
        .flatMap(Collection::stream)
        .forEach(
            serverMethodDefinition ->
                System.out.println(
                    serverMethodDefinition.getMethodDescriptor().getFullMethodName()));

    System.out.println(TestServiceGrpc.getServiceDescriptor().getName());

    server.start();
    System.out.println("Started");
    server.awaitTermination();
  }
}

package pathstore.test;

import io.grpc.ServerBuilder;
import pathstore.authentication.grpc.AuthManager;
import pathstore.authentication.grpc.AuthServerInterceptor;
import pathstore.grpc.UnAuthenticatedServiceGrpc;
import pathstore.system.network.*;

import java.io.IOException;

public class Server {
  public static void main(String[] args) {
    io.grpc.Server server =
        ServerBuilder.forPort(1099)
            .addService(new UnAuthenticatedServiceImpl()) // nothing
            .intercept(
                new AuthServerInterceptor(
                    AuthManager.newBuilder()
                        .unauthenticatedEndpoint(UnAuthenticatedServiceGrpc.SERVICE_NAME)
                        .build()))
            .build();

    try {
      server.start();
      server.awaitTermination();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}

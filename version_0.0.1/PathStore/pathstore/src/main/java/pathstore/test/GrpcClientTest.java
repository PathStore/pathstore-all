package pathstore.test;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pathstore.authentication.grpc.AuthClientInterceptor;
import pathstore.grpc.TestServiceGrpc;
import pathstore.grpc.pathStoreProto;

public class GrpcClientTest {

  public static void main(String[] args) {

    ManagedChannel managedChannel =
        ManagedChannelBuilder.forAddress("127.0.0.1", 1099)
            .intercept(new AuthClientInterceptor())
            .usePlaintext(true)
            .build();

    TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(managedChannel);

    try {
      pathStoreProto.InfoFromServer server =
          stub.testEndpoint(pathStoreProto.InfoFromServer.newBuilder().setInfo("Bladen").build());

      System.out.println(server.getInfo());
    } catch (StatusRuntimeException e) {
      if (e.getStatus() == Status.UNAUTHENTICATED) System.out.println("Un-authenticated");
    } finally {
      managedChannel.shutdownNow();
    }
  }
}

package pathstore.test;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pathstore.authentication.grpc.PathStoreClientInterceptor;
import pathstore.grpc.UnAuthenticatedServiceGrpc;
import pathstore.grpc.pathStoreProto;
import pathstore.system.network.NetworkUtil;

import java.util.List;

public class Client {
  public static void main(String[] args) {
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", 1099)
            .intercept(PathStoreClientInterceptor.getInstance())
            .usePlaintext(true)
            .build();

    UnAuthenticatedServiceGrpc.UnAuthenticatedServiceBlockingStub stub =
        UnAuthenticatedServiceGrpc.newBlockingStub(channel);

    System.out.println("Returned");

    List<Object> objects =
        NetworkUtil.concatenate(
            stub.registerApplicationClient(
                pathStoreProto
                    .RegisterApplicationRequest
                    .newBuilder()
                    .setApplicationName("asd")
                    .setPassword("asd")
                    .build()),
            pathStoreProto.RegisterApplicationResponse::getStatus,
            (pathStoreProto.RegisterApplicationResponse r) -> r.getCredentials().toByteArray(),
            (pathStoreProto.RegisterApplicationResponse r) -> r.getSchemaInfo().toByteArray());

    System.out.println("Concatenated");

    System.out.println(objects.get(0));
    System.out.println(((List<String>) objects.get(1)).size());

    System.out.println("Done");
  }
}

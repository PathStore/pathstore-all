package pathstore.test;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pathstore.grpc.UnAuthenticatedServiceGrpc;
import pathstore.grpc.pathStoreProto;
import pathstore.util.SchemaInfo;

public class Client {
  public static void main(String[] args) {
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", 1099).usePlaintext(true).build();

    UnAuthenticatedServiceGrpc.UnAuthenticatedServiceBlockingStub stub =
        UnAuthenticatedServiceGrpc.newBlockingStub(channel);

    System.out.println("Returned");

    //    Iterator<pathStoreProto.RegisterApplicationResponse> response =
    //        stub.registerApplicationClient(
    //            pathStoreProto
    //                .RegisterApplicationRequest
    //                .newBuilder()
    //                .setApplicationName("asd")
    //                .setPassword("asd")
    //                .build());
    //
    //    System.out.println(response.hasNext());
    //    System.out.println(NetworkUtil.readObject(response.next().getSchemaInfo()));

    pathStoreProto.RegisterApplicationResponse response =
        stub.registerApplicationClient(
            pathStoreProto
                .RegisterApplicationRequest
                .newBuilder()
                .setApplicationName("asd")
                .setPassword("zasd")
                .build());

    System.out.println(response.getCredentials());
    System.out.println(SchemaInfo.fromGRPCObject(response.getSchemaInfo()).getLoadedKeyspaces());

    System.out.println("Done");
  }
}

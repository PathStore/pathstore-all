package pathstore.test;

public class Client {
  public static void main(String[] args) {
    //    ManagedChannel channel =
    //        ManagedChannelBuilder.forAddress("localhost", 1099).usePlaintext(true).build();
    //
    //    UnAuthenticatedServiceGrpc.UnAuthenticatedServiceBlockingStub stub =
    //        UnAuthenticatedServiceGrpc.newBlockingStub(channel);
    //
    //    System.out.println("Returned");
    //
    //    //    Iterator<pathStoreProto.RegisterApplicationResponse> response =
    //    //        stub.registerApplicationClient(
    //    //            pathStoreProto
    //    //                .RegisterApplicationRequest
    //    //                .newBuilder()
    //    //                .setApplicationName("asd")
    //    //                .setPassword("asd")
    //    //                .build());
    //    //
    //    //    System.out.println(response.hasNext());
    //    //    System.out.println(NetworkUtil.readObject(response.next().getSchemaInfo()));
    //
    //    List<Object> objects =
    //        NetworkUtil.concatenate(
    //            stub.registerApplicationClient(
    //                pathStoreProto
    //                    .RegisterApplicationRequest
    //                    .newBuilder()
    //                    .setApplicationName("asd")
    //                    .setPassword("asd")
    //                    .build()),
    //            pathStoreProto.RegisterApplicationResponse::getStatus,
    //            (pathStoreProto.RegisterApplicationResponse r) ->
    // r.getCredentials().toByteArray(),
    //            (pathStoreProto.RegisterApplicationResponse r) ->
    // r.getSchemaInfo().toByteArray());
    //
    //    System.out.println("Concatenated");
    //
    //    System.out.println(objects.get(0));
    //    System.out.println(((List<String>) objects.get(1)).size());
    //
    //    System.out.println("Done");
  }
}

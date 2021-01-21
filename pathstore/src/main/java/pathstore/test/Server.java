package pathstore.test;

public class Server {
  public static void main(String[] args) {
//    io.grpc.Server server =
//        ServerBuilder.forPort(1099)
//            .addService(
//                new UnAuthenticatedServiceGrpc.UnAuthenticatedServiceImplBase() {
//                  @Override
//                  public void registerApplicationClient(
//                      pathStoreProto.RegisterApplicationRequest request,
//                      StreamObserver<pathStoreProto.RegisterApplicationResponse> responseObserver) {
//                    String applicationName = request.getApplicationName();
//                    String password = request.getPassword();
//
//                    Set<String> test = new HashSet<>();
//                    test.add(applicationName);
//                    test.add(password);
//
//                    responseObserver.onNext(
//                        pathStoreProto
//                            .RegisterApplicationResponse
//                            .newBuilder()
//                            .setCredentials(new JSONObject().toString())
//                            .setSchemaInfo(
//                                new SchemaInfo(
//                                        test,
//                                        new ConcurrentHashMap<>(),
//                                        new ConcurrentHashMap<>(),
//                                        new ConcurrentHashMap<>(),
//                                        new ConcurrentHashMap<>(),
//                                        new ConcurrentHashMap<>(),
//                                        new ConcurrentHashMap<>())
//                                    .toGRPCSchemaInfoObject())
//                            .build());
//                    responseObserver.onCompleted();
//                  }
//                }) // nothing
//            .build();
//
//    try {
//      server.start();
//      server.awaitTermination();
//    } catch (IOException | InterruptedException e) {
//      e.printStackTrace();
//    }
  }
}

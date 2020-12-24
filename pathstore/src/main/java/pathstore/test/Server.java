package pathstore.test;

import com.google.protobuf.ByteString;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import pathstore.grpc.UnAuthenticatedServiceGrpc;
import pathstore.grpc.pathStoreProto;
import pathstore.system.network.NetworkUtil;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

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

                    String credentials = "myles";
                    List<String> schemaInfo = new LinkedList<>();
                    for (int i = 0; i < 100000; i++) schemaInfo.add("asd");

                    System.out.println("ABC");

                    try {

                      List<List<ByteString>> objectInChunkWindows =
                          NetworkUtil.objectToByteChunksWindows(credentials, schemaInfo);

                      System.out.println(objectInChunkWindows.size());

                      for (List<ByteString> objectInChunkWindow : objectInChunkWindows) {
                        pathStoreProto.RegisterApplicationResponse.Builder responseBuilder =
                            pathStoreProto.RegisterApplicationResponse.newBuilder();
                        if (objectInChunkWindow.get(0) != null)
                          System.out.println(objectInChunkWindow.get(0).size());

                        responseBuilder.setCredentials(objectInChunkWindow.get(0));
                        if (objectInChunkWindow.get(1) != null)
                          System.out.println(objectInChunkWindow.get(1).size());

                        responseBuilder.setSchemaInfo(objectInChunkWindow.get(1));

                        responseBuilder.setStatus(pathStoreProto.Status.PENDING);
                        responseObserver.onNext(responseBuilder.build());
                        System.out.println("Sent a payload");
                      }

                      // inform client that the call is finished
                      responseObserver.onNext(
                          pathStoreProto
                              .RegisterApplicationResponse
                              .newBuilder()
                              .setStatus(pathStoreProto.Status.DONE)
                              .build());
                      responseObserver.onCompleted();
                      System.out.println("sent on completed");

                    } catch (Exception e) {
                      responseObserver.onError(e);
                    }
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

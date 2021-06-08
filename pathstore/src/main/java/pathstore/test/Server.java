/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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

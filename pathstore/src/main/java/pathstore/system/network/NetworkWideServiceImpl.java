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
package pathstore.system.network;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import pathstore.grpc.NetworkWideServiceGrpc;
import pathstore.grpc.pathStoreProto;
import pathstore.sessions.SessionToken;

/**
 * This Service impl is specifically for endpoints that can be accessed on any node from any other
 * node.
 */
public class NetworkWideServiceImpl extends NetworkWideServiceGrpc.NetworkWideServiceImplBase {
  /** Network impl which has logic for each endpoint */
  private final NetworkImpl network = NetworkImpl.getInstance();

  /**
   * Force push data from source to lca
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#forcePush(SessionToken, int)
   */
  @Override
  public void forcePush(
      final pathStoreProto.ForcePushRequest request, final StreamObserver<Empty> responseObserver) {
    SessionToken sessionToken = SessionToken.fromGRPCSessionTokenObject(request.getSessionToken());
    int lca = request.getLca();

    this.network.forcePush(sessionToken, lca);

    responseObserver.onNext(Empty.newBuilder().build());
    responseObserver.onCompleted();
  }
}

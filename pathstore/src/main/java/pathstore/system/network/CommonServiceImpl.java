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

import io.grpc.stub.StreamObserver;
import pathstore.grpc.CommonServiceGrpc;
import pathstore.grpc.pathStoreProto;

/**
 * This Service impl is for endpoints that can be accessed by both client to server and child to
 * parent server
 */
public class CommonServiceImpl extends CommonServiceGrpc.CommonServiceImplBase {
  /** Network impl which has logic for each endpoint */
  private final NetworkImpl network = NetworkImpl.getInstance();

  /**
   * Updates local node / parents cache
   *
   * @param request request send
   * @param responseObserver way to response
   * @see NetworkImpl#updateCache(String, String, byte[], int)
   */
  @Override
  public void updateCache(
      final pathStoreProto.QueryEntry request,
      final StreamObserver<pathStoreProto.InfoFromServer> responseObserver) {

    String keyspace = request.getKeyspace();
    String table = request.getTable();
    byte[] clauses = request.getClauses().toByteArray();
    int limit = request.getLimit();

    String response = this.network.updateCache(keyspace, table, clauses, limit);

    responseObserver.onNext(pathStoreProto.InfoFromServer.newBuilder().setInfo(response).build());
    responseObserver.onCompleted();
  }
}

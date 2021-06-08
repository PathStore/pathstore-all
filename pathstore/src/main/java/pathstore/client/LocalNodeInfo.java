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
package pathstore.client;

import lombok.Getter;
import pathstore.common.PathStoreProperties;
import pathstore.grpc.pathStoreProto;
import pathstore.util.BlobObject;

/**
 * This class is used for denoting the information that some client will need from the server during
 * the initial handshake
 *
 * <p>Needs to include:
 *
 * <ul>
 *   <li>Node Id
 *   <li>Cassandra IP
 *   <li>Cassandra Port
 * </ul>
 */
public class LocalNodeInfo implements BlobObject {

  /** instance of class */
  @Getter(lazy = true)
  private static final LocalNodeInfo instance =
      new LocalNodeInfo(
          PathStoreProperties.getInstance().NodeID,
          PathStoreProperties.getInstance().CassandraIP.equals("127.0.0.1")
              ? PathStoreProperties.getInstance().ExternalAddress
              : PathStoreProperties.getInstance().CassandraIP,
          PathStoreProperties.getInstance().CassandraPort);

  /** @see PathStoreProperties#NodeID */
  @Getter private final int nodeId;

  /**
   * @see PathStoreProperties#CassandraIP
   * @implNote So currently during orchestration we supply a new child node with the cassandra ip of
   *     127.0.0.1. Eventually this will exist on a separate server. To account for this if the
   *     provided address is 127.0.0.1 we can provide the external address of the machine for the
   *     client otherwise we will provide the cassandra ip directly
   */
  @Getter private final String cassandraIP;

  /** @see PathStoreProperties#CassandraPort */
  @Getter private final int cassandraPort;

  /**
   * @param nodeId {@link #nodeId}
   * @param cassandraIP {@link #cassandraIP}
   * @param cassandraPort {@link #cassandraPort}
   */
  private LocalNodeInfo(final int nodeId, final String cassandraIP, final int cassandraPort) {
    this.nodeId = nodeId;
    this.cassandraIP = cassandraIP;
    this.cassandraPort = cassandraPort;
  }

  /**
   * @param grpcLocalNodeInfoObject from grpc local node info
   * @return local node info object from grpc equivalent
   */
  public static LocalNodeInfo fromGRPCLocalNodeInfoObject(
      final pathStoreProto.LocalNodeInfo grpcLocalNodeInfoObject) {
    return new LocalNodeInfo(
        grpcLocalNodeInfoObject.getNodeId(),
        grpcLocalNodeInfoObject.getCassandraIP(),
        grpcLocalNodeInfoObject.getCassandraPort());
  }

  /** @return grpc local node info object from this object */
  public pathStoreProto.LocalNodeInfo toGRPCLocalNodeInfoObject() {
    return pathStoreProto
        .LocalNodeInfo
        .newBuilder()
        .setNodeId(this.nodeId)
        .setCassandraIP(this.cassandraIP)
        .setCassandraPort(this.cassandraPort)
        .build();
  }
}

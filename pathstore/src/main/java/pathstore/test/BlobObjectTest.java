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

import pathstore.common.tables.ServerIdentity;
import pathstore.util.BlobObject;

import java.nio.ByteBuffer;

/**
 * Simple test to check the functionality of the {@link BlobObject} serialization and
 * deserialization. This is used to write any java object to a Cassandra blob
 */
public class BlobObjectTest {
  public static void main(String[] args) {
    ServerIdentity original = new ServerIdentity(("myles' bytes").getBytes(), "myles' passphrase");

    ByteBuffer originalSerialized = original.serialize();

    ServerIdentity deSerialized = (ServerIdentity) BlobObject.deserialize(originalSerialized);

    if (deSerialized == null) throw new RuntimeException("deSerialization failed");

    System.out.println(
        String.format("%s %s", new String(deSerialized.privateKey), deSerialized.passphrase));
  }
}

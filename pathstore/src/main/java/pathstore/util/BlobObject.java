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

package pathstore.util;

import com.datastax.driver.core.utils.Bytes;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;

/** This interface should extend any class you wish to store in a blob object within cassandra. */
public interface BlobObject extends Serializable {

  /** Serial version UID */
  long serialVersionUID = -700369464622170054L;

  /** Log errors during serialization and de-serialization */
  PathStoreLogger logger = PathStoreLoggerFactory.getLogger(BlobObject.class);

  /** @return byte buffer of object in hex string format, prefixed with '0x' */
  default ByteBuffer serialize() {
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bytes)) {
      oos.writeObject(this);
      String hexString = Bytes.toHexString(bytes.toByteArray());
      return Bytes.fromHexString(hexString);
    } catch (IOException e) {
      logger.error("Serializing blob object error");
      logger.error(e);
      return null;
    }
  }

  /**
   * @param bytes byte buffer from database
   * @return object
   */
  static BlobObject deserialize(final ByteBuffer bytes) {
    String hx = Bytes.toHexString(bytes);
    ByteBuffer ex = Bytes.fromHexString(hx);
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(ex.array()))) {
      return (BlobObject) ois.readObject();
    } catch (ClassNotFoundException | IOException e) {
      logger.error("Deserializing blob object error");
      logger.error(e);
      return null;
    }
  }
}

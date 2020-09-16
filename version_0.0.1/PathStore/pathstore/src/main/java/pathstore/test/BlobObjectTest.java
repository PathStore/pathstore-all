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
        String.format("%s %s", new String(deSerialized.privKey), deSerialized.passphrase));
  }
}

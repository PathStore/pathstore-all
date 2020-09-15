package pathstore.util;

import com.datastax.driver.core.utils.Bytes;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;

public interface BlobObject extends Serializable {

  PathStoreLogger logger = PathStoreLoggerFactory.getLogger(BlobObject.class);

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

  static BlobObject deserialize(ByteBuffer bytes) {
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

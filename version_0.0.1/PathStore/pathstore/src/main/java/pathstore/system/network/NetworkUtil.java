package pathstore.system.network;

import com.google.protobuf.ByteString;

import java.io.*;

/** Network utils for GRPC object write and read */
public class NetworkUtil {

  /**
   * @param object object to write to byte string
   * @return grpc bytestring
   */
  public static ByteString writeObject(final Object object) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(object);
      out.flush();
      return ByteString.copyFrom(bos.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param byteString from grpc response
   * @return object parsed must be casted
   */
  public static Object readObject(final ByteString byteString) {
    try (ObjectInput in =
        new ObjectInputStream(new ByteArrayInputStream(byteString.toByteArray()))) {
      return in.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}

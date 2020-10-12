package pathstore.system.network;

import com.google.protobuf.ByteString;

import java.io.*;

// TODO: Comment
public class NetworkUtil {
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

  public static Object readObject(final byte[] bytes) {
    try (ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return in.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}

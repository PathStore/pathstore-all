package pathstore.system.network;

import com.google.protobuf.ByteString;
import pathstore.grpc.pathStoreProto;

import java.io.*;
import java.util.*;
import java.util.function.Function;

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
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  // 1mb
  private static final int PAYLOAD_SIZE = 1024 * 1024;

  // TODO: Comment
  public static List<List<ByteString>> objectToByteChunksWindows(final Object... objects) {
    List<byte[]> objectsInBytes = new ArrayList<>();

    for (Object object : objects) { // convert all objects to byte arrays
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
          ObjectOutputStream out = new ObjectOutputStream(bos)) {
        out.writeObject(object);
        out.flush();
        objectsInBytes.add(bos.toByteArray());
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    int position = 0;

    List<List<ByteString>> objectInChunkWindows = new ArrayList<>();

    Set<Integer> neededPartitions;
    while ((neededPartitions = getNeededPartitions(objectsInBytes, position)).size() > 0) {

      List<ByteString> window = new ArrayList<>();

      int chunkSize = PAYLOAD_SIZE / neededPartitions.size();

      int added = 0;

      for (int i = 0; i < objectsInBytes.size(); i++) {
        byte[] objectInBytes = objectsInBytes.get(i);
        if (neededPartitions.contains(i)) {
          int toAdd = Math.min(position + chunkSize, objectInBytes.length);
          window.add(ByteString.copyFrom(Arrays.copyOfRange(objectInBytes, position, toAdd)));
          added = Math.max(toAdd, added);
          System.out.println(
              String.format(
                  "i: %d, toAdd: %d, added: %d, sent: %d", i, toAdd, added, toAdd - position));
        } else window.add(null);
      }
      position += (added - position);
      System.out.println(
          String.format(
              "Partitions: %s, added: %d, position: %d", neededPartitions, added, position));
      objectInChunkWindows.add(window);
    }

    int maxObjectLength = 0;
    for (byte[] objectsInByte : objectsInBytes)
      if (objectsInByte.length > maxObjectLength) maxObjectLength = objectsInByte.length;

    System.out.println(
        String.format(
            "Final position: %d, Max object length: %d, Difference (should be 0): %d",
            position, maxObjectLength, maxObjectLength - position));

    return objectInChunkWindows;
  }

  // TODO: Comment
  private static Set<Integer> getNeededPartitions(
      final List<byte[]> objectBytes, final int position) {
    Set<Integer> neededPartitions = new HashSet<>();
    for (int i = 0; i < objectBytes.size(); i++)
      if (position < objectBytes.get(i).length) neededPartitions.add(i);
    return neededPartitions;
  }

  // TODO: Comment
  @SafeVarargs
  public static <T> List<Object> concatenate(
      final Iterator<T> iterator,
      final Function<T, pathStoreProto.Status> getStatus,
      final Function<T, byte[]>... getByteArrays) {
    try {
      List<ByteArrayOutputStream> arrayOutputStreams = new ArrayList<>();
      // init output stream
      for (int i = 0; i < getByteArrays.length; i++)
        arrayOutputStreams.add(new ByteArrayOutputStream());

      T value = iterator.next();
      do {
        int i = 0;
        int counter = 0;
        for (Function<T, byte[]> function : getByteArrays) {
          byte[] array = function.apply(value);
          counter = Math.max(array.length, counter);
          arrayOutputStreams.get(i++).write(array);
        }
        System.out.println(String.format("Got %d bytes", counter));
        value = iterator.next();
      } while (getStatus.apply(value).equals(pathStoreProto.Status.PENDING));

      List<Object> objects = new ArrayList<>();
      for (ByteArrayOutputStream arrayOutputStream : arrayOutputStreams)
        objects.add(
            new ObjectInputStream(new ByteArrayInputStream(arrayOutputStream.toByteArray()))
                .readObject());
      return objects;
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
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
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}

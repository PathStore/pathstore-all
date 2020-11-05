package pathstore.system.network;

import com.google.protobuf.ByteString;
import pathstore.grpc.pathStoreProto.Status;

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

  /** How large a split can be. For now it is 1mb */
  private static final int PAYLOAD_SIZE = 1024 * 1024;

  /**
   * This function should be used when you're trying to send some number of objects to the user over
   * a server-side stream call. The idea here is to split these objects into some number of windows
   * where each window is the PAYLOAD_SIZE except for the last window where it will be at most the
   * payload size. (Ideally, currently the payloads are not optimized, see the github issue related
   * to this, we have at most n unoptimized splits)
   *
   * <p>Also note that when we create a window we must add the largest amount we can of each object
   * if they haven't already been written to the stream. This is because we want to write them in
   * some linear fashion and not have "gaps" in the payloads
   *
   * @param objects objects to split
   * @return list of windows. If you passed n objects as the parameters each window would be of size
   *     n. Each objects chunk is at their ordered index of insertion in the window. For example if
   *     you called this function with o1, and o2 in that order at the 0 index of each window you
   *     would find a chunk for o1. If there are no more chunks to be written we set it to null at
   *     their index.
   */
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

    // While there are still objects that haven't been written fully to the stream
    Set<Integer> neededPartitions;
    while ((neededPartitions = stillHaveData(objectsInBytes, position)).size() > 0) {

      List<ByteString> window = new ArrayList<>();

      int chunkSize = PAYLOAD_SIZE / neededPartitions.size();
      int added = 0;
      for (int i = 0; i < objectsInBytes.size(); i++) {
        byte[] objectInBytes = objectsInBytes.get(i);

        // for all objects that still have data to be written to the stream
        if (neededPartitions.contains(i)) {
          int toAdd = Math.min(position + chunkSize, objectInBytes.length);
          window.add(ByteString.copyFrom(Arrays.copyOfRange(objectInBytes, position, toAdd)));
          added = Math.max(toAdd, added);
        } else window.add(null);
      }

      // update the position to the place of the max added chunk.
      // As the only time it is less than max is when an object has been exhausted
      position += (added - position);
      objectInChunkWindows.add(window);
    }

    return objectInChunkWindows;
  }

  /**
   * This function is used to calculate a set of indices that still require chunks to be sent. In
   * other words they should be included in the next window
   *
   * @param objectBytes bytes to search through
   * @param position position after most recent write
   * @return set of indices that have lesser length than the position
   */
  private static Set<Integer> stillHaveData(final List<byte[]> objectBytes, final int position) {
    Set<Integer> neededPartitions = new HashSet<>();
    for (int i = 0; i < objectBytes.size(); i++)
      if (position < objectBytes.get(i).length) neededPartitions.add(i);
    return neededPartitions;
  }

  /**
   * This function is used to calculate a set of indices that still require chunks to be sent. In
   * other words they should be included in the next window
   *
   * @param iterator iterator from a blocking stub response
   * @param getStatus function to transfer a RespT object to a Status. (Status is used to determine
   *     when the data is done flowing)
   * @param getByteArrays a var args of functions to denote how to get the byte arrays from the
   *     fields inside RespT
   * @param <RespT> Response Type
   */
  @SafeVarargs
  public static <RespT> List<Object> concatenate(
      final Iterator<RespT> iterator,
      final Function<RespT, Status> getStatus,
      final Function<RespT, byte[]>... getByteArrays) {
    try {
      List<ByteArrayOutputStream> arrayOutputStreams = new ArrayList<>();
      // init output stream
      for (int i = 0; i < getByteArrays.length; i++)
        arrayOutputStreams.add(new ByteArrayOutputStream());

      for (RespT value = iterator.next();
          !getStatus.apply(value).equals(Status.PENDING);
          value = iterator.next()) {
        int i = 0;
        int counter = 0;
        for (Function<RespT, byte[]> function : getByteArrays) {
          byte[] array = function.apply(value);
          counter = Math.max(array.length, counter);
          arrayOutputStreams.get(i++).write(array);
        }
      }

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

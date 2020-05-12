package pathstore.common.logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to manage all loggers throughout pathstore and the pathstore api
 *
 * @see PathStoreLogger
 */
public final class PathStoreLoggerFactory {
  /** Map of loggers from name -> literal logger */
  private static final Map<String, PathStoreLogger> loggers = new ConcurrentHashMap<>();

  /**
   * Denotes the start point to start the merge from.
   *
   * <p>This is used because everytime the daemon requests the merged log all in-memory logs are
   * cleared. Thus instead of looping from 0 to the current count we have a starting point. This is
   * simply a small optimization to {@link #getMergedLog()}
   */
  private static int startPoint = 0;

  /**
   * Lazy function to pass class object instead of name
   *
   * @param c current class the logger is in
   * @return {@link #getLogger(String)}
   */
  public static PathStoreLogger getLogger(final Class<?> c) {
    return getLogger(c.getSimpleName());
  }

  /**
   * Manually set name of logger
   *
   * @param name name of logger
   * @return returns {@link PathStoreLogger} iff it doesn't already exist
   * @apiNote {@link #loggers} is a concurrent map thus no locking within this function is required
   */
  public static PathStoreLogger getLogger(final String name) {
    return loggers.computeIfAbsent(name, k -> new PathStoreLogger(name));
  }

  /**
   * This function is used to check to see if there are new messages available (This is to avoid
   * redundant writes to cassandra)
   *
   * @return true iff a single logger has new logs
   * @see PathStoreLogger#hasNew()
   */
  public static boolean hasNew() {

    for (PathStoreLogger logger : loggers.values()) if (logger.hasNew()) return true;

    return false;
  }

  /**
   * Allows the log daemon to query a merged log of all loggers used within pathstore
   *
   * @return list of merged strings
   * @apiNote merging is controlled by {@link PathStoreLogger#counter}
   */
  public static List<PathStoreLoggerMessage> getMergedLog() {

    Map<Integer, PathStoreLoggerMessage> map = new HashMap<>();

    for (PathStoreLogger logger : loggers.values()) map.putAll(logger.getMessages());

    LinkedList<PathStoreLoggerMessage> messages = new LinkedList<>();

    int endPoint = PathStoreLogger.counter.get();

    // Create the merged log
    for (int i = startPoint; i < endPoint; i++) {
      PathStoreLoggerMessage current = map.get(i);
      if (current == null) continue;
      messages.addLast(current);
    }

    // Clear all in memory logger data
    loggers.values().forEach(PathStoreLogger::clear);

    // Set start point
    startPoint = endPoint;

    return messages;
  }
}

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
   * @param level minimum logging level to check
   * @return true iff a single logger has new logs under the minimum log constraint
   * @see PathStoreLogger#hasNew(LoggerLevel)
   */
  public static boolean hasNew(final LoggerLevel level) {

    for (PathStoreLogger logger : loggers.values()) if (logger.hasNew(level)) return true;

    return false;
  }

  /**
   * Allows the log daemon to query a merged log of all loggers used within pathstore
   *
   * @param level what is the minimum level of logger to filter by
   * @return list of merged strings
   * @apiNote merging is controlled by {@link PathStoreLogger#counter}
   */
  public static List<String> getMergedLog(final LoggerLevel level) {
    int currentOrdinal = level.ordinal();

    Map<Integer, LoggerMessage> map = new HashMap<>();

    for (PathStoreLogger logger : loggers.values()) map.putAll(logger.getMessages(level));

    LinkedList<String> messages = new LinkedList<>();

    for (int i = 0; i < PathStoreLogger.counter.get(); i++) {
      LoggerMessage current = map.get(i);
      if (current == null) continue;
      if (current.getLoggerLevel().ordinal() >= currentOrdinal)
        messages.addLast(current.getFormattedMessage());
    }

    return messages;
  }
}

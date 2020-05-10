package pathstore.common.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is used as an internal logger for pathstore and the pathstore api.
 *
 * <p>Level ranks:
 *
 * <p>FINEST, DEBUG, INFO, ERROR
 */
public class PathStoreLogger {

  /** Denotes the counter */
  static final AtomicInteger counter = new AtomicInteger(0);

  /** Denotes the max size of log level list */
  private static final int MAX_ORDINAL = LoggerLevel.values().length;

  /** Name of logger */
  private final String name;

  /** Map of messages used to allow for merging of multiple loggers concurrently */
  private final Map<Integer, LoggerMessage> messages;

  /** Used to denote what level of messages are displayed */
  private LoggerLevel displayLevel;

  /** Is there new data available to read from */
  private boolean[] hasNew;

  /** @param name name of logger */
  protected PathStoreLogger(final String name) {
    this.name = name;
    this.messages = new ConcurrentHashMap<>();
    this.displayLevel = LoggerLevel.INFO;
    this.hasNew = new boolean[MAX_ORDINAL];
  }

  /**
   * Send an information message
   *
   * @param message message to show
   */
  public void info(final String message) {
    this.log(LoggerLevel.INFO, message);
  }

  /**
   * Send a debug message
   *
   * @param message message to show
   */
  public void debug(final String message) {
    this.log(LoggerLevel.DEBUG, message);
  }

  /**
   * Send a finest message
   *
   * @param message message to show
   */
  public void finest(final String message) {
    this.log(LoggerLevel.FINEST, message);
  }

  /**
   * Send an error message
   *
   * @param message message to show
   */
  public void error(final String message) {
    this.log(LoggerLevel.ERROR, message);
  }

  /**
   * Pass an error so the stack trace can be printed
   *
   * @param throwable throwable to print
   */
  public void error(final Throwable throwable) {
    StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));
    this.log(LoggerLevel.ERROR, sw.toString());
  }

  /**
   * Stores log messages internally and prints message iff the message level <= the display level
   *
   * @param loggerLevel which log level to print to
   * @param message what message to print
   */
  public void log(final LoggerLevel loggerLevel, final String message) {

    this.setNew(loggerLevel, true);

    int count = counter.getAndIncrement();

    LoggerMessage loggerMessage = new LoggerMessage(count, loggerLevel, message, this.name);

    this.messages.put(count, loggerMessage);

    if (loggerMessage.getLoggerLevel().ordinal() >= this.displayLevel.ordinal())
      System.out.println(loggerMessage.getFormattedMessage());
  }

  /**
   * Allows the class to write updates to the new array to denote whether messages have been written
   * or not
   *
   * <p>The state is set from loggerLevel -> MAX to status
   *
   * @param loggerLevel min logger level
   * @param status what status to set
   */
  private void setNew(final LoggerLevel loggerLevel, final boolean status) {
    int ordinal = loggerLevel.ordinal();

    do {
      this.hasNew[ordinal] = status;
    } while ((ordinal += 1) < MAX_ORDINAL);
  }

  /**
   * Sets has new to false as the logger factory as pulled the messages recently
   *
   * @param loggerLevel what logger level of messages are you looking for
   * @return internal messages written by this logger
   * @apiNote Since this function is used for {@link PathStoreLoggerFactory} we don't need to filter
   *     off messages that are in the bound of [0, loggerLevel] as {@link
   *     PathStoreLoggerFactory#getMergedLog(LoggerLevel)} will check for proper bounds. This helps
   *     not produce null pointer exceptions as all messages are ordered by {@link #counter}
   */
  protected Map<Integer, LoggerMessage> getMessages(final LoggerLevel loggerLevel) {

    //this.setNew(loggerLevel, false);

    return this.messages;
  }

  /**
   * Checks to see if new messages are available
   *
   * @param loggerLevel what minimum logger level to check
   * @return true if any logger level greater than or equal to loggerLevel is true
   */
  protected boolean hasNew(final LoggerLevel loggerLevel) {

    for (int ordinal = loggerLevel.ordinal(); ordinal < MAX_ORDINAL; ordinal++)
      if (this.hasNew[ordinal]) return true;

    return false;
  }
}

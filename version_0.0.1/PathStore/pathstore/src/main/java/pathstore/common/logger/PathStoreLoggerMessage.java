package pathstore.common.logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PathStoreLoggerMessage {

  /** What position in the array are you */
  private final int count;

  /** What level to print at */
  private final LoggerLevel loggerLevel;

  /** What information to show */
  private final String message;

  /** Name of logger */
  private final String loggerName;

  /** Formatted message */
  private final String formattedMessage;

  /**
   * @param loggerLevel {@link #loggerLevel}
   * @param message {@link #message}
   */
  PathStoreLoggerMessage(
      final int count,
      final LoggerLevel loggerLevel,
      final String message,
      final String loggerName) {
    this.count = count;
    this.loggerLevel = loggerLevel;
    this.message = message;
    this.loggerName = loggerName;
    this.formattedMessage = this.formatMessage();
  }

  /**
   * Formats a message to [type][type][loggername] message
   *
   * @return formatted message
   */
  private String formatMessage() {
    return String.format(
        "[%4.6s][%-20s][%s] %s",
        this.loggerLevel.toString(),
        this.loggerName,
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()),
        this.message);
  }

  /** @return count */
  public int getCount() {
    return count;
  }

  /** @return logger level of message */
  public LoggerLevel getLoggerLevel() {
    return this.loggerLevel;
  }

  /** @return formatted message */
  public String getFormattedMessage() {
    return this.formattedMessage;
  }
}

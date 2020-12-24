package pathstore.system.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** This class denotes some log message in the system */
@RequiredArgsConstructor
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
  @Getter(lazy = true)
  private final String formattedMessage = formatMessage(this.loggerLevel, this.loggerName, this.message);

  /**
   * Formats a message to [type][type][loggername] message
   *
   * @return formatted message
   */
  private static String formatMessage(final LoggerLevel loggerLevel, final String loggerName, final String message) {
    return String.format(
        "[%-6s][%-40s][%s] %s",
        loggerLevel.toString(),
        loggerName,
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()),
        message);
  }

  /** @return count */
  public int getCount() {
    return count;
  }

  /** @return logger level of message */
  public LoggerLevel getLoggerLevel() {
    return this.loggerLevel;
  }
}

package pathstore.system.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
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
  private final String formattedMessage = this.formatMessage();

  /**
   * Formats a message to [type][type][loggername] message
   *
   * @return formatted message
   */
  private String formatMessage() {
    return String.format(
        "[%-6s][%-40s][%s] %s",
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

package pathstoreweb.pathstoreadminpanel.services.logs;

import pathstore.common.logger.LoggerLevel;

/**
 * This class is used to represent a UDT value of log_message which is used in the logs table to
 * denote a list of messages
 */
public class LogMessage {

  /** What type of message is it */
  public final LoggerLevel loggerLevel;

  /** What is the message */
  public final String message;

  /**
   * @param loggerLevel {@link #loggerLevel}
   * @param message {@link #message}
   */
  public LogMessage(final LoggerLevel loggerLevel, final String message) {
    this.loggerLevel = loggerLevel;
    this.message = message;
  }
}

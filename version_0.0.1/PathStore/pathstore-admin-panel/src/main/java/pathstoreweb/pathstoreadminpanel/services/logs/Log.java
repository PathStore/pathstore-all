package pathstoreweb.pathstoreadminpanel.services.logs;

import pathstore.common.Constants;
import pathstore.common.logger.LoggerLevel;

/** This class represents a log record within the {@link Constants#LOGS} table */
public class Log {
  /** Which node the log is associated with */
  public final int nodeId;

  /** What date was the log written. Used for client side filtering by date */
  public final String date;

  /** Ordering of log */
  public final int count;

  /** What level of message it is */
  public final LoggerLevel loggerLevel;

  /** What was the message */
  public final String log;

  /**
   * @param nodeId {@link #nodeId}
   * @param date {@link #date}
   * @param count {@link #count}
   * @param loggerLevel {@link #loggerLevel}
   * @param log {@link #log}
   */
  public Log(
      final int nodeId,
      final String date,
      final int count,
      final LoggerLevel loggerLevel,
      final String log) {
    this.nodeId = nodeId;
    this.date = date;
    this.count = count;
    this.loggerLevel = loggerLevel;
    this.log = log;
  }
}

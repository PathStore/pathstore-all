package pathstoreweb.pathstoreadminpanel.services.logs;

import pathstore.common.Constants;

/** This class represents a log record within the {@link Constants#LOGS} table */
public class Log {
  /** Which node the log is associated with */
  public final int nodeId;

  /** What date was the log written. Used for client side filtering by date */
  public final String date;

  /** What was the message */
  public final String log;

  /**
   * @param nodeId {@link #nodeId}
   * @param date {@link #date}
   * @param log {@link #log}
   */
  public Log(final int nodeId, final String date, final String log) {
    this.nodeId = nodeId;
    this.date = date;
    this.log = log;
  }
}

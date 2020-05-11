package pathstoreweb.pathstoreadminpanel.services.logs;

import pathstore.common.Constants;

import java.util.List;

/** This class represents a log record within the {@link Constants#LOGS} table */
public class Log {
  /** Which node the log is associated with */
  public final int nodeId;

  /** List of messages from user */
  public final List<String> log;

  /**
   * @param nodeId {@link #nodeId}
   * @param log {@link #log}
   */
  public Log(final int nodeId, final List<String> log) {
    this.nodeId = nodeId;
    this.log = log;
  }
}

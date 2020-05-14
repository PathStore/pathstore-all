package pathstoreweb.pathstoreadminpanel.services.logs.payload;

/**
 * TODO: Validations
 *
 * <p>TODO: Should log levels work similarly to log levels in a logger (i.e. log_level = INFO)
 * displays error aswell
 *
 * <p>This payload is used to query a specific set of logs based on the node_id, the date and the
 * specific log level the user wants to see
 */
public class GetLogRecordsPayload {

  /** Denotes the node_id requested */
  public final int node_id;

  /** Denotes the date they want */
  public final String date;

  /** Denotes the specific log level they want */
  public final String log_level;

  /**
   * @param node_id {@link #node_id}
   * @param date {@link #date}
   * @param log_level {@link #log_level}
   */
  public GetLogRecordsPayload(final int node_id, final String date, final String log_level) {
    this.node_id = node_id;
    this.date = date;
    this.log_level = log_level;
  }
}

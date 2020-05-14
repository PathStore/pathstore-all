package pathstoreweb.pathstoreadminpanel.services.logs.payload;

/**
 * TODO: Validations
 *
 * <p>TODO: Should log levels work similiarly to log levels in a logger (i.e. log_level = INFO)
 * displays error aswell
 *
 * <p>This payload is used to query a specific set of logs based on the node_id, the date and the
 * specific log level the user wants to see
 */
public class GetLogRecordsPayload {

  /** Denotes the node_id requested */
  public int node_id;

  /** Denotes the date they want */
  public String date;

  /** Denotes the specific log level they want */
  public String log_level;
}

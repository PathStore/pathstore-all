package pathstoreweb.pathstoreadminpanel.services.logs.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.logs.Log;

import java.util.List;

/** This class is used to format a list of logs into a readable json array */
public class LogRecordsFormatter implements IFormatter {

  /** Given from {@link pathstoreweb.pathstoreadminpanel.services.logs.GetLogRecords} */
  private final List<Log> logs;

  /** @param logs {@link #logs} */
  public LogRecordsFormatter(final List<Log> logs) {
    this.logs = logs;
  }

  /**
   * Parses the list into a json array of json objects where node_id is the node id of the given log
   * file and log is an array of strings from pathstore
   *
   * @return json array of log data for all nodes
   */
  @Override
  public ResponseEntity<String> format() {
    JSONArray data = new JSONArray();

    for (Log log : this.logs) {
      JSONObject object = new JSONObject();
      object.put(Constants.LOGS_COLUMNS.NODE_ID, log.nodeId);

      JSONArray array = new JSONArray();

      log.log.forEach(
          i ->
              array.put(
                  new JSONObject()
                      .put(Constants.LOG_MESSAGE_PROPERTIES.MESSAGE_TYPE, i.loggerLevel.name())
                      .put(Constants.LOG_MESSAGE_PROPERTIES.MESSAGE, i.message)));

      object.put(Constants.LOGS_COLUMNS.LOG, array);

      data.put(object);
    }

    return new ResponseEntity<>(data.toString(), HttpStatus.OK);
  }
}

package pathstoreweb.pathstoreadminpanel.services.logs.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.logs.Log;

import java.util.LinkedList;
import java.util.Map;

/** This class is used to format a list of logs into a readable json array */
public class LogRecordsFormatter implements IFormatter {

  /** Given from {@link pathstoreweb.pathstoreadminpanel.services.logs.GetLogRecords} */
  private final Map<Integer, LinkedList<Log>> map;

  /** @param map {@link #map} */
  public LogRecordsFormatter(final Map<Integer, LinkedList<Log>> map) {
    this.map = map;
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

    for (Map.Entry<Integer, LinkedList<Log>> entry : this.map.entrySet()) {
      JSONObject object = new JSONObject();
      object.put(Constants.LOGS_COLUMNS.NODE_ID, entry.getKey());

      JSONArray array = new JSONArray();

      entry
          .getValue()
          .forEach(
              i ->
                  array.put(
                      new JSONObject()
                          .put(Constants.LOGS_COLUMNS.LOG_LEVEL, i.loggerLevel.name())
                          .put(Constants.LOGS_COLUMNS.LOG, i.log)));

      object.put(Constants.LOGS_COLUMNS.LOG, array);

      data.put(object);
    }

    return new ResponseEntity<>(data.toString(), HttpStatus.OK);
  }
}

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
  private final Map<Integer, Map<String, LinkedList<Log>>> map;

  /** @param map {@link #map} */
  public LogRecordsFormatter(final Map<Integer, Map<String, LinkedList<Log>>> map) {
    this.map = map;
  }

  /**
   * Parses the map of maps. The response is a json array of a node object. The node object is
   * comprised of a node id and a json array of logs per date
   *
   * @return json array of parsed data
   */
  @Override
  public ResponseEntity<String> format() {
    JSONArray data = new JSONArray();

    for (Map.Entry<Integer, Map<String, LinkedList<Log>>> entry : this.map.entrySet()) {
      JSONObject nodeObject = new JSONObject();
      nodeObject.put(Constants.LOGS_COLUMNS.NODE_ID, entry.getKey());

      for (Map.Entry<String, LinkedList<Log>> innerEntry : entry.getValue().entrySet()) {
        JSONArray dates = new JSONArray();
        JSONObject dateObject = new JSONObject();
        dateObject.put(Constants.LOGS_COLUMNS.DATE, innerEntry.getKey());

        JSONArray array = new JSONArray();
        innerEntry.getValue().forEach(i -> array.put(i.log));
        dateObject.put(Constants.LOGS_COLUMNS.LOG, array);

        dates.put(dateObject);
        nodeObject.put("dates", dates);
      }

      data.put(nodeObject);
    }

    return new ResponseEntity<>(data.toString(), HttpStatus.OK);
  }
}

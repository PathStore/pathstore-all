package pathstoreweb.pathstoreadminpanel.services.availablelogdates.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

import java.util.List;
import java.util.Map;

public class GetAvailableLogDatesFormatter implements IFormatter {

  private final Map<Integer, List<String>> nodeToDates;

  public GetAvailableLogDatesFormatter(final Map<Integer, List<String>> nodeToDates) {
    this.nodeToDates = nodeToDates;
  }

  @Override
  public ResponseEntity<String> format() {

    JSONArray array = new JSONArray();

    for (Map.Entry<Integer, List<String>> entry : this.nodeToDates.entrySet())
      array.put(
          new JSONObject()
              .put(Constants.AVAILABLE_LOG_DATES_COLUMNS.NODE_ID, entry.getKey())
              .put(Constants.AVAILABLE_LOG_DATES_COLUMNS.DATE, entry.getValue()));

    return new ResponseEntity<>(array.toString(), HttpStatus.OK);
  }
}

package pathstoreweb.pathstoreadminpanel.services.logs.formatter;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

import java.util.List;

/** This class is used to format a list of logs into a readable json array */
public class LogRecordsFormatter implements IFormatter {

  /** Given from {@link pathstoreweb.pathstoreadminpanel.services.logs.GetLogRecords} */
  private final List<String> messages;

  /** @param messages {@link #messages} */
  public LogRecordsFormatter(final List<String> messages) {
    this.messages = messages;
  }

  /**
   * Parses the list of logs based on the filtering parameters sent by the user into a json object
   * of logs: string[]
   *
   * @return json object of parsed logs
   */
  @Override
  public ResponseEntity<String> format() {

    JSONObject object = new JSONObject();

    object.put(Constants.LOGS, messages);

    return new ResponseEntity<>(object.toString(), HttpStatus.OK);
  }
}

package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstore.system.schemaFSM.ApplicationEntry;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.GetApplicationState;

import java.util.List;

/**
 * Formatter for Application State.
 *
 * @see GetApplicationState
 */
public class GetApplicationStateFormatter implements IFormatter {

  /** List of Application Entries */
  private final List<ApplicationEntry> entryList;

  /** @param entryList generated from {@link GetApplicationState} */
  public GetApplicationStateFormatter(final List<ApplicationEntry> entryList) {
    this.entryList = entryList;
  }

  /**
   * Generates a json array. See the readme for an example out.
   *
   * @return json array of current node_schemas state.
   */
  @Override
  public ResponseEntity<String> format() {
    JSONArray array = new JSONArray();

    for (ApplicationEntry entry : this.entryList) {
      JSONObject object = new JSONObject();

      object
          .put(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, entry.node_id)
          .put(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, entry.keyspace_name)
          .put(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, entry.proccess_status.toString())
          .put(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID, entry.process_uuid.toString())
          .put(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, entry.waiting_for);

      array.put(object);
    }

    return new ResponseEntity<>(array.toString(), HttpStatus.OK);
  }
}

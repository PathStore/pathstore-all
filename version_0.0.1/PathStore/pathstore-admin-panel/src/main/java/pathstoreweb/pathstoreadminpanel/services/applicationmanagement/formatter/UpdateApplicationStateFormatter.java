package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter;

import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants.NODE_SCHEMAS_COLUMNS;
import pathstore.system.schemaFSM.ApplicationEntry;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

/**
 * This formatter is used to take a list of node_schema records that where written to the db and
 * format the records written into a json array to be returned to the user.
 */
public class UpdateApplicationStateFormatter implements IFormatter {

  /** Records written will always be non-empty */
  private final Map<Integer, ApplicationEntry> entriesWritten;

  /** @param entriesWritten {@link #entriesWritten} */
  public UpdateApplicationStateFormatter(final Map<Integer, ApplicationEntry> entriesWritten) {
    this.entriesWritten = entriesWritten;
  }

  /** @return create json array of json objects for each record written */
  @Override
  public ResponseEntity<String> format() {
    JSONArray array = new JSONArray();

    for (Map.Entry<Integer, ApplicationEntry> record : this.entriesWritten.entrySet()) {
      JSONObject object = new JSONObject();

      ApplicationEntry value = record.getValue();

      object.put(NODE_SCHEMAS_COLUMNS.NODE_ID, value.node_id);
      object.put(NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, value.keyspace_name);
      object.put(NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, value.proccess_status.toString());
      object.put(NODE_SCHEMAS_COLUMNS.WAIT_FOR, value.waiting_for.toString());
      object.put(NODE_SCHEMAS_COLUMNS.PROCESS_UUID, value.process_uuid.toString());

      array.put(object);
    }

    return new ResponseEntity<>(array.toString(), HttpStatus.OK);
  }
}

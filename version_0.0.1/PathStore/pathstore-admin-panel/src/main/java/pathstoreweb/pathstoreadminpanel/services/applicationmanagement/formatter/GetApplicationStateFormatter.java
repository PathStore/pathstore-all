package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstore.system.schemaFSM.NodeSchemaEntry;
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
  private final List<NodeSchemaEntry> entryList;

  /** @param entryList generated from {@link GetApplicationState} */
  public GetApplicationStateFormatter(final List<NodeSchemaEntry> entryList) {
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

    for (NodeSchemaEntry entry : this.entryList) {
      JSONObject object = new JSONObject();

      object
          .put(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, entry.nodeId)
          .put(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, entry.keyspaceName)
          .put(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, entry.status.toString())
          .put(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, entry.waitFor);

      array.put(object);
    }

    return new ResponseEntity<>(array.toString(), HttpStatus.OK);
  }
}

package pathstoreweb.pathstoreadminpanel.formatter.applicationmanagement;

import org.json.JSONArray;
import org.json.JSONObject;
import pathstore.common.Constants;
import pathstore.system.schemaloader.ApplicationEntry;
import pathstoreweb.pathstoreadminpanel.formatter.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.ApplicationState;

import java.util.List;

/**
 * Formatter for Application State.
 *
 * @see pathstoreweb.pathstoreadminpanel.services.applicationmanagement.ApplicationState
 */
public class ApplicationStateFormatter implements IFormatter {

  /** List of Application Entries */
  private final List<ApplicationEntry> entryList;

  /** @param entryList generated from {@link ApplicationState} */
  public ApplicationStateFormatter(final List<ApplicationEntry> entryList) {
    this.entryList = entryList;
  }

  /**
   * Generates a json array. See the readme for an example out.
   *
   * @return json array of current node_schemas state.
   */
  @Override
  public String format() {
    JSONArray array = new JSONArray();

    for (ApplicationEntry entry : this.entryList) {
      JSONObject object = new JSONObject();

      object
          .put(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, entry.node_id)
          .put(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, entry.keyspace_name)
          .put(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS, entry.proccess_status.toString())
          .put(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_UUID, entry.proccess_status)
          .put(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, entry.waiting_for);

      array.put(object);
    }

    return array.toString();
  }
}

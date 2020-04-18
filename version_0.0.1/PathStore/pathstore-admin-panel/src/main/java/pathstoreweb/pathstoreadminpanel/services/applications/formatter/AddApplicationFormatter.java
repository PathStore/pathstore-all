package pathstoreweb.pathstoreadminpanel.services.applications.formatter;

import org.json.JSONObject;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

/**
 * Response for {@link pathstoreweb.pathstoreadminpanel.services.applications.AddApplication}.
 * Returns the status of the request
 *
 * @see pathstoreweb.pathstoreadminpanel.services.applications.AddApplication
 */
public class AddApplicationFormatter implements IFormatter {

  /** Status of request */
  private final String status;

  /** @param status {@link #status} */
  public AddApplicationFormatter(final String status) {
    this.status = status;
  }

  /** @return json wrapped status */
  @Override
  public String format() {
    JSONObject object = new JSONObject();

    object.put("keyspace_created", this.status);

    return object.toString();
  }
}

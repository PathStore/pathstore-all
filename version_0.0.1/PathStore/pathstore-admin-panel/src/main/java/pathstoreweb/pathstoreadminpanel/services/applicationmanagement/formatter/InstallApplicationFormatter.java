package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter;

import org.json.JSONObject;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.InstallApplication;

/**
 * Formatter for DeployApplication
 *
 * @see InstallApplication
 */
public class InstallApplicationFormatter implements IFormatter {

  /** Number of inserts that occurred */
  private final int numOfInserts;

  /** @param numOfInserts {@link #numOfInserts} */
  public InstallApplicationFormatter(final int numOfInserts) {
    this.numOfInserts = numOfInserts;
  }

  /**
   * TODO: Maybe return a json array of entries that were written
   *
   * @return json response
   */
  @Override
  public String format() {

    JSONObject object = new JSONObject();

    object.put("num_of_inserts", this.numOfInserts);

    return object.toString();
  }
}

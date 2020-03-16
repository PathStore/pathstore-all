package pathstoreweb.pathstoreadminpanel.formatter.applicationmanagement;

import org.json.JSONObject;
import pathstoreweb.pathstoreadminpanel.formatter.IFormatter;

/**
 * Formatter for DeployApplication
 *
 * @see pathstoreweb.pathstoreadminpanel.services.applicationmanagement.DeployApplication
 */
public class DeployApplicationFormatter implements IFormatter {

  /** Number of inserts that occurred */
  private final int numOfInserts;

  /** @param numOfInserts {@link #numOfInserts} */
  public DeployApplicationFormatter(final int numOfInserts) {
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

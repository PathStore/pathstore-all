package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter;

import org.json.JSONObject;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

/**
 * This class is used to pass an error that has occurred during a request to install/uninstall an
 * application
 *
 * @see pathstoreweb.pathstoreadminpanel.services.applicationmanagement.InstallApplication
 * @see pathstoreweb.pathstoreadminpanel.services.applicationmanagement.UnInstallApplication
 */
public class ResponseErrorFormatter implements IFormatter {

  /** error message that was generated */
  private final String errorMessage;

  /** @param errorMessage {@link #errorMessage} */
  public ResponseErrorFormatter(final String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /**
   * Wraps the error message into a json string
   *
   * @return wrapped error message
   */
  @Override
  public String format() {
    JSONObject response = new JSONObject();

    response.put("error", this.errorMessage);

    return response.toString();
  }
}

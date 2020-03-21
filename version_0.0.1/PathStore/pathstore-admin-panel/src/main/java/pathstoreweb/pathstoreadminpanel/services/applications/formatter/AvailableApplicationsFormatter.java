package pathstoreweb.pathstoreadminpanel.services.applications.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.applications.Application;

import java.util.List;

/**
 * Simple formatter for a list of available applications.
 *
 * <p>Used by the frontend to limit what the user can query on application installation
 *
 * @see pathstoreweb.pathstoreadminpanel.services.applications.AvailableApplications
 */
public class AvailableApplicationsFormatter implements IFormatter {

  /** List of available applications */
  private final List<Application> applications;

  /** @param applications {@link #applications} */
  public AvailableApplicationsFormatter(final List<Application> applications) {
    this.applications = applications;
  }

  /**
   * Json array of all available applications
   *
   * @return json array
   */
  @Override
  public String format() {
    JSONArray response = new JSONArray();

    for (Application application : this.applications) {
      JSONObject object = new JSONObject();

      object
          .put(Constants.APPS_COLUMNS.KEYSPACE_NAME, application.keyspaceName)
          .put(Constants.APPS_COLUMNS.AUGMENTED_SCHEMA, application.augmentedSchema);

      response.put(object);
    }

    return response.toString();
  }
}

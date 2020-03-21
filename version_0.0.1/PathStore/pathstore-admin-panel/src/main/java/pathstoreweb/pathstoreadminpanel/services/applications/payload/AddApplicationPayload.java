package pathstoreweb.pathstoreadminpanel.services.applications.payload;

import org.springframework.web.multipart.MultipartFile;
import pathstoreweb.pathstoreadminpanel.services.applications.validator.ApplicationNameUnique;
import pathstoreweb.pathstoreadminpanel.services.applications.validator.ApplicationNamePathStore;
import pathstoreweb.pathstoreadminpanel.services.applications.validator.ApplicationSchemaExists;

/**
 * TODO: Check to ensure that the application name and the application schema filename and keyspace
 * created have the same name
 *
 * <p>This class denotes the payload that is required for executing the add application endpoint.
 *
 * <p>It requires an application name a multipartfile that is a schema to load
 *
 * @see pathstoreweb.pathstoreadminpanel.services.applications.AddApplication
 */
public class AddApplicationPayload {

  /** name of application to add */
  @ApplicationNamePathStore(message = "Application name must start with pathstore_")
  @ApplicationNameUnique(message = "Application already exists")
  public final String applicationName;

  /** Schema file to load */
  @ApplicationSchemaExists(message = "Application schema must be passed.")
  private MultipartFile applicationSchema;

  /**
   * The multipart file is not in the constructor because spring requires a setter for it (Doesn't
   * make any sense)
   *
   * @param applicationName {@link #applicationName}
   */
  public AddApplicationPayload(final String applicationName) {
    this.applicationName = applicationName;
  }

  /** @param applicationSchema passed by the spring http request handler */
  public void setApplicationSchema(final MultipartFile applicationSchema) {
    this.applicationSchema = applicationSchema;
  }

  /** @return retrieve the application schema */
  public MultipartFile getApplicationSchema() {
    return this.applicationSchema;
  }
}

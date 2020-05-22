package pathstoreweb.pathstoreadminpanel.services.applications.payload;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.web.multipart.MultipartFile;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.validator.ValidatedPayload;

import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.ADD_APPLICATION_PAYLOAD.*;

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
public final class AddApplicationPayload extends ValidatedPayload {

  /** name of application to add */
  public final String applicationName;

  /** Schema file to load */
  private MultipartFile applicationSchema;

  /**
   * The multipart file is not in the constructor because spring requires a setter for it (Doesn't
   * make any sense)
   *
   * @param application_name {@link #applicationName}
   */
  public AddApplicationPayload(final String application_name) {
    this.applicationName = application_name;
  }

  /** @param application_schema passed by the spring http request handler */
  public void setApplicationSchema(final MultipartFile application_schema) {
    this.applicationSchema = application_schema;
  }

  /** @return retrieve the application schema */
  public MultipartFile getApplicationSchema() {
    return this.applicationSchema;
  }

  /**
   * Checks to see if the information passed above is valid
   *
   * <p>(1): Wrong submission format
   *
   * <p>(2): Application name starts with pathstore_
   *
   * <p>(3): Application name is not already used
   *
   * <p>(4): Application schema is present
   *
   * @return all null iff the validity test has passed
   */
  @Override
  protected String[] calculateErrors() {

    // (1)
    if (this.bulkNullCheck(this.applicationName, this.applicationSchema)) {
      return new String[] {WRONG_SUBMISSION_FORMAT};
    }

    String[] errors = {
      IMPROPER_APPLICATION_NAME_FORM, APPLICATION_NAME_NOT_UNIQUE, APPLICATION_SCHEMA_NOT_PASSED
    };

    Session session = PathStoreCluster.getInstance().connect();

    // (2)
    if (this.applicationName.startsWith("pathstore_")) errors[0] = null;

    // (3)
    Select selectApplicationName =
        QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);
    selectApplicationName.where(
        QueryBuilder.eq(Constants.APPS_COLUMNS.KEYSPACE_NAME, this.applicationName));

    for (Row row : session.execute(selectApplicationName)) errors[1] = null;

    // (4)
    if (this.applicationSchema != null) errors[2] = null;

    return errors;
  }
}

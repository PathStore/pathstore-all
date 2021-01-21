package pathstoreweb.pathstoreadminpanel.services.applications.payload;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.web.multipart.MultipartFile;
import pathstore.client.PathStoreClientAuthenticatedCluster;
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

  /** Master password for application */
  public final String masterPassword;

  /** CLT definition */
  public final int clientLeaseTime;

  /** SLT = CLT + serverAdditionalTime */
  public final int serverAdditionalTime;

  /**
   * The multipart file is not in the constructor because spring requires a setter for it (Doesn't
   * make any sense)
   *
   * @param application_name {@link #applicationName}
   * @param master_password {@link #masterPassword}
   * @param client_lease_time {@link #clientLeaseTime}
   * @param server_additional_time {@link #serverAdditionalTime}
   */
  public AddApplicationPayload(
      final String application_name,
      final String master_password,
      final int client_lease_time,
      final int server_additional_time) {
    this.applicationName = application_name;
    this.masterPassword = master_password;
    this.clientLeaseTime = client_lease_time;
    this.serverAdditionalTime = server_additional_time;
  }

  /** @param applicationSchema passed by the spring http request handler */
  public void setApplicationSchema(final MultipartFile applicationSchema) {
    this.applicationSchema = applicationSchema;
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
   * <p>(4): CLT <= 0
   *
   * <p>(5): Additional Server Time <= 0
   *
   * @return all null iff the validity test has passed
   */
  @Override
  protected String[] calculateErrors() {

    // (1)
    if (this.bulkNullCheck(this.applicationName, this.applicationSchema, this.masterPassword))
      return new String[] {WRONG_SUBMISSION_FORMAT};

    String[] errors = {IMPROPER_APPLICATION_NAME_FORM, null, null, null};

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    // (2)
    if (this.applicationName.startsWith("pathstore_")) errors[0] = null;

    // (3)
    Select selectApplicationName =
        QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);
    selectApplicationName.where(
        QueryBuilder.eq(Constants.APPS_COLUMNS.KEYSPACE_NAME, this.applicationName));

    for (Row row : session.execute(selectApplicationName)) errors[1] = APPLICATION_NAME_NOT_UNIQUE;

    if (this.clientLeaseTime <= 0) errors[2] = CLIENT_LEASE_TIME_OUT_OF_BOUNDS;

    if (this.serverAdditionalTime <= 0) errors[3] = SERVER_ADDITIONAL_TIME_OUT_OF_BOUNDS;

    return errors;
  }
}

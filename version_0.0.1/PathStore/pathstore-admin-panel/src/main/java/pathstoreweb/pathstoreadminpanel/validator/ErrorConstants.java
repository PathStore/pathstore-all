package pathstoreweb.pathstoreadminpanel.validator;

/**
 * This class is used to denote all error constants that could occur during the execution of an API
 * endpoint
 */
public final class ErrorConstants {

  public static final class ADD_APPLICATION_DEPLOYMENT_RECORD_PAYLOAD {
    public static final String EMPTY = "You must pass at least one record";
    public static final String TO_MANY_KEYSPACES =
        "You can only pass one keyspace update per request";
    public static final String INVALID_RECORD =
        "Each record must reference a valid node id and it must not already have the given application installed on the node";
  }

  public static final class DELETE_APPLICATION_DEPLOYMENT_RECORD_PAYLOAD {
    public static final String EMPTY = ADD_APPLICATION_DEPLOYMENT_RECORD_PAYLOAD.EMPTY;
    public static final String TO_MANY_KEYSPACES =
        ADD_APPLICATION_DEPLOYMENT_RECORD_PAYLOAD.TO_MANY_KEYSPACES;
    public static final String INVALID_RECORD =
        "Each record must reference a valid node id and it must already have the given keyspace installed on it";
  }

  /**
   * Validity errors for {@link
   * pathstoreweb.pathstoreadminpanel.services.applications.payload.AddApplicationPayload}
   */
  public static final class ADD_APPLICATION_PAYLOAD {
    public static final String WRONG_SUBMISSION_FORMAT =
        "You must submit the fields: application_name which is the desired name for your application and application_schema which is a valid cql file";
    public static final String IMPROPER_APPLICATION_NAME_FORM =
        "Your application name must start with pathstore_";
    public static final String APPLICATION_NAME_NOT_UNIQUE =
        "The application name you passed is already used";
  }

  /**
   * Validity errors for {@link
   * pathstoreweb.pathstoreadminpanel.services.servers.payload.AddServerPayload}
   */
  public static final class ADD_SERVER_PAYLOAD {
    public static final String WRONG_SUBMISSION_FORMAT =
        "You must submit the fields: ip, username, password, ssh_port, rmi_port, name";
    public static final String IP_IS_NOT_UNIQUE =
        "You must use an ip address that isn't already in use";
    public static final String NAME_IS_NOT_UNIQUE = "You must use a name that isn't already in use";
    public static final String CONNECTION_INFORMATION_IS_INVALID =
        "The connection information you provided is invalid";
  }

  /**
   * Validity errors for {@link
   * pathstoreweb.pathstoreadminpanel.services.servers.payload.UpdateServerPayload}
   */
  public static final class UPDATE_SERVER_PAYLOAD {
    public static final String WRONG_SUBMISSION_FORMAT =
        "You must submit the following fields: server_uuid, ip, username, password, ssh_port, rmi_port, name";
    public static final String SERVER_UUID_DOESNT_EXIST =
        "The server uuid you passed does not exist";
    public static final String IP_IS_NOT_UNIQUE =
        "The ip you've submitted conflicts with another ip in the record set that is not the original ip given";
    public static final String NAME_IS_NOT_UNIQUE =
        "The name you've submitted conflicts with another server name in the record set that is not the original name given";
    public static final String SERVER_UUID_IS_NOT_FREE =
        "You cannot modify a server record that is attached to an existing pathstore node";
    public static final String CONNECTION_INFORMATION_IS_INVALID =
        "The connection information you provided is invalid";
  }

  /**
   * Validity errors for {@link
   * pathstoreweb.pathstoreadminpanel.services.servers.payload.DeleteServerPayload}
   */
  public static final class DELETE_SERVER_PAYLOAD {
    public static final String WRONG_SUBMISSION_FORMAT =
        "You must submit the following fields: server_uuid";
    public static final String SERVER_UUID_DOESNT_EXIST = "The server uuid does not exist";
    public static final String SERVER_UUID_IS_NOT_FREE =
        "The server uuid passed cannot be attached to a pathstore node";
  }

  /**
   * Validity errors for {@link
   * pathstoreweb.pathstoreadminpanel.services.deployment.payload.AddDeploymentRecordPayload}
   */
  public static final class ADD_DEPLOYMENT_RECORD_PAYLOAD {
    public static final String EMPTY = "You cannot pass an empty list of deployment objects";
    public static final String SERVER_UUID_DUPLICATES = "You cannot have duplicate server uuid's";
    public static final String NODE_ID_DUPLICATES = "You cannot have duplicate node id's";
    public static final String SERVER_UUID_IN_USE = "A server uuid passed is already in use";
    public static final String NODE_IDS_IN_USE = "A node id passed is already in use";
    public static final String PARENT_ID_NOT_VALID =
        "A parent id passed does not point to a valid node";
    public static final String NODE_ID_EQUALS_PARENT_ID =
        "You cannot have a node where the node id equals the parent id";
    public static final String SERVER_UUID_DOESNT_EXIST = "A server uuid passed does not exist";
  }

  /**
   * Validity errors for {@link
   * pathstoreweb.pathstoreadminpanel.services.deployment.payload.UpdateDeploymentRecordPayload}
   */
  public static final class UPDATE_DEPLOYMENT_RECORD_PAYLOAD {
    public static final String INVALID_FAILED_ENTRY = "You must enter a valid failed entry";
  }

  /**
   * Validity errors for {@link
   * pathstoreweb.pathstoreadminpanel.services.deployment.payload.DeleteDeploymentRecordPayload}
   */
  public static final class DELETE_DEPLOYMENT_RECORD_PAYLOAD {
    public static final String EMPTY = ADD_DEPLOYMENT_RECORD_PAYLOAD.EMPTY;
    public static final String INVALID_RECORD = "You must only pass records that are DEPLOYED";
  }

  /**
   * Validity errors for {@link
   * pathstoreweb.pathstoreadminpanel.services.logs.payload.GetLogRecordsPayload}
   */
  public static final class GET_LOG_RECORDS_PAYLOAD {
    public static final String WRONG_SUBMISSION_FORMAT =
        "You must submit the following fields: node_id, date, log_level";
    public static final String INVALID_NODE_ID = "The node id passed is invalid";
    public static final String INVALID_DATE = "The date passed does not have any records";
    public static final String INVALID_LOG_LEVEL = "The log level passed is invalid";
  }
}

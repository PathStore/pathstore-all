package pathstore.system.deployment.utilities;

import pathstore.common.Role;
import pathstore.system.deployment.commands.ForcePush;

/** Constants used in {@link DeploymentBuilder} */
public class DeploymentConstants {
  // name of cassandra container and directory in repo
  public static final String CASSANDRA = "cassandra";

  // name of pathstore container and directory in repo
  public static final String PATHSTORE = "pathstore";

  // base directory on remote host
  public static final String REMOTE_BASE_DIRECTORY = "pathstore-install";

  // logs directory name
  public static final String LOGS_DIRECTORY_NAME = "logs";

  // pathstore dir
  private static final String REMOTE_PATHSTORE_SUB_DIR =
      String.format("%s/%s", REMOTE_BASE_DIRECTORY, PATHSTORE);

  // logs dir
  public static final String REMOTE_PATHSTORE_LOGS_SUB_DIR =
      String.format("%s/%s", REMOTE_PATHSTORE_SUB_DIR, LOGS_DIRECTORY_NAME);

  // pathstore registry directories
  public static final String PATHSTORE_REGISTRY = "pathstore-registry";

  /**
   * Constants for the remove function
   *
   * @see DeploymentBuilder#remove(ForcePush)
   */
  public static final class REMOVAL_COMMANDS {
    public static final String KILL_CASSANDRA = String.format("docker kill %s", CASSANDRA);
    public static final String REMOVE_CASSANDRA = String.format("docker rm %s", CASSANDRA);

    public static final String KILL_PATHSTORE = String.format("docker kill %s", PATHSTORE);
    public static final String REMOVE_PATHSTORE = String.format("docker rm %s", PATHSTORE);

    public static final String REMOVE_BASE_DIRECTORY =
        String.format("rm -rf %s", REMOTE_BASE_DIRECTORY);

    public static final String REMOVE_DOCKER_CERTS = "rm -rf /etc/docker/certs.d/*";
  }

  /**
   * Constants for the init function
   *
   * @see DeploymentBuilder#init()
   */
  public static final class INIT_COMMANDS {
    public static final String DOCKER_CHECK = "docker ps";

    public static final String CREATE_BASE_DIRECTORY =
        String.format("mkdir %s", REMOTE_BASE_DIRECTORY);
  }

  /**
   * Constants for the create remote directory
   *
   * @see DeploymentBuilder#createRemoteDirectory(String)
   */
  public static final class CREATE_REMOTE_DIRECTORY {
    public static final String CREATE_SUB_DIRECTORY = "mkdir -p %s";
  }

  /**
   * Constants for generate properties command
   *
   * @see DeploymentBuilder#generatePropertiesFiles(int, String, int, Role, String, int, String,
   *     int, String, int, String, int, String, String, String, String, String)
   */
  public static final class GENERATE_PROPERTIES {
    public static final String LOCAL_TEMP_PROPERTIES_FILE =
        "/etc/pathstore/temp-properties-file.properties";

    public static final String REMOTE_PATHSTORE_PROPERTIES_FILE =
        String.format("%s/pathstore.properties", REMOTE_PATHSTORE_SUB_DIR);
  }

  /** Constants that denote how docker containers are run */
  public static final class RUN_COMMANDS {

    /**
     * @param rootIP root ip, (where the registry is)
     * @return run command for cassandra
     */
    public static String CASSANDRA_RUN(final String rootIP) {
      return String.format(
          "docker run --network=host -dit --restart always --name %s %s/%s",
          CASSANDRA, rootIP, CASSANDRA);
    }

    /**
     * @param rootIP root ip, (where the registry is)
     * @return run command for the pathstore container
     */
    public static String PATHSTORE_RUN(final String rootIP) {
      return String.format(
          "docker run --network=host -dit --restart always -v ~/%s:/etc/pathstore --user $(id -u):$(id -g) --name %s %s/%s",
          String.format("%s/%s", "pathstore-install", "pathstore"), PATHSTORE, rootIP, PATHSTORE);
    }
  }
}

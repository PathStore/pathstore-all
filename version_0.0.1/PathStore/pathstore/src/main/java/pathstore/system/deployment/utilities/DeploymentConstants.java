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

  // local docker certs dir
  public static final String LOCAL_DOCKER_CERTS_DIR = "/etc/docker/certs.d";

  /**
   * @param registryIP registry ip
   * @return /etc/docker/certs.d/registry_ip/
   */
  public static String LOCAL_DOCKER_REGISTRY_CERT_DIR(final String registryIP) {
    return String.format("%s/%s", LOCAL_DOCKER_CERTS_DIR, registryIP);
  }

  /**
   * @param registryIP registry ip
   * @return mkdir to create local docker registry cert dir
   */
  public static String CREATE_LOCAL_DOCKER_REGISTRY_CERT_DIR(final String registryIP) {
    return String.format("mkdir -p %s", LOCAL_DOCKER_REGISTRY_CERT_DIR(registryIP));
  }

  /**
   * @param registryIP registry ip
   * @return where the cert should be present on a child machine in order to pull from the pathstore
   *     registry
   */
  public static String LOCAL_DOCKER_REGISTRY_CERT(final String registryIP) {
    return String.format("%s/ca.crt", LOCAL_DOCKER_REGISTRY_CERT_DIR(registryIP));
  }

  // docker registry cert name
  public static final String DOCKER_REGISTRY_CERT_NAME = "pathstore-registry.crt";

  // docker registry cert location
  public static final String REMOTE_DOCKER_REGISTRY_CERT_LOCATION =
      String.format("%s/%s", REMOTE_PATHSTORE_SUB_DIR, DOCKER_REGISTRY_CERT_NAME);

  /**
   * @param registryIP registry ip
   * @return command to copy from the remote docker registry location to local location
   *     (pathstore-install -> /etc/docker)
   */
  public static String COPY_FROM_REMOTE_TO_LOCAL_DOCKER_REGISTRY_CERT(final String registryIP) {
    return String.format(
        "cp ~/%s %s",
        DeploymentConstants.REMOTE_DOCKER_REGISTRY_CERT_LOCATION,
        DeploymentConstants.LOCAL_DOCKER_REGISTRY_CERT(registryIP));
  }

  /**
   * @param registryIP registry ip
   * @return chgrp command to set the local docker registry cert dir to docker
   */
  public static String CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIRECTORY(final String registryIP) {
    return String.format("chgrp docker -R %s", LOCAL_DOCKER_REGISTRY_CERT_DIR(registryIP));
  }

  /**
   * @param registryIP registry ip
   * @return chmod command to set the local docker registry cert dir permissions to 775
   */
  public static String CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIRECTORY(
      final String registryIP) {
    return String.format("chmod 775 -R %s", LOCAL_DOCKER_REGISTRY_CERT_DIR(registryIP));
  }

  // where the cert is locally accessible
  public static final String LOCAL_DOCKER_REGISTRY_CERT_LOCATION =
      String.format("/etc/pathstore/%s", DOCKER_REGISTRY_CERT_NAME);

  /**
   * Constants for the remove function
   *
   * @see DeploymentBuilder#remove(ForcePush, String)
   */
  public static final class REMOVAL_COMMANDS {
    public static final String KILL_CASSANDRA = String.format("docker kill %s", CASSANDRA);
    public static final String REMOVE_CASSANDRA = String.format("docker rm %s", CASSANDRA);

    public static final String KILL_PATHSTORE = String.format("docker kill %s", PATHSTORE);
    public static final String REMOVE_PATHSTORE = String.format("docker rm %s", PATHSTORE);

    public static final String REMOVE_BASE_DIRECTORY =
        String.format("rm -rf %s", REMOTE_BASE_DIRECTORY);

    /**
     * @param registryIP pathstore registry ip
     * @return removal command to remove registry certs directory
     */
    public static String REMOVE_DOCKER_CERTS(final String registryIP) {
      return String.format("rm -rf /etc/docker/certs.d/%s", registryIP);
    }
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
   *     int, String, int, String, int, String, String, String, String, String, String)
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
          "docker run --network=host -dit --restart always --name %s %s/%s:latest",
          CASSANDRA, rootIP, CASSANDRA);
    }

    /**
     * @param rootIP root ip, (where the registry is)
     * @return run command for the pathstore container
     */
    public static String PATHSTORE_RUN(final String rootIP, final String version) {
      return String.format(
          "docker run --network=host -dit --restart always -v ~/%s:/etc/pathstore --user $(id -u):$(id -g) --name %s %s/%s:%s",
          String.format("%s/%s", "pathstore-install", "pathstore"),
          PATHSTORE,
          rootIP,
          PATHSTORE,
          version);
    }
  }
}

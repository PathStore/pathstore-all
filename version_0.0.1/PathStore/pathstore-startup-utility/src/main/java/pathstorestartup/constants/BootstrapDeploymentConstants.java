package pathstorestartup.constants;

import pathstore.system.deployment.utilities.DeploymentConstants;
import pathstorestartup.DevelopmentDeployment;

import java.util.Arrays;
import java.util.List;

/**
 * This class is used to store all constants related to {@link DevelopmentDeployment} and {@link
 * BootstrapDeploymentBuilder}
 */
public class BootstrapDeploymentConstants {

  /* local paths to relative information */
  public static final String PATHSTORE_ADMIN_PANEL = "pathstore-admin-panel";
  public static final String LOCAL_TEMP_PROPERTIES_FILE = "temp-properties-file.properties";

  /* Prompts and notifications */
  public static final String ENTRY = "Development Deployment utility";
  public static final String DIRECTORY_PROMPT = "PathStore Directory: ";
  public static final String DEPLOYMENT_ENTRY = "\nDeployment server information\n";
  public static final String HOST_PROMPT = "Host: ";
  public static final String USERNAME_PROMPT = "Username: ";
  public static final String AUTH_TYPE_PROMPT =
      String.format("Auth Type (%s, %s): ", AUTH_TYPES.PASSWORD, AUTH_TYPES.KEY);
  public static final String PASSWORD_PROMPT = "Password: ";
  public static final String PRIVATE_KEY_PATH_PROMPT = "Private Key Absolute Path: ";
  public static final String PASSPHRASE_PROMPT = "Passphrase (hit enter if none exists): ";
  public static final String SSH_PORT_PROMPT = "SSH port: ";
  public static final String GRPC_PROMPT_PORT = "GRPC port (if unsure enter 1099): ";
  public static final String NETWORK_ADMIN_USERNAME_PROMPT = "Network Admin Username: ";
  public static final String NETWORK_ADMIN_PASSWORD_PROMPT = "Network Admin Password: ";
  public static final String PATHSTORE_VERSION = "PathStore Version: ";
  public static final String COULD_NOT_CONNECT =
      "Could not connect to the server with the provided credentials please try again or quit with ctrl-c";

  public static final class AUTH_TYPES {
    public static final String PASSWORD = "password";
    public static final String KEY = "key";
  }

  /**
   * These are the constants and their respective functions for {@link
   * pathstorestartup.DevelopmentBuilder}
   */
  public static final class DEVELOPMENT_BUILDER {
    // Operation to print whilst packing the program locally
    public static final String MVN_PACKAGE_TAG = "Building";

    /**
     * This function is used to generate the command to package a mvn project locally
     *
     * @param directoryToBuild which directory to build
     * @return mvn package command
     */
    public static List<String> MVN_PACKAGE(final String directoryToBuild) {
      return Arrays.asList("mvn", "package", "-f", directoryToBuild);
    }

    // Operation to print whilst building a docker image
    public static final String BUILDING_IMAGE_TAG = "Building image";

    /**
     * This function generates a docker build command
     *
     * @param name name of the image you're building
     * @param path where is the docker file located
     * @return docker build command for a certain image
     */
    public static List<String> BUILD_IMAGE(final String name, final String path) {
      return Arrays.asList("docker", "build", "-t", name, path);
    }
  }

  /** This class stores constants for {@link BootstrapDeploymentBuilder#bootstrapInit()} */
  public static final class INIT_BOOTSTRAP_COMMANDS {
    // command to kill the pathstore admin panel container
    public static final String KILL_PATHSTORE_ADMIN_PANEL =
        String.format("docker kill %s", PATHSTORE_ADMIN_PANEL);

    // command to remove the pathstore admin panel container
    public static final String REMOVE_PATHSTORE_ADMIN_PANEL =
        String.format("docker rm %s", PATHSTORE_ADMIN_PANEL);

    /** command to kill the pathstore registry container */
    public static final String KILL_PATHSTORE_REGISTRY =
        String.format("docker kill %s", DeploymentConstants.PATHSTORE_REGISTRY);

    // command to remove the pathstore registry container
    public static final String REMOVE_PATHSTORE_REGISTRY =
        String.format("docker rm %s", DeploymentConstants.PATHSTORE_REGISTRY);
  }

  /**
   * This class stores constants related to deploying the website. This is not included in pathstore
   * as this only occurs on the deployment of the root node
   */
  public static final class REMOTE_DIRECTORIES_AND_FILES {
    // subdir on the remote host for the website
    public static final String REMOTE_PATHSTORE_ADMIN_PANEL_SUB_DIR =
        String.format("%s/%s", DeploymentConstants.REMOTE_BASE_DIRECTORY, PATHSTORE_ADMIN_PANEL);

    // where to store the website properties file on the remote host
    public static final String REMOTE_PATHSTORE_ADMIN_PANEL_PROPERTIES_FILE =
        String.format("%s/pathstore.properties", REMOTE_PATHSTORE_ADMIN_PANEL_SUB_DIR);
  }

  /** This class stores the run commands for {@link BootstrapDeploymentBuilder} */
  public static final class RUN_COMMANDS {

    // how to store the admin panel
    public static String PATHSTORE_ADMIN_PANEL_RUN(final String rootIP) {
      return String.format(
          "docker run --network=host -dit --restart always -v ~/%s:/etc/pathstore --name %s %s/%s:latest",
          REMOTE_DIRECTORIES_AND_FILES.REMOTE_PATHSTORE_ADMIN_PANEL_SUB_DIR,
          PATHSTORE_ADMIN_PANEL,
          rootIP,
          PATHSTORE_ADMIN_PANEL);
    }
  }
}

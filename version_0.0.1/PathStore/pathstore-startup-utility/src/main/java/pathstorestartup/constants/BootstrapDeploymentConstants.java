package pathstorestartup.constants;

import pathstore.system.deployment.utilities.DeploymentConstants;
import pathstore.system.deployment.utilities.SSHUtil;
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
  public static final String PASSWORD_PROMPT = "Password: ";
  public static final String SSH_PORT_PROMPT = "SSH port: ";
  public static final String RMI_PORT_PROMPT = "RMI port (if unsure enter 1099): ";
  public static final String COULD_NOT_CONNECT =
      "Could not connect to the server with the provided credentials please try again or quit with ctrl-c";

  /**
   * These are the constants and their respective functions for {@link
   * pathstorestartup.DevelopmentBuilder}
   */
  public static final class DEVELOPMENT_BUILDER {
    // Operation to print out whilst deleting a local tar if applicable
    public static final String DELETE_TAR_TAG = "Deleting";

    /**
     * This function is used to generate the command to delete a tar file
     *
     * @param tarLocation absolute location of tar file
     * @return command to execute
     */
    public static List<String> DELETE_TAR(final String tarLocation) {
      return Arrays.asList("rm", "-f", tarLocation);
    }

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

    // Operation to print tag whilst saving a docker image
    public static final String SAVING_IMAGE_TAG = "Saving image";

    /**
     * This function generates a docker save command to a specific output tar directory
     *
     * @param outputTar where to save the docker tar
     * @param imageName what image to save
     * @return docker save command
     */
    public static List<String> SAVING_IMAGE(final String outputTar, final String imageName) {
      return Arrays.asList("docker", "save", "-o", outputTar, imageName);
    }
  }

  /** This class stores constants for {@link BootstrapDeploymentBuilder#initBootstrap()} */
  public static final class INIT_BOOTSTRAP_COMMANDS {
    // command to kill the pathstore admin panel container
    public static final String KILL_PATHSTORE_ADMIN_PANEL =
        String.format("docker kill %s", PATHSTORE_ADMIN_PANEL);

    // command to remove the pathstore admin panel container
    public static final String REMOVE_PATHSTORE_ADMIN_PANEL =
        String.format("docker rm %s", PATHSTORE_ADMIN_PANEL);

    // command to remove the old pathstore admin panel image
    public static final String REMOVE_PATHSTORE_ADMIN_PANEL_IMAGE =
        String.format("docker image rm %s", PATHSTORE_ADMIN_PANEL);
  }

  /**
   * This class stores formatable strings to determine the local locations of tar files
   *
   * @see DevelopmentDeployment#init()
   */
  public static final class DEVELOPMENT_TAR_LOCATIONS {
    public static final String LOCAL_CASSANDRA_TAR =
        "%s/"
            + String.format(
                "%s/%s.tar", DeploymentConstants.CASSANDRA, DeploymentConstants.CASSANDRA);

    public static final String LOCAL_PATHSTORE_TAR =
        "%s/"
            + String.format(
                "%s/%s.tar", DeploymentConstants.PATHSTORE, DeploymentConstants.PATHSTORE);

    public static final String LOCAL_PATHSTORE_ADMIN_PANEL_TAR =
        "%s/" + String.format("%s/%s.tar", PATHSTORE_ADMIN_PANEL, PATHSTORE_ADMIN_PANEL);
  }

  /**
   * This class stores constants related to deploying the website. This is not included in pathstore
   * as this only occurs on the deployment of the root node
   */
  public static final class REMOTE_DIRECTORIES_AND_FILES {
    // subdir on the remote host for the website
    public static final String REMOTE_PATHSTORE_ADMIN_PANEL_SUB_DIR =
        String.format("%s/%s", DeploymentConstants.REMOTE_BASE_DIRECTORY, PATHSTORE_ADMIN_PANEL);

    // where to store the tar on the remote host
    public static final String REMOTE_PATHSTORE_ADMIN_PANEL_TAR =
        String.format("%s/%s.tar", REMOTE_PATHSTORE_ADMIN_PANEL_SUB_DIR, PATHSTORE_ADMIN_PANEL);

    // where to store the website properties file on the remote host
    public static final String REMOTE_PATHSTORE_ADMIN_PANEL_PROPERTIES_FILE =
        String.format("%s/pathstore.properties", REMOTE_PATHSTORE_ADMIN_PANEL_SUB_DIR);
  }

  /** This class stores the run commands for {@link BootstrapDeploymentBuilder} */
  public static final class RUN_COMMANDS {

    // how to store the admin panel
    public static final String PATHSTORE_ADMIN_PANEL_RUN =
        String.format(
            "docker run --network=host -dit --restart always -v ~/%s:/etc/pathstore --name %s %s",
            REMOTE_DIRECTORIES_AND_FILES.REMOTE_PATHSTORE_ADMIN_PANEL_SUB_DIR,
            PATHSTORE_ADMIN_PANEL,
            PATHSTORE_ADMIN_PANEL);
  }
}

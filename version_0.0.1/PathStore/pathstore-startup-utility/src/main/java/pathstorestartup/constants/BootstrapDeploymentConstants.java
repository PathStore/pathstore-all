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

  /** This class stores constants for {@link BootstrapDeploymentBuilder#mkcertSetup(String)} */
  public static final class DOCKER_REGISTRY_CERTIFICATE {

    /**
     * @param registryIP registry ip
     * @return cert file name
     */
    public static String LOCAL_CERT_NAME(final String registryIP) {
      return String.format("%s.pem", registryIP);
    }

    /**
     * @param registryIP registry ip
     * @return key file name
     */
    public static String LOCAL_KEY_NAME(final String registryIP) {
      return String.format("%s-key.pem", registryIP);
    }

    // docker registry key location
    public static final String REMOTE_DOCKER_REGISTRY_KEY_LOCATION =
        String.format(
            "%s/%s", DeploymentConstants.REMOTE_PATHSTORE_SUB_DIR, "pathstore-registry.key");

    // mkcert program name
    public static final String MKCERT_NAME = "mkcert-v1.4.3-linux-amd64";

    // command to download program from github
    public static final List<String> DOWNLOAD_MKCERT =
        Arrays.asList(
            "wget",
            "https://github.com/FiloSottile/mkcert/releases/download/v1.4.3/mkcert-v1.4.3-linux-amd64");

    // entry message
    public static final String ENTRY_DOWNLOAD_MKCERT = "Downloading mkcert-v1.4.3-linux-amd64";

    // exit message
    public static final String EXIT_DOWNLOAD_MKCERT =
        "Finished downloading mkcert-v1.4.3-linux-amd64";

    // error message
    public static final String ERROR_DOWNLOAD_MKCERT = "Error download mkcert-v1.4.3-linux-amd64";

    // command to change mkcert access to be executable
    public static final List<String> CHANGE_MKCERT_ACCESS =
        Arrays.asList("chmod", "u+rtx", "mkcert-v1.4.3-linux-amd64");

    // entry message
    public static final String ENTRY_CHANGE_MKCERT_ACCESS =
        "Granting executable permissions for mkcert-v1.4.3-linux-amd64";

    // exit message
    public static final String EXIT_CHANGE_MKCERT_ACCESS =
        "Finished updating permissions for mkcert-v1.4.3-linux-amd64";

    // error message
    public static final String ERROR_CHANGE_MKCERT_ACCESS =
        "Error updating permissions for mkcert-v1.4.3-linux-amd64";

    /**
     * @param registryIP registry ip
     * @return command to generate certificates for registry
     */
    public static List<String> GENERATE_REGISTRY_CERTIFICATES(final String registryIP) {
      return Arrays.asList("./mkcert-v1.4.3-linux-amd64", registryIP);
    }

    // entry message
    public static final String ENTRY_GENERATE_REGISTRY_CERTIFICATES =
        "Creating pathstore-registry self signed certificates";

    // exit message
    public static final String EXIT_GENERATE_REGISTRY_CERTIFICATES =
        "Finished creating pathstore-registry self signed certificates";

    // error message
    public static final String ERROR_GENERATE_REGISTRY_CERTIFICATES =
        "Error creating pathstore-registry self signed certificates";

    /**
     * @param registryIP registry ip
     * @return command to create docker registry cert dir
     */
    public static List<String> CREATE_DOCKER_REGISTRY_CERT_DIR(final String registryIP) {
      return Arrays.asList(
          "mkdir", "-p", DeploymentConstants.LOCAL_DOCKER_REGISTRY_CERT_DIR(registryIP));
    }

    /**
     * @param registryIP registry ip
     * @return entry message
     */
    public static String ENTRY_CREATE_DOCKER_REGISTRY_CERT_DIR(final String registryIP) {
      return String.format(
          "Creating %s dir", DeploymentConstants.LOCAL_DOCKER_REGISTRY_CERT_DIR(registryIP));
    }

    /**
     * @param registryIP registry ip
     * @return exit message
     */
    public static String EXIT_CREATE_DOCKER_REGISTRY_CERT_DIR(final String registryIP) {
      return String.format(
          "Created %s dir", DeploymentConstants.LOCAL_DOCKER_REGISTRY_CERT_DIR(registryIP));
    }

    /**
     * @param registryIP registry ip
     * @return error message
     */
    public static String ERROR_CREATE_DOCKER_REGISTRY_CERT_DIR(final String registryIP) {
      return String.format(
          "Error creating %s", DeploymentConstants.LOCAL_DOCKER_REGISTRY_CERT_DIR(registryIP));
    }

    /**
     * @param registryIP registry ip
     * @return command to create docker registry cert dir
     */
    public static List<String> COPY_DOCKER_REGISTRY_CERT_TO_DIR(final String registryIP) {
      return Arrays.asList(
          "cp",
          LOCAL_CERT_NAME(registryIP),
          DeploymentConstants.LOCAL_DOCKER_REGISTRY_CERT(registryIP));
    }

    /**
     * @param registryIP registry ip
     * @return entry message
     */
    public static String ENTRY_COPY_DOCKER_REGISTRY_CERT_TO_DIR(final String registryIP) {
      return String.format(
          "Setting up local registry cert at %s",
          DeploymentConstants.LOCAL_DOCKER_REGISTRY_CERT(registryIP));
    }

    /**
     * @param registryIP registry ip
     * @return exit message
     */
    public static String EXIT_COPY_DOCKER_REGISTRY_CERT_TO_DIR(final String registryIP) {
      return String.format(
          "Set up local registry cert at %s",
          DeploymentConstants.LOCAL_DOCKER_REGISTRY_CERT(registryIP));
    }

    /**
     * @param registryIP registry ip
     * @return error message
     */
    public static String ERROR_COPY_DOCKER_REGISTRY_CERT_TO_DIR(final String registryIP) {
      return String.format(
          "Error setting up local registry cert at %s",
          DeploymentConstants.LOCAL_DOCKER_REGISTRY_CERT(registryIP));
    }

    /**
     * @param registryIP registry ip
     * @return command to change group of local docker registry dir to docker
     */
    public static List<String> CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIR(final String registryIP) {
      return Arrays.asList(
          DeploymentConstants.CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIRECTORY(registryIP)
              .split(" "));
    }

    // entry message
    public static final String ENTRY_CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIR =
        "Setting dir ownership";

    // exit message
    public static final String EXIT_CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIR = "Set dir ownership";

    // error message
    public static final String ERROR_CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIR =
        "Error setting dir ownership";

    /**
     * @param registryIP registry ip
     * @return command to update permission of local docker dir from 755 to 775
     */
    public static List<String> CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIR(
        final String registryIP) {
      return Arrays.asList(
          DeploymentConstants.CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIRECTORY(registryIP)
              .split(" "));
    }

    // entry message
    public static final String ENTRY_CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIR =
        "Setting dir permissions";

    // exit message
    public static final String EXIT_CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIR =
        "Set dir permission";

    // error message
    public static final String ERROR_CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIR =
        "Error setting dir permission";

    /**
     * @param fileToRemove file to remove
     * @return command to remove file
     */
    public static List<String> REMOVE_LOCAL_FILE(final String fileToRemove) {
      return Arrays.asList("rm", fileToRemove);
    }

    /**
     * @param fileToRemove file to remove
     * @return entry message
     */
    public static String ENTRY_REMOVE_LOCAL_FILE(final String fileToRemove) {
      return String.format("Removing %s", fileToRemove);
    }

    /**
     * @param fileToRemove file to remove
     * @return exit message
     */
    public static String EXIT_REMOVE_LOCAL_FILE(final String fileToRemove) {
      return String.format("Removed %s", fileToRemove);
    }

    /**
     * @param fileToRemove file to remove
     * @return error message
     */
    public static String ERROR_REMOVE_LOCAL_FILE(final String fileToRemove) {
      return String.format("Error removing %s", fileToRemove);
    }

    // docker registry run command
    public static final String DOCKER_REGISTRY_START =
        String.format(
            "docker run -d --restart=always --name pathstore-registry -v \"$(pwd)\"/%s:/certs -e REGISTRY_HTTP_ADDR=0.0.0.0:443 -e REGISTRY_HTTP_TLS_CERTIFICATE=/certs/pathstore-registry.crt -e REGISTRY_HTTP_TLS_KEY=/certs/pathstore-registry.key -p 443:443 registry:2",
            DeploymentConstants.REMOTE_PATHSTORE_SUB_DIR);

    /**
     * @param imageName docker image name in local registry
     * @param registryIP registry ip
     * @param version version of image
     * @return command to tag local image with registry ip and version
     */
    public static String DOCKER_TAG(
        final String imageName, final String registryIP, final String version) {
      return String.format("docker tag %s %s/%s:%s", imageName, registryIP, imageName, version);
    }

    /**
     * @param imageName docker image name in local registry
     * @param registryIP registry ip
     * @param version version of image
     * @return command to push tagged image to registry
     */
    public static String DOCKER_PUSH(
        final String imageName, final String registryIP, final String version) {
      return String.format("docker push %s/%s:%s", registryIP, imageName, version);
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

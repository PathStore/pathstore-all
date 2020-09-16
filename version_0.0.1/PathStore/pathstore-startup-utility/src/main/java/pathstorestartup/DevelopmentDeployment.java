package pathstorestartup;

import com.datastax.driver.core.utils.Bytes;
import com.jcraft.jsch.JSchException;
import pathstore.authentication.AuthenticationUtil;
import pathstore.common.Constants;
import pathstore.common.Role;
import pathstore.system.deployment.commands.*;
import pathstore.system.deployment.utilities.DeploymentConstants;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstore.system.deployment.utilities.ServerIdentity;
import pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils;
import pathstorestartup.commands.FinalizeRootInstallation;
import pathstorestartup.constants.BootstrapDeploymentConstants;
import pathstorestartup.constants.BootstrapDeploymentBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;

import static pathstorestartup.constants.BootstrapDeploymentConstants.DEVELOPMENT_BUILDER.*;

/**
 * This represents the development deployment process
 *
 * <p>TODO: Error check build command responses
 */
public class DevelopmentDeployment {

  /** static cassandra port todo: change */
  private static final int cassandraPort = 9052;

  /** Represents how to receive information from the user */
  private final Scanner scanner;

  /** local absolute paths to each tar that was generated */
  private String cassandraTar, pathstoreTar, pathstoreAdminPanelTar;

  /** @param scanner {@link #scanner} */
  public DevelopmentDeployment(final Scanner scanner) {
    this.scanner = scanner;
  }

  /**
   * Builds cassandra, pathstore and pathstore-admin-panel locally. This is represented by {@link
   * DevelopmentBuilder}
   */
  public void init() {
    System.out.println(BootstrapDeploymentConstants.ENTRY);
    String dir =
        Utils.askQuestionWithInvalidResponse(
            this.scanner, BootstrapDeploymentConstants.DIRECTORY_PROMPT, null);

    // gather local directories of interest
    String cassandraPath = String.format("%s/%s", dir, DeploymentConstants.CASSANDRA);
    String pathstorePath = String.format("%s/%s", dir, DeploymentConstants.PATHSTORE);
    String pathstoreAdminPanelPath =
        String.format("%s/%s", dir, BootstrapDeploymentConstants.PATHSTORE_ADMIN_PANEL);

    // set all tar locations
    this.cassandraTar =
        String.format(
            BootstrapDeploymentConstants.DEVELOPMENT_TAR_LOCATIONS.LOCAL_CASSANDRA_TAR, dir);
    this.pathstoreTar =
        String.format(
            BootstrapDeploymentConstants.DEVELOPMENT_TAR_LOCATIONS.LOCAL_PATHSTORE_TAR, dir);
    this.pathstoreAdminPanelTar =
        String.format(
            BootstrapDeploymentConstants.DEVELOPMENT_TAR_LOCATIONS.LOCAL_PATHSTORE_ADMIN_PANEL_TAR,
            dir);

    // add shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(this::cleanUp));

    try {
      // build cassandra
      new DevelopmentBuilder()
          .execute(
              BUILDING_IMAGE_TAG,
              DeploymentConstants.CASSANDRA,
              BUILD_IMAGE(DeploymentConstants.CASSANDRA, cassandraPath),
              0)
          .execute(
              SAVING_IMAGE_TAG,
              DeploymentConstants.CASSANDRA,
              SAVING_IMAGE(this.cassandraTar, DeploymentConstants.CASSANDRA),
              0)
          .build();

      // build pathstore
      new DevelopmentBuilder()
          .execute(MVN_PACKAGE_TAG, pathstorePath, MVN_PACKAGE(pathstorePath), 0)
          .execute(
              BUILDING_IMAGE_TAG,
              DeploymentConstants.PATHSTORE,
              BUILD_IMAGE(DeploymentConstants.PATHSTORE, pathstorePath),
              0)
          .execute(
              SAVING_IMAGE_TAG,
              DeploymentConstants.PATHSTORE,
              SAVING_IMAGE(this.pathstoreTar, DeploymentConstants.PATHSTORE),
              0)
          .build();

      // build pathstore-admin-panel
      new DevelopmentBuilder()
          .execute(
              MVN_PACKAGE_TAG, pathstoreAdminPanelPath, MVN_PACKAGE(pathstoreAdminPanelPath), 0)
          .execute(
              BUILDING_IMAGE_TAG,
              BootstrapDeploymentConstants.PATHSTORE_ADMIN_PANEL,
              BUILD_IMAGE(
                  BootstrapDeploymentConstants.PATHSTORE_ADMIN_PANEL, pathstoreAdminPanelPath),
              0)
          .execute(
              SAVING_IMAGE_TAG,
              BootstrapDeploymentConstants.PATHSTORE_ADMIN_PANEL,
              SAVING_IMAGE(
                  this.pathstoreAdminPanelTar, BootstrapDeploymentConstants.PATHSTORE_ADMIN_PANEL),
              0)
          .build();

      // deploy the built images to a server
      this.deploy();

    } finally {
      // remove local tars after deployment is finished or failed.
      this.cleanUp();
    }
  }

  /**
   * This function will prompt the user for the connection information to a server and ask for the
   * rmi port to start the root server with
   *
   * @see #initList(SSHUtil, String, int, String, String, String, String, FinalizeRootInstallation)
   * @see FinalizeRootInstallation
   */
  private void deploy() {
    System.out.println(BootstrapDeploymentConstants.DEPLOYMENT_ENTRY);
    String ip =
        Utils.askQuestionWithSpecificResponses(
            this.scanner, BootstrapDeploymentConstants.HOST_PROMPT, null);
    String username =
        Utils.askQuestionWithInvalidResponse(
            this.scanner, BootstrapDeploymentConstants.USERNAME_PROMPT, new String[] {"root"});

    String authType =
        Utils.askQuestionWithSpecificResponses(
            this.scanner,
            BootstrapDeploymentConstants.AUTH_TYPE_PROMPT,
            new String[] {
              BootstrapDeploymentConstants.AUTH_TYPES.PASSWORD,
              BootstrapDeploymentConstants.AUTH_TYPES.KEY
            });

    String password = null, privKeyPath = null, passphrase = null;

    if (authType.equals(BootstrapDeploymentConstants.AUTH_TYPES.PASSWORD))
      password =
          Utils.askQuestionWithInvalidResponse(
              this.scanner, BootstrapDeploymentConstants.PASSWORD_PROMPT, null);
    else {
      privKeyPath =
          Utils.askQuestionWithInvalidResponse(
              this.scanner, BootstrapDeploymentConstants.PRIVATE_KEY_PATH_PROMPT, null);
      passphrase =
          Utils.askQuestionWithInvalidResponse(
              this.scanner, BootstrapDeploymentConstants.PASSPHRASE_PROMPT, null);
    }

    int sshPort =
        Utils.askQuestionWithInvalidResponseInteger(
            this.scanner, BootstrapDeploymentConstants.SSH_PORT_PROMPT, null);
    int rmiPort =
        Utils.askQuestionWithInvalidResponseInteger(
            this.scanner, BootstrapDeploymentConstants.RMI_PORT_PROMPT, null);
    String networkAdminUsername =
        Utils.askQuestionWithInvalidResponse(
            this.scanner, BootstrapDeploymentConstants.NETWORK_ADMIN_USERNAME_PROMPT, null);
    String networkAdminPassword =
        Utils.askQuestionWithInvalidResponse(
            this.scanner, BootstrapDeploymentConstants.NETWORK_ADMIN_PASSWORD_PROMPT, null);

    try {
      SSHUtil sshUtil;

      byte[] privKey = null;

      if (authType.equals(BootstrapDeploymentConstants.AUTH_TYPES.PASSWORD))
        sshUtil = new SSHUtil(ip, username, password, sshPort);
      else {
        privKey = Files.readAllBytes(new File(privKeyPath).toPath());
        System.out.println(Bytes.toHexString(privKey));
        sshUtil =
            new SSHUtil(
                ip,
                username,
                sshPort,
                privKey,
                passphrase.trim().length() == 0 ? null : passphrase);
      }

      System.out.println("Connected");

      String childSuperuserUsername = Constants.PATHSTORE_SUPERUSER_USERNAME;
      String childSuperuserPassword = AuthenticationUtil.generateAlphaNumericPassword();

      try {
        // Execute all commands in the given list
        for (ICommand command :
            this.initList(
                sshUtil,
                ip,
                rmiPort,
                childSuperuserUsername,
                childSuperuserPassword,
                networkAdminUsername,
                networkAdminPassword,
                new FinalizeRootInstallation(
                    childSuperuserUsername,
                    childSuperuserPassword,
                    ip,
                    cassandraPort,
                    username,
                    authType,
                    authType.equals(BootstrapDeploymentConstants.AUTH_TYPES.PASSWORD)
                        ? null
                        : new ServerIdentity(privKey, passphrase),
                    password,
                    sshPort,
                    rmiPort))) {
          System.out.println(command);
          command.execute();
        }
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(-1);
      } finally {
        sshUtil.disconnect();
      }

    } catch (JSchException | IOException e) {
      e.printStackTrace();
      System.err.println(BootstrapDeploymentConstants.COULD_NOT_CONNECT);
      this.deploy();
    }
  }

  /**
   * @param sshUtil used for commands that need to use ssh
   * @param ip ip of new node
   * @param rmiRegistryPort new node's local rmi registry port
   * @param childSuperuserUsername new super user username {@link
   *     Constants#PATHSTORE_SUPERUSER_USERNAME}
   * @param childSuperuserPassword new super user password
   * @param networkAdminUsername network admin username
   * @param networkAdminPassword network admin password
   * @param finalizeRootInstallation finalization object to occur at the end of deployment
   * @return list of deployment commands to execute
   */
  private List<ICommand> initList(
      final SSHUtil sshUtil,
      final String ip,
      final int rmiRegistryPort,
      final String childSuperuserUsername,
      final String childSuperuserPassword,
      final String networkAdminUsername,
      final String networkAdminPassword,
      final FinalizeRootInstallation finalizeRootInstallation) {

    String childDaemonUsername = Constants.PATHSTORE_DAEMON_USERNAME;
    String childDaemonPassword = AuthenticationUtil.generateAlphaNumericPassword();

    return new BootstrapDeploymentBuilder(sshUtil)
        .initBootstrap()
        .init()
        .createRemoteDirectory(DeploymentConstants.REMOTE_PATHSTORE_LOGS_SUB_DIR)
        .createRemoteDirectory(
            BootstrapDeploymentConstants.REMOTE_DIRECTORIES_AND_FILES
                .REMOTE_PATHSTORE_ADMIN_PANEL_SUB_DIR)
        .copyAndLoad(this.cassandraTar, DeploymentConstants.COPY_AND_LOAD.REMOTE_CASSANDRA_TAR)
        .copyAndLoad(this.pathstoreTar, DeploymentConstants.COPY_AND_LOAD.REMOTE_PATHSTORE_TAR)
        .copyAndLoad(
            this.pathstoreAdminPanelTar,
            BootstrapDeploymentConstants.REMOTE_DIRECTORIES_AND_FILES
                .REMOTE_PATHSTORE_ADMIN_PANEL_TAR)
        .generatePropertiesFiles(
            1,
            ip,
            -1,
            Role.ROOTSERVER,
            "127.0.0.1",
            rmiRegistryPort,
            "127.0.0.1",
            rmiRegistryPort,
            "127.0.0.1",
            cassandraPort,
            "127.0.0.1",
            cassandraPort,
            BootstrapDeploymentConstants.LOCAL_TEMP_PROPERTIES_FILE,
            DeploymentConstants.GENERATE_PROPERTIES.REMOTE_PATHSTORE_PROPERTIES_FILE,
            childSuperuserUsername,
            childSuperuserPassword)
        .generateWebsiteProperties(
            "127.0.0.1",
            cassandraPort,
            rmiRegistryPort,
            childSuperuserUsername,
            childSuperuserPassword)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.CASSANDRA_RUN, new WaitForCassandra(ip, cassandraPort))
        .createRole(
            Constants.DEFAULT_CASSANDRA_USERNAME,
            Constants.DEFAULT_CASSANDRA_PASSWORD,
            ip,
            cassandraPort,
            childSuperuserUsername,
            childSuperuserPassword,
            true)
        .dropRole(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            Constants.DEFAULT_CASSANDRA_USERNAME)
        .loadKeyspace(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            PathStoreSchemaLoaderUtils::loadApplicationSchema,
            Constants.PATHSTORE_APPLICATIONS)
        .createRole(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            childDaemonUsername,
            childDaemonPassword,
            false)
        .grantReadAndWriteAccess(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            childDaemonUsername,
            Constants.PATHSTORE_APPLICATIONS)
        .createRole(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            networkAdminUsername,
            networkAdminPassword,
            true)
        .writeCredentialsToRootNodeBootstrap(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            1,
            childDaemonUsername,
            childDaemonPassword)
        .writeCredentialsToRootNodeBootstrap(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            -1,
            networkAdminUsername,
            networkAdminPassword)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.PATHSTORE_RUN,
            new WaitForPathStore(childSuperuserUsername, childSuperuserPassword, ip, cassandraPort))
        .startImageAndWait(
            BootstrapDeploymentConstants.RUN_COMMANDS.PATHSTORE_ADMIN_PANEL_RUN, null)
        .custom(finalizeRootInstallation)
        .build();
  }

  /**
   * This function is used at the end or on shutdown to cleanup the local image tars. As if these
   * aren't cleaned up and this runs again, it will rebuild the image with a copy of the previous
   * image which will exponentially make the image size larger.
   */
  private void cleanUp() {
    new DevelopmentBuilder()
        .execute(DELETE_TAR_TAG, this.cassandraTar, DELETE_TAR(this.cassandraTar), -1)
        .execute(DELETE_TAR_TAG, this.pathstoreTar, DELETE_TAR(this.pathstoreTar), -1)
        .execute(
            DELETE_TAR_TAG,
            this.pathstoreAdminPanelTar,
            DELETE_TAR(this.pathstoreAdminPanelTar),
            -1)
        .build();
  }
}

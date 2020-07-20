package pathstorestartup;

import com.jcraft.jsch.JSchException;
import org.apache.commons.text.RandomStringGenerator;
import pathstore.authentication.Credential;
import pathstore.common.Constants;
import pathstore.common.Role;
import pathstore.system.deployment.commands.*;
import pathstore.system.deployment.utilities.DeploymentConstants;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils;
import pathstorestartup.commands.FinalizeRootInstallation;
import pathstorestartup.constants.BootstrapDeploymentConstants;
import pathstorestartup.constants.BootstrapDeploymentBuilder;

import java.util.List;
import java.util.Scanner;

import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;
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
        .execute(MVN_PACKAGE_TAG, pathstoreAdminPanelPath, MVN_PACKAGE(pathstoreAdminPanelPath), 0)
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

    // remove local tars after deployment is finished
    new DevelopmentBuilder()
        .execute(DELETE_TAR_TAG, this.cassandraTar, DELETE_TAR(this.cassandraTar), 0)
        .execute(DELETE_TAR_TAG, this.pathstoreTar, DELETE_TAR(this.pathstoreTar), 0)
        .execute(
            DELETE_TAR_TAG, this.pathstoreAdminPanelTar, DELETE_TAR(this.pathstoreAdminPanelTar), 0)
        .build();
  }

  /**
   * This function will prompt the user for the connection information to a server and ask for the
   * rmi port to start the root server with
   *
   * @see #initList(SSHUtil, String, int, String, String, FinalizeRootInstallation)
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
    String password =
        Utils.askQuestionWithInvalidResponse(
            this.scanner, BootstrapDeploymentConstants.PASSWORD_PROMPT, null);
    int sshPort =
        Utils.askQuestionWithInvalidResponseInteger(
            this.scanner, BootstrapDeploymentConstants.SSH_PORT_PROMPT, null);
    int rmiPort =
        Utils.askQuestionWithInvalidResponseInteger(
            this.scanner, BootstrapDeploymentConstants.RMI_PORT_PROMPT, null);

    try {
      SSHUtil sshUtil = new SSHUtil(ip, username, password, sshPort);
      System.out.println("Connected");

      String childSuperuserUsername = Constants.PATHSTORE_SUPERUSER_USERNAME;
      String childSuperuserPassword =
          new RandomStringGenerator.Builder()
              .withinRange('0', 'z')
              .filteredBy(LETTERS, DIGITS)
              .build()
              .generate(10);

      try {
        // Execute all commands in the given list
        for (ICommand command :
            this.initList(
                sshUtil,
                ip,
                rmiPort,
                childSuperuserUsername,
                childSuperuserPassword,
                new FinalizeRootInstallation(
                    childSuperuserUsername,
                    childSuperuserPassword,
                    ip,
                    cassandraPort,
                    username,
                    password,
                    sshPort,
                    rmiPort))) {
          System.out.println(command);
          command.execute();
        }
      } catch (CommandError error) {
        System.err.println(error.errorMessage);
        System.exit(-1);
      } finally {
        sshUtil.disconnect();
      }

    } catch (JSchException e) {
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
   * @param finalizeRootInstallation finalization object to occur at the end of deployment
   * @return list of deployment commands to execute
   */
  private List<ICommand> initList(
      final SSHUtil sshUtil,
      final String ip,
      final int rmiRegistryPort,
      final String childSuperuserUsername,
      final String childSuperuserPassword,
      final FinalizeRootInstallation finalizeRootInstallation) {

    String childDaemonUsername = Constants.PATHSTORE_DAEMON_USERNAME;
    String childDaemonPassword =
        new RandomStringGenerator.Builder()
            .withinRange('0', 'z')
            .filteredBy(LETTERS, DIGITS)
            .build()
            .generate(10);

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
            ip, cassandraPort, rmiRegistryPort, childSuperuserUsername, childSuperuserPassword)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.CASSANDRA_RUN, new WaitForCassandra(ip, cassandraPort))
        .createSuperUserAccount(childSuperuserUsername, childSuperuserPassword, ip, cassandraPort)
        .loadKeyspace(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            PathStoreSchemaLoaderUtils::loadLocalKeyspace,
            "local_keyspace")
        .loadKeyspace(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            PathStoreSchemaLoaderUtils::loadApplicationSchema,
            Constants.PATHSTORE_APPLICATIONS)
        .createDaemonAccount(
            childSuperuserUsername,
            childSuperuserPassword,
            ip,
            cassandraPort,
            childDaemonUsername,
            childDaemonPassword)
        .writeCredentialsToChildNode(
            new Credential(1, childDaemonUsername, childDaemonPassword),
            childSuperuserUsername,
            childDaemonPassword,
            ip,
            cassandraPort)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.PATHSTORE_RUN,
            new WaitForPathStore(childSuperuserUsername, childSuperuserPassword, ip, cassandraPort))
        .startImageAndWait(
            BootstrapDeploymentConstants.RUN_COMMANDS.PATHSTORE_ADMIN_PANEL_RUN, null)
        .custom(finalizeRootInstallation)
        .build();
  }
}

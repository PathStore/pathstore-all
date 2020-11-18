package pathstorestartup;

import com.jcraft.jsch.JSchException;
import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.authentication.credentials.AuxiliaryCredential;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.authentication.credentials.NodeCredential;
import pathstore.common.Constants;
import pathstore.common.Role;
import pathstore.common.tables.ServerIdentity;
import pathstore.system.deployment.commands.ICommand;
import pathstore.system.deployment.commands.WaitForCassandra;
import pathstore.system.deployment.commands.WaitForPathStore;
import pathstore.system.deployment.utilities.DeploymentConstants;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils;
import pathstorestartup.commands.FinalizeRootInstallation;
import pathstorestartup.constants.BootstrapDeploymentBuilder;
import pathstorestartup.constants.BootstrapDeploymentConstants;

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
  }

  /**
   * This function will prompt the user for the connection information to a server and ask for the
   * grpc port to start the root server with
   *
   * @see #initList(SSHUtil, int, DeploymentCredential, AuxiliaryCredential, String,
   *     FinalizeRootInstallation)
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
    int grpcPort =
        Utils.askQuestionWithInvalidResponseInteger(
            this.scanner, BootstrapDeploymentConstants.GRPC_PROMPT_PORT, null);
    String networkAdminUsername =
        Utils.askQuestionWithInvalidResponse(
            this.scanner, BootstrapDeploymentConstants.NETWORK_ADMIN_USERNAME_PROMPT, null);
    String networkAdminPassword =
        Utils.askQuestionWithInvalidResponse(
            this.scanner, BootstrapDeploymentConstants.NETWORK_ADMIN_PASSWORD_PROMPT, null);

    AuxiliaryCredential networkAdministratorAccount =
        new AuxiliaryCredential(
            Constants.AUXILIARY_ACCOUNTS.NETWORK_ADMINISTRATOR,
            networkAdminUsername,
            networkAdminPassword);

    try {
      SSHUtil sshUtil;

      byte[] privKey = null;

      if (authType.equals(BootstrapDeploymentConstants.AUTH_TYPES.PASSWORD))
        sshUtil = new SSHUtil(ip, username, password, sshPort);
      else {
        privKey = Files.readAllBytes(new File(privKeyPath).toPath());
        sshUtil =
            new SSHUtil(
                ip,
                username,
                sshPort,
                privKey,
                passphrase.trim().length() == 0 ? null : passphrase);
      }

      System.out.println("Connected");

      DeploymentCredential childSuperUserCredential =
          new DeploymentCredential(
              Constants.PATHSTORE_SUPERUSER_USERNAME,
              CassandraAuthenticationUtil.generateAlphaNumericPassword(),
              ip,
              cassandraPort);

      String masterPassword = CassandraAuthenticationUtil.generateAlphaNumericPassword();

      try {
        // Execute all commands in the given list
        for (ICommand command :
            this.initList(
                sshUtil,
                grpcPort,
                childSuperUserCredential,
                networkAdministratorAccount,
                masterPassword,
                new FinalizeRootInstallation(
                    childSuperUserCredential,
                    username,
                    authType,
                    authType.equals(BootstrapDeploymentConstants.AUTH_TYPES.PASSWORD)
                        ? null
                        : new ServerIdentity(privKey, passphrase),
                    password,
                    masterPassword,
                    sshPort,
                    grpcPort))) {
          System.out.println(command);
          command.execute();
        }

        System.exit(1);
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
   * @param grpcPort new node's local grpc registry port
   * @param childSuperUserCredentials super user credentials for the root node
   * @param networkAdministratorCredential network administrator account
   * @param masterPassword master password for the pathstore_applications table
   * @param finalizeRootInstallation finalization object to occur at the end of deployment
   * @return list of deployment commands to execute
   */
  private List<ICommand> initList(
      final SSHUtil sshUtil,
      final int grpcPort,
      final DeploymentCredential childSuperUserCredentials,
      final AuxiliaryCredential networkAdministratorCredential,
      final String masterPassword,
      final FinalizeRootInstallation finalizeRootInstallation) {

    DeploymentCredential defaultLogin =
        new DeploymentCredential(
            Constants.DEFAULT_CASSANDRA_USERNAME,
            Constants.DEFAULT_CASSANDRA_PASSWORD,
            childSuperUserCredentials.getIp(),
            cassandraPort);

    NodeCredential childDaemonCredential =
        new NodeCredential(
            1,
            Constants.PATHSTORE_DAEMON_USERNAME,
            CassandraAuthenticationUtil.generateAlphaNumericPassword());

    AuxiliaryCredential networkWideGrpcCredential =
        new AuxiliaryCredential(
            Constants.AUXILIARY_ACCOUNTS.NETWORK_WIDE_GRPC_CREDENTIAL,
            CassandraAuthenticationUtil.generateAlphaNumericPassword(),
            CassandraAuthenticationUtil.generateAlphaNumericPassword());

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
            childSuperUserCredentials.getIp(),
            -1,
            Role.ROOTSERVER,
            "127.0.0.1",
            grpcPort,
            "127.0.0.1",
            grpcPort,
            "127.0.0.1",
            cassandraPort,
            "127.0.0.1",
            cassandraPort,
            BootstrapDeploymentConstants.LOCAL_TEMP_PROPERTIES_FILE,
            DeploymentConstants.GENERATE_PROPERTIES.REMOTE_PATHSTORE_PROPERTIES_FILE,
            childSuperUserCredentials.getUsername(),
            childSuperUserCredentials.getPassword())
        .generateWebsiteProperties("127.0.0.1", cassandraPort, grpcPort, masterPassword)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.CASSANDRA_RUN, new WaitForCassandra(defaultLogin))
        .createRole(defaultLogin, childSuperUserCredentials, true)
        .dropRole(childSuperUserCredentials, Constants.DEFAULT_CASSANDRA_USERNAME)
        .loadKeyspace(
            childSuperUserCredentials,
            PathStoreSchemaLoaderUtils::loadApplicationSchema,
            Constants.PATHSTORE_APPLICATIONS)
        .createRole(childSuperUserCredentials, childDaemonCredential, false)
        .grantReadAndWriteAccess(
            childSuperUserCredentials,
            childDaemonCredential.getUsername(),
            Constants.PATHSTORE_APPLICATIONS)
        .createRole(childSuperUserCredentials, networkAdministratorCredential, true)
        .writeNodeCredentialToChildNode(
            childDaemonCredential, childSuperUserCredentials) // write root node daemon account
        .writeAuxiliaryCredentialToChildNode( // write network administrator account to root
            networkAdministratorCredential, childSuperUserCredentials)
        .writeAuxiliaryCredentialToChildNode( // write network wide grpc account to root
            networkWideGrpcCredential, childSuperUserCredentials)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.PATHSTORE_RUN,
            new WaitForPathStore(childSuperUserCredentials))
        .custom(finalizeRootInstallation)
        .startImageAndWait(
            BootstrapDeploymentConstants.RUN_COMMANDS.PATHSTORE_ADMIN_PANEL_RUN, null)
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

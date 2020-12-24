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
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static pathstorestartup.constants.BootstrapDeploymentConstants.DEVELOPMENT_BUILDER.*;

/**
 * This represents the development deployment process
 *
 * <p>Setup build stuff -> get connection information -> create registry -> upload stuff -> perform
 * normal setup
 *
 * <p>Only part of normal setup that needs to be changed is that copy and load should perform some
 * pull from the created registry
 */
public class DevelopmentDeployment {

  /** static cassandra port todo: change */
  private static final int cassandraPort = 9052;

  /** Represents how to receive information from the user */
  private final Scanner scanner;

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

    // build cassandra
    new DevelopmentBuilder()
        .execute(
            BUILDING_IMAGE_TAG,
            DeploymentConstants.CASSANDRA,
            BUILD_IMAGE(DeploymentConstants.CASSANDRA, cassandraPath),
            0)
        .build();

    // build pathstore
    new DevelopmentBuilder()
        .execute(
            BUILDING_IMAGE_TAG,
            DeploymentConstants.PATHSTORE,
            BUILD_IMAGE(DeploymentConstants.PATHSTORE, pathstorePath),
            0)
        .build();

    // build pathstore-admin-panel
    new DevelopmentBuilder()
        .execute(
            BUILDING_IMAGE_TAG,
            BootstrapDeploymentConstants.PATHSTORE_ADMIN_PANEL,
            BUILD_IMAGE(
                BootstrapDeploymentConstants.PATHSTORE_ADMIN_PANEL, pathstoreAdminPanelPath),
            0)
        .build();

    // deploy the built images to a server
    this.deploy();
  }

  /**
   * This function will prompt the user for the connection information to a server and ask for the
   * grpc port to start the root server with
   *
   * @see #initList(SSHUtil, int, DeploymentCredential, AuxiliaryCredential, String, String,
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

    String pathstoreVersion =
        Utils.askQuestionWithInvalidResponse(
            this.scanner, BootstrapDeploymentConstants.PATHSTORE_VERSION, null);

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

      // remove all certs before hand
      this.cleanUp(ip);

      // add shutdown hook to clean up registry certs.d
      Runtime.getRuntime().addShutdownHook(new Thread(() -> this.cleanUp(ip)));

      try {
        // Execute all commands in the given list
        for (ICommand command :
            this.initList(
                sshUtil,
                grpcPort,
                childSuperUserCredential,
                networkAdministratorAccount,
                masterPassword,
                pathstoreVersion,
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
   * @param pathstoreVersion version of pathstore
   * @param finalizeRootInstallation finalization object to occur at the end of deployment
   * @return list of deployment commands to execute
   */
  private List<ICommand> initList(
      final SSHUtil sshUtil,
      final int grpcPort,
      final DeploymentCredential childSuperUserCredentials,
      final AuxiliaryCredential networkAdministratorCredential,
      final String masterPassword,
      final String pathstoreVersion,
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
        .mkcertSetup(childSuperUserCredentials.getIp())
        .init(childSuperUserCredentials.getIp())
        .bootstrapInit()
        .createRemoteDirectory(DeploymentConstants.REMOTE_PATHSTORE_LOGS_SUB_DIR)
        .createRemoteDirectory(
            BootstrapDeploymentConstants.REMOTE_DIRECTORIES_AND_FILES
                .REMOTE_PATHSTORE_ADMIN_PANEL_SUB_DIR)
        .copyRegistryCertsTo(childSuperUserCredentials.getIp())
        .createDockerRegistry(childSuperUserCredentials.getIp())
        .pushToRegistry(DeploymentConstants.CASSANDRA, childSuperUserCredentials.getIp(), "latest")
        .pushToRegistry(
            DeploymentConstants.PATHSTORE, childSuperUserCredentials.getIp(), pathstoreVersion)
        .pushToRegistry(
            BootstrapDeploymentConstants.PATHSTORE_ADMIN_PANEL,
            childSuperUserCredentials.getIp(),
            "latest")
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
            childSuperUserCredentials.getUsername(),
            childSuperUserCredentials.getPassword(),
            childSuperUserCredentials.getIp(),
            pathstoreVersion,
            BootstrapDeploymentConstants.LOCAL_TEMP_PROPERTIES_FILE,
            DeploymentConstants.GENERATE_PROPERTIES.REMOTE_PATHSTORE_PROPERTIES_FILE)
        .generateWebsiteProperties("127.0.0.1", cassandraPort, grpcPort, masterPassword)
        .startImageAndWait(
            DeploymentConstants.RUN_COMMANDS.CASSANDRA_REMOVE_TAG(
                childSuperUserCredentials.getIp()),
            DeploymentConstants.RUN_COMMANDS.CASSANDRA_RUN(childSuperUserCredentials.getIp()),
            new WaitForCassandra(defaultLogin))
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
            DeploymentConstants.RUN_COMMANDS.PATHSTORE_REMOVE_TAG(
                childSuperUserCredentials.getIp(), pathstoreVersion),
            DeploymentConstants.RUN_COMMANDS.PATHSTORE_RUN(
                childSuperUserCredentials.getIp(), pathstoreVersion),
            new WaitForPathStore(childSuperUserCredentials))
        .custom(finalizeRootInstallation)
        .startImageAndWait(
            BootstrapDeploymentConstants.RUN_COMMANDS.PATHSTORE_ADMIN_PANEL_REMOVE_TAG(
                childSuperUserCredentials.getIp()),
            BootstrapDeploymentConstants.RUN_COMMANDS.PATHSTORE_ADMIN_PANEL_RUN(
                childSuperUserCredentials.getIp()),
            null)
        .build();
  }

  /**
   * This function is used at the start and end to remove any dangling certificates from the system.
   *
   * @param registryIP registry ip to remove dir for
   */
  private void cleanUp(final String registryIP) {
    String dir = String.format("/etc/docker/certs.d/%s", registryIP);

    new DevelopmentBuilder()
        .execute("Remove certs", dir, Arrays.asList("rm", "-rf", dir), 0)
        .build();
  }
}

package pathstorestartup.constants;

import pathstore.system.deployment.commands.Exec;
import pathstore.system.deployment.commands.FileTransfer;
import pathstore.system.deployment.commands.RemoveGeneratedPropertiesFile;
import pathstore.system.deployment.utilities.DeploymentBuilder;
import pathstore.system.deployment.utilities.DeploymentConstants;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstorestartup.commands.CreateWebsitePropertiesFile;

/**
 * Deployment functions specific to the startup utility
 *
 * @see pathstore.system.deployment.utilities.DeploymentBuilder
 */
public class BootstrapDeploymentBuilder extends DeploymentBuilder<BootstrapDeploymentBuilder> {

  /** @param remoteHostConnect root node connection */
  public BootstrapDeploymentBuilder(final SSHUtil remoteHostConnect) {
    super(remoteHostConnect);
  }

  /**
   * This function is used to remove all remote references to the admin panel. This should be used
   * ontop of {@link DeploymentBuilder#init(String)}
   */
  public BootstrapDeploymentBuilder bootstrapInit() {
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.INIT_BOOTSTRAP_COMMANDS.KILL_PATHSTORE_ADMIN_PANEL,
            -1));

    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.INIT_BOOTSTRAP_COMMANDS.REMOVE_PATHSTORE_ADMIN_PANEL,
            -1));

    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.INIT_BOOTSTRAP_COMMANDS.KILL_PATHSTORE_REGISTRY,
            -1));

    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.INIT_BOOTSTRAP_COMMANDS.REMOVE_PATHSTORE_REGISTRY,
            -1));

    return this;
  }

  /**
   * TODO: Remove downloading from github. Either integrate into the codebase or re-write
   *
   * <p>This function is used to download the mkcert program and generate the certs for the docker
   * registry
   *
   * @return this
   */
  public BootstrapDeploymentBuilder mkcertSetup(final String registryIP) {

    // clone mkcert
    this.commands.add(
        new LocalCommand(
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.DOWNLOAD_MKCERT,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.ENTRY_DOWNLOAD_MKCERT,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.EXIT_DOWNLOAD_MKCERT,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.ERROR_DOWNLOAD_MKCERT,
            0));

    // allow for execution
    this.commands.add(
        new LocalCommand(
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.CHANGE_MKCERT_ACCESS,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.ENTRY_CHANGE_MKCERT_ACCESS,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.EXIT_CHANGE_MKCERT_ACCESS,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.ERROR_CHANGE_MKCERT_ACCESS,
            0));

    // generate certs for pathstore registry
    this.commands.add(
        new LocalCommand(
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.GENERATE_REGISTRY_CERTIFICATES(
                registryIP),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .ENTRY_GENERATE_REGISTRY_CERTIFICATES,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .EXIT_GENERATE_REGISTRY_CERTIFICATES,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .ERROR_GENERATE_REGISTRY_CERTIFICATES,
            0));

    // remove mkcert from local fs
    this.removeLocalFile(BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.MKCERT_NAME);

    // create pathstore registry cert dir on local machine
    this.commands.add(
        new LocalCommand(
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .CREATE_DOCKER_REGISTRY_CERT_DIR(registryIP),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .ENTRY_CREATE_DOCKER_REGISTRY_CERT_DIR(registryIP),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .EXIT_CREATE_DOCKER_REGISTRY_CERT_DIR(registryIP),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .ERROR_CREATE_DOCKER_REGISTRY_CERT_DIR(registryIP),
            0));

    // move cert from generation dir to docker certs dir
    this.commands.add(
        new LocalCommand(
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .COPY_DOCKER_REGISTRY_CERT_TO_DIR(registryIP),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .ENTRY_COPY_DOCKER_REGISTRY_CERT_TO_DIR(registryIP),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .EXIT_COPY_DOCKER_REGISTRY_CERT_TO_DIR(registryIP),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .ERROR_COPY_DOCKER_REGISTRY_CERT_TO_DIR(registryIP),
            0));

    // set the docker group as the group for the docker certs dir
    this.commands.add(
        new LocalCommand(
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIR(registryIP),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .ENTRY_CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIR,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .EXIT_CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIR,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .ERROR_CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIR,
            0));

    // set the permissions to 775 from 755. We need group permissions to modify the dir in the
    // future.
    this.commands.add(
        new LocalCommand(
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIR(registryIP),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .ENTRY_CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIR,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .EXIT_CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIR,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .ERROR_CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIR,
            0));

    return this;
  }

  /**
   * This function is used to move the locally generated certificate onto the root node
   *
   * @param registryIP registry ip
   * @return this
   */
  public BootstrapDeploymentBuilder copyRegistryCertsTo(final String registryIP) {

    // transfer cert over
    this.commands.add(
        new FileTransfer(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.LOCAL_CERT_NAME(registryIP),
            DeploymentConstants.REMOTE_DOCKER_REGISTRY_CERT_LOCATION));

    this.removeLocalFile(
        BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.LOCAL_CERT_NAME(registryIP));

    // transfer key over
    this.commands.add(
        new FileTransfer(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.LOCAL_KEY_NAME(registryIP),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE
                .REMOTE_DOCKER_REGISTRY_KEY_LOCATION));

    this.removeLocalFile(
        BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.LOCAL_KEY_NAME(registryIP));

    return this;
  }

  /**
   * This function will remove some file from the local machine
   *
   * @param fileToRemove file to remove
   */
  private void removeLocalFile(final String fileToRemove) {
    this.commands.add(
        new LocalCommand(
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.REMOVE_LOCAL_FILE(
                fileToRemove),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.ENTRY_REMOVE_LOCAL_FILE(
                fileToRemove),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.EXIT_REMOVE_LOCAL_FILE(
                fileToRemove),
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.ERROR_REMOVE_LOCAL_FILE(
                fileToRemove),
            0));
  }

  /**
   * This function is used to create a docker registry on the root node.
   *
   * <p>It will start the registry and create the needed directories on the root node to allow for
   * the root node to push and pull docker images from the registry
   *
   * @param registryIP registry ip
   * @return this
   */
  public BootstrapDeploymentBuilder createDockerRegistry(final String registryIP) {

    // start the registry
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.DOCKER_REGISTRY_START,
            0));

    // create certs directory
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            DeploymentConstants.CREATE_LOCAL_DOCKER_REGISTRY_CERT_DIR(registryIP),
            0));

    // copy cert into directory
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            DeploymentConstants.COPY_FROM_REMOTE_TO_LOCAL_DOCKER_REGISTRY_CERT(registryIP),
            0));

    // set dir group
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            DeploymentConstants.CHANGE_GROUP_OF_LOCAL_DOCKER_REGISTRY_DIRECTORY(registryIP),
            0));

    // set dir permissions
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            DeploymentConstants.CHANGE_PERMISSIONS_OF_LOCAL_DOCKER_REGISTRY_DIRECTORY(registryIP),
            0));

    return this;
  }

  /**
   * This function is used to tag and push a docker image to the newly created private registry
   *
   * @param imageName image name to push
   * @param registryIP registry ip
   * @param version version of the image
   * @return this
   */
  public BootstrapDeploymentBuilder pushToRegistry(
      final String imageName, final String registryIP, final String version) {

    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.DOCKER_TAG(
                imageName, registryIP, version),
            0));

    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.DOCKER_REGISTRY_CERTIFICATE.DOCKER_PUSH(
                imageName, registryIP, version),
            0));

    return this;
  }

  /**
   * This function is used to generate the properties file for the website, transfer it, then delete
   * the local copy
   *
   * @param ip ip of the root node
   * @param cassandraPort cassandra port of the root node
   * @param grpcPort grpc port of the root node
   * @param password pathstore_applications master password
   */
  public BootstrapDeploymentBuilder generateWebsiteProperties(
      final String ip, final int cassandraPort, final int grpcPort, final String password) {
    this.commands.add(
        new CreateWebsitePropertiesFile(
            ip,
            cassandraPort,
            grpcPort,
            BootstrapDeploymentConstants.LOCAL_TEMP_PROPERTIES_FILE,
            password));

    this.commands.add(
        new FileTransfer(
            this.remoteHostConnect,
            BootstrapDeploymentConstants.LOCAL_TEMP_PROPERTIES_FILE,
            BootstrapDeploymentConstants.REMOTE_DIRECTORIES_AND_FILES
                .REMOTE_PATHSTORE_ADMIN_PANEL_PROPERTIES_FILE));

    this.commands.add(
        new RemoveGeneratedPropertiesFile(BootstrapDeploymentConstants.LOCAL_TEMP_PROPERTIES_FILE));

    return this;
  }
}

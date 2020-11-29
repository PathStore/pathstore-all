package pathstorestartup.constants;

import pathstore.system.deployment.commands.Exec;
import pathstore.system.deployment.commands.FileTransfer;
import pathstore.system.deployment.commands.RemoveGeneratedPropertiesFile;
import pathstore.system.deployment.utilities.DeploymentBuilder;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstorestartup.commands.CreateWebsitePropertiesFile;

import java.util.Arrays;

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
   * ontop of {@link DeploymentBuilder#init()}
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
   * This function is used to download the mkcert program and generate the certs for the docker
   * registry
   *
   * @return this
   */
  public BootstrapDeploymentBuilder mkcertSetup(final String registryIP) {
    this.commands.add(
        new LocalCommand(
            Arrays.asList(
                "wget",
                "https://github.com/FiloSottile/mkcert/releases/download/v1.4.3/mkcert-v1.4.3-linux-amd64"),
            "Downloading mkcert-v1.4.3-linux-amd64",
            "Finished downloading mkcert-v1.4.3-linux-amd64",
            "Error download mkcert-v1.4.3-linux-amd64",
            0));

    this.commands.add(
        new LocalCommand(
            Arrays.asList("chmod", "u+rtx", "mkcert-v1.4.3-linux-amd64"),
            "Granting executable permissions for mkcert-v1.4.3-linux-amd64",
            "Finished updating permissions for mkcert-v1.4.3-linux-amd64",
            "Error updating permissions for mkcert-v1.4.3-linux-amd64",
            0));

    this.commands.add(
        new LocalCommand(
            Arrays.asList("./mkcert-v1.4.3-linux-amd64", registryIP),
            "Creating pathstore-registry self signed certificates",
            "Finished creating pathstore-registry self signed certificates",
            "Error creating pathstore-registry self signed certificates",
            0));

    String dir = String.format("/etc/docker/certs.d/%s", registryIP);

    this.commands.add(
        new LocalCommand(
            Arrays.asList("mkdir", "-p", dir),
            String.format("Creating %s dir", dir),
            String.format("Created %s dir", dir),
            String.format("Error creating %s", dir),
            0));

    String localcert = String.format("%s/ca.crt", dir);

    this.commands.add(
        new LocalCommand(
            Arrays.asList("cp", String.format("%s.pem", registryIP), localcert),
            String.format("Setting up local registry cert at %s", localcert),
            String.format("Set up local registry cert at %s", localcert),
            String.format("Error setting up local registry cert at %s", localcert),
            0));

    this.commands.add(
        new LocalCommand(
            Arrays.asList("chgrp", "docker", "-R", dir),
            "Setting dir ownership",
            "Set dir ownership",
            "Error setting dir ownership",
            0));

    this.commands.add(
        new LocalCommand(
            Arrays.asList("chmod", "775", "-R", dir),
            "Setting dir permissions",
            "Set dir permission",
            "Error setting dir permission",
            0));

    return this;
  }

  public BootstrapDeploymentBuilder copyRegistryCertsTo(final String registryIP) {

    String cert = String.format("%s.pem", registryIP);

    this.commands.add(new FileTransfer(this.remoteHostConnect, cert, cert));

    String key = String.format("%s-key.pem", registryIP);

    this.commands.add(new FileTransfer(this.remoteHostConnect, key, key));

    return this;
  }

  public BootstrapDeploymentBuilder createDockerRegistry(final String registryIP) {

    // move certificate to proper name
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            String.format(
                "mv ~/%s.pem ~/pathstore-install/pathstore-registry/pathstore-registry.crt",
                registryIP),
            0));

    // move key to proper name
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            String.format(
                "mv ~/%s-key.pem ~/pathstore-install/pathstore-registry/pathstore-registry.key",
                registryIP),
            0));

    // start the registry
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            "docker run -d --restart=always --name pathstore-registry -v \"$(pwd)\"/pathstore-install/pathstore-registry:/certs -e REGISTRY_HTTP_ADDR=0.0.0.0:443 -e REGISTRY_HTTP_TLS_CERTIFICATE=/certs/pathstore-registry.crt -e REGISTRY_HTTP_TLS_KEY=/certs/pathstore-registry.key -p 443:443 registry:2",
            0));

    String dir = String.format("/etc/docker/certs.d/%s", registryIP);

    // create certs directory
    this.commands.add(new Exec(this.remoteHostConnect, String.format("mkdir -p %s", dir), 0));

    // copy cert into directory
    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            String.format(
                "cp ~/pathstore-install/pathstore-registry/pathstore-registry.crt /etc/docker/certs.d/%s/ca.crt",
                registryIP),
            0));

    // set dir group
    this.commands.add(
        new Exec(this.remoteHostConnect, String.format("chgrp docker -R %s", dir), 0));

    // set dir permissions
    this.commands.add(new Exec(this.remoteHostConnect, String.format("chmod 775 -R %s", dir), 0));

    return this;
  }

  public BootstrapDeploymentBuilder pushToRegistry(final String imageName, final String registryIP) {

    this.commands.add(
        new Exec(
            this.remoteHostConnect,
            String.format("docker tag %s %s/%s", imageName, registryIP, imageName),
            0));

    this.commands.add(
        new Exec(this.remoteHostConnect, String.format("docker push %s/%s", registryIP, imageName), 0));

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

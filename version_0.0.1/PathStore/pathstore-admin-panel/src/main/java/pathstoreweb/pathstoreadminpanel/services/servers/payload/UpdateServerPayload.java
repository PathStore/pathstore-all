package pathstoreweb.pathstoreadminpanel.services.servers.payload;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.jcraft.jsch.JSchException;
import org.springframework.web.multipart.MultipartFile;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.tables.ServerAuthType;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstoreweb.pathstoreadminpanel.services.servers.Server;
import pathstoreweb.pathstoreadminpanel.validator.ValidatedPayload;

import java.io.IOException;
import java.util.UUID;

import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.UPDATE_SERVER_PAYLOAD.*;

/**
 * This payload is used when a user makes a request to add a server to the {@link
 * pathstore.common.Constants#SERVERS} table
 */
public final class UpdateServerPayload extends ValidatedPayload {

  /**
   * Store an instance of the server class, check to see if the information allows you to connect to
   * a server
   */
  public final Server server;

  /** Private key (if applicable) */
  private MultipartFile privateKey;

  /**
   * @param server_uuid server UUID to modify
   * @param ip ip of server
   * @param username username to connect
   * @param password password to connect
   * @param ssh_port what port ssh is running on
   * @param rmi_port what rmi port you want that server to use
   * @param name what is the human readable name
   */
  public UpdateServerPayload(
      final String server_uuid,
      final String ip,
      final String username,
      final String auth_type,
      final String passphrase,
      final String password,
      final int ssh_port,
      final int rmi_port,
      final String name) {
    this.server =
        new Server(
            UUID.fromString(server_uuid),
            ip,
            username,
            auth_type,
            passphrase,
            password,
            ssh_port,
            rmi_port,
            name);
  }

  /** @param privateKey set {@link #privateKey} to this */
  public void setPrivateKey(final MultipartFile privateKey) {
    this.privateKey = privateKey;
  }

  /** @return {@link #privateKey} */
  public MultipartFile getPrivateKey() {
    return this.privateKey;
  }

  /**
   * TODO: Modify once pathstore cluster has one support
   *
   * <p>Validity test function
   *
   * <p>(1) In valid submission format
   *
   * <p>(2) server UUID is valid
   *
   * <p>(3) ip does not conflict with any other records (excluding the record where serverUUID's
   * match)
   *
   * <p>(4) name does not conflict with any other records (excluding the record where serverUUID's
   * match)
   *
   * <p>(5) server UUID is not attached to a deployment node that is at any other state then {@link
   * pathstore.common.tables.DeploymentProcessStatus#DEPLOYED}
   *
   * <p>(6) Information provided is a valid server (can connect)
   *
   * @return if all values are null the validity test has passed
   */
  @Override
  protected String[] calculateErrors() {

    // (1)
    if (this.bulkNullCheck(
        this.server.serverUUID,
        this.server.ip,
        this.server.username,
        this.server.sshPort,
        this.server.rmiPort,
        this.server.name)) return new String[] {WRONG_SUBMISSION_FORMAT};

    String[] errors = {SERVER_UUID_DOESNT_EXIST, null, null, null, null, null};

    Session session = PathStoreCluster.getSuperUserInstance().connect();

    // (2) & (3) & (4)
    Select serverSelect =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);

    for (Row row : session.execute(serverSelect)) {
      String serverUUID = row.getString(Constants.SERVERS_COLUMNS.SERVER_UUID);

      // (2)
      if (serverUUID.equals(this.server.serverUUID.toString())) errors[0] = null;
      else {
        // (3)
        if (row.getString(Constants.SERVERS_COLUMNS.IP).equals(this.server.ip))
          errors[1] = IP_IS_NOT_UNIQUE;

        // (4)
        if (row.getString(Constants.SERVERS_COLUMNS.NAME).equals(this.server.name))
          errors[2] = NAME_IS_NOT_UNIQUE;
      }

      // Optimization as if all values have been set nothing will change
      if (errors[0] == null && errors[1] != null && errors[2] != null) break;
    }

    // (5)
    Select deploymentSelect =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

    for (Row row : session.execute(deploymentSelect))
      if (row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID)
          .equals(this.server.serverUUID.toString())) errors[3] = SERVER_UUID_IS_NOT_FREE;

    // (6)
    try {
      if (this.server.authType.equals(ServerAuthType.PASSWORD.toString()))
        if (this.server.password == null) errors[4] = PASSWORD_NOT_PRESENT;
        else
          new SSHUtil(
                  this.server.ip, this.server.username, this.server.password, this.server.sshPort)
              .disconnect();
      else if (this.server.authType.equals(ServerAuthType.IDENTITY.toString()))
        if (this.getPrivateKey() == null) errors[4] = PRIVATE_KEY_NOT_PRESENT;
        else
          new SSHUtil(
              this.server.ip,
              this.server.username,
              this.server.sshPort,
              this.getPrivateKey().getBytes(),
              this.server.passphrase);
    } catch (JSchException | IOException e) {
      errors[5] = CONNECTION_INFORMATION_IS_INVALID;
    }

    return errors;
  }
}

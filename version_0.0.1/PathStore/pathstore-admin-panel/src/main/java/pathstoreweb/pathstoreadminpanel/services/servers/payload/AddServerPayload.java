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

import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.ADD_SERVER_PAYLOAD.*;

import java.io.IOException;
import java.util.UUID;

/**
 * This payload is used when a user makes a request to add a server to the {@link
 * pathstore.common.Constants#SERVERS} table
 */
public final class AddServerPayload extends ValidatedPayload {

  /** Store an instance of the server class for usage in the service */
  public final Server server;

  /** Private key (if applicable) */
  private MultipartFile privateKey;

  /**
   * @param ip ip of server
   * @param username username to connect with
   * @param password password to connect with
   * @param ssh_port what port ssh is running
   * @param rmi_port what port you want rmi to use
   * @param name name of server
   */
  public AddServerPayload(
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
            UUID.randomUUID(),
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
   * Validity check for this payload:
   *
   * <p>(1): Improper submission format
   *
   * <p>(2): Ip given is unique
   *
   * <p>(3): Name given is unique
   *
   * <p>(4): Either password is present or private key is present
   *
   * <p>(5): Can connect
   *
   * @return list of errors if all null then the validity test has passed
   */
  @Override
  protected String[] calculateErrors() {

    // (1)
    if (this.bulkNullCheck(
        this.server.ip,
        this.server.username,
        this.server.sshPort,
        this.server.rmiPort,
        this.server.name)) return new String[] {WRONG_SUBMISSION_FORMAT};

    String[] errors = {null, null, null, null};

    Session session = PathStoreCluster.getSuperUserInstance().connect();

    // (2) & (3)
    Select serverSelectAll =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);

    for (Row row : session.execute(serverSelectAll)) {
      if (row.getString(Constants.SERVERS_COLUMNS.IP).equals(this.server.ip))
        errors[0] = IP_IS_NOT_UNIQUE;
      if (row.getString(Constants.SERVERS_COLUMNS.NAME).equals(this.server.name))
        errors[1] = NAME_IS_NOT_UNIQUE;

      // Optimization to break out of the loop if both failed
      if (errors[0] != null && errors[1] != null) break;
    }

    // (4)
    try {
      if (this.server.authType.equals(ServerAuthType.PASSWORD.toString()))
        if (this.server.password == null) errors[2] = PASSWORD_NOT_PRESENT;
        else
          new SSHUtil(
                  this.server.ip, this.server.username, this.server.password, this.server.sshPort)
              .disconnect();
      else if (this.server.authType.equals(ServerAuthType.IDENTITY.toString()))
        if (this.getPrivateKey() == null) errors[2] = PRIVATE_KEY_NOT_PRESENT;
        else
          new SSHUtil(
              this.server.ip,
              this.server.username,
              this.server.sshPort,
              this.getPrivateKey().getBytes(),
              this.server.passphrase);
    } catch (JSchException e) {
      errors[3] = CONNECTION_INFORMATION_IS_INVALID;
    } catch (IOException e) {
      errors[3] = "Could not read file";
    }

    return errors;
  }
}

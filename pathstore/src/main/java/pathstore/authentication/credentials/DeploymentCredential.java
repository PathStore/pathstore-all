package pathstore.authentication.credentials;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/** This class is used to denote some authentication information to connect to the child node */
@EqualsAndHashCode(callSuper = true)
public class DeploymentCredential extends NopCredential {

  /** Cassandra ip address */
  @Getter private final String ip;

  /** Cassandra port */
  @Getter private final int port;

  /**
   * @param superUserName username to connect
   * @param superUserPassword password to connect
   * @param ip ip of child node
   * @param port port of child cassandra
   */
  public DeploymentCredential(
      @NonNull final String superUserName,
      @NonNull final String superUserPassword,
      @NonNull final String ip,
      final int port) {
    super(superUserName, superUserPassword);
    this.ip = ip;
    this.port = port;
  }
}

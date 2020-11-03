package pathstore.authentication;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * Row in pathstore_applications.local_auth or pathstore_applications.local_client_auth with node_id
 * of -1 is for the network administrator account
 *
 * <p>TODO: Account 0 comments
 */
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = "primaryKey")
public final class Credential<T> {

  /**
   * Node id which the credential is associated with, if -1 then its the network administrator
   * account else its the daemon account for that specific node
   */
  public final T primaryKey;

  /** username */
  public final String username;

  /** password */
  public final String password;
}

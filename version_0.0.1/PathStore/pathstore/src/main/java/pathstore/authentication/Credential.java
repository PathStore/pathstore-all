package pathstore.authentication;

import java.util.Objects;

/**
 * Row in pathstore_applications.local_auth or pathstore_applications.local_client_auth with node_id
 * of -1 is for the network administrator account
 *
 * <p>TODO: Account 0 comments
 */
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

  /**
   * @param primaryKey {@link #primaryKey}
   * @param username {@link #username}
   * @param password {@link #password}
   */
  public Credential(final T primaryKey, final String username, final String password) {
    this.primaryKey = primaryKey;
    this.username = username;
    this.password = password;
  }

  /**
   * @param o some object
   * @return true if that object is an credential object and is equal to this credential object
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Credential<?> that = (Credential<?>) o;
    return this.primaryKey == that.primaryKey
        && this.username.equals(that.username)
        && this.password.equals(that.password);
  }

  /** @return combined hash of all internal data */
  @Override
  public int hashCode() {
    return Objects.hash(this.primaryKey, this.username, this.password);
  }
}

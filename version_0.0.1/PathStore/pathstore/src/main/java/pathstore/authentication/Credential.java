package pathstore.authentication;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.Constants;

import java.util.Objects;

/**
 * Row in pathstore_applications.local_auth or pathstore_applications.local_client_auth with node_id
 * of -1
 */
public final class Credential {

  /**
   * Node id which the credential is associated with, if -1 then its the network administrator
   * account else its the daemon account for that specific node
   */
  public final int node_id;

  /** Cassandra username */
  public final String username;

  /** Cassandra password */
  public final String password;

  /**
   * @param node_id {@link #node_id}
   * @param username {@link #username}
   * @param password {@link #password}
   */
  public Credential(final int node_id, final String username, final String password) {
    this.node_id = node_id;
    this.username = username;
    this.password = password;
  }

  /**
   * @param row row from pathstore_applications.local_auth
   * @return parsed credential object
   */
  public static Credential buildFromRow(final Row row) {
    return new Credential(
        row.getInt(Constants.LOCAL_AUTH_COLUMNS.NODE_ID),
        row.getString(Constants.LOCAL_AUTH_COLUMNS.USERNAME),
        row.getString(Constants.LOCAL_AUTH_COLUMNS.PASSWORD));
  }

  /**
   * Take a credential object and write it to a database.
   *
   * @param session session, as this may be related to the parent, a child or the local node
   * @param credential credential to write
   * @return credential object passed in.
   * @apiNote credential and session must be non-null for a write to occur. This does not stop an
   *     error from being thrown during the write, the caller must handle that if an error may occur
   */
  public static Credential writeCredentialToRow(
      final Session session, final Credential credential) {
    if (session != null && credential != null)
      session.execute(
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_AUTH)
              .value(Constants.LOCAL_AUTH_COLUMNS.NODE_ID, credential.node_id)
              .value(Constants.LOCAL_AUTH_COLUMNS.USERNAME, credential.username)
              .value(Constants.LOCAL_AUTH_COLUMNS.PASSWORD, credential.password));

    return credential;
  }

  /**
   * @param o some object
   * @return true if that object is an credential object and is equal to this credential object
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Credential that = (Credential) o;
    return this.node_id == that.node_id
        && this.username.equals(that.username)
        && this.password.equals(that.password);
  }

  /** @return combined hash of all internal data */
  @Override
  public int hashCode() {
    return Objects.hash(this.node_id, this.username, this.password);
  }
}

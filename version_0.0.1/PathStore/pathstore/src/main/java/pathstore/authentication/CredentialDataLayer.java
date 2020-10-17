package pathstore.authentication;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;

/**
 * This class is used to denote how to store a credential object in the database depending on where
 * it is generated
 *
 * @see CredentialCache#getNodeAuth() For the pathstore_applications.local_auth table
 * @see CredentialCache#getClientAuth() For the pathstore_applications.local_client_auth table
 * @param <T> Data type of primary key
 */
public class CredentialDataLayer<T> {

  /** Keyspace name */
  public final String keyspaceName;

  /** Table name */
  public final String tableName;

  /** Name of the primary column */
  private final String primaryKeyColumnName;

  /** username column name */
  private final String username;

  /** password column name */
  private final String password;

  /**
   * @param keyspaceName {@link #keyspaceName}
   * @param tableName {@link #tableName}
   * @param primaryKeyColumnName {@link #primaryKeyColumnName}
   * @param username {@link #username}
   * @param password {@link #password}
   */
  public CredentialDataLayer(
      final String keyspaceName,
      final String tableName,
      final String primaryKeyColumnName,
      final String username,
      final String password) {
    this.keyspaceName = keyspaceName;
    this.tableName = tableName;
    this.primaryKeyColumnName = primaryKeyColumnName;
    this.username = username;
    this.password = password;
  }

  /**
   * @param row row from {@link #keyspaceName}.{@link #tableName}
   * @return Credential credential parsed row
   */
  @SuppressWarnings("ALL")
  public Credential<T> buildFromRow(final Row row) {
    return new Credential<>(
        (T) row.getObject(this.primaryKeyColumnName),
        row.getString(this.username),
        row.getString(this.password));
  }

  /**
   * @param session session object to write to the database
   * @param credential credentials to write
   * @return credential object that was passed
   */
  public Credential<T> write(final Session session, final Credential<T> credential) {
    if (session != null && credential != null)
      session.execute(
          QueryBuilder.insertInto(this.keyspaceName, this.tableName)
              .value(this.primaryKeyColumnName, credential.primaryKey)
              .value(this.username, credential.username)
              .value(this.password, credential.password));
    return credential;
  }

  /**
   * @param session session object to delete to the database
   * @param credential credentials to delete
   */
  public void delete(final Session session, final Credential<T> credential) {
    if (session != null && credential != null)
      session.execute(
          QueryBuilder.delete()
              .from(this.keyspaceName, this.tableName)
              .where(QueryBuilder.eq(this.primaryKeyColumnName, credential.primaryKey)));
  }
}

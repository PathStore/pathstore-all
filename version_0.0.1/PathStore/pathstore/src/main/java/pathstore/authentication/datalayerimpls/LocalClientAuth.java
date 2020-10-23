package pathstore.authentication.datalayerimpls;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.authentication.Credential;
import pathstore.authentication.CredentialDataLayer;
import pathstore.common.Constants;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This is an impl of {@link CredentialDataLayer} and is used to manage the Local Client Auth Table
 */
public class LocalClientAuth implements CredentialDataLayer<String> {

  /** @return map from primary key of Credential to credential object */
  @Override
  public ConcurrentMap<String, Credential<String>> load(final Session session) {
    return StreamSupport.stream(
            session
                .execute(
                    QueryBuilder.select()
                        .all()
                        .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_CLIENT_AUTH))
                .spliterator(),
            true)
        .map(this::buildFromRow)
        .collect(
            Collectors.toConcurrentMap(credential -> credential.primaryKey, Function.identity()));
  }

  /**
   * @param row row from pathstore_applications.local_client_auth
   * @return Credential credential parsed row
   */
  @Override
  public Credential<String> buildFromRow(final Row row) {
    return new Credential<>(
        row.getString(Constants.LOCAL_CLIENT_AUTH_COLUMNS.KEYSPACE_NAME),
        row.getString(Constants.LOCAL_CLIENT_AUTH_COLUMNS.USERNAME),
        row.getString(Constants.LOCAL_CLIENT_AUTH_COLUMNS.PASSWORD));
  }

  /**
   * @param session session object to write to the database
   * @param credential credentials to write
   * @return credential object that was passed
   */
  @Override
  public Credential<String> write(final Session session, final Credential<String> credential) {
    if (session != null && credential != null) {
      CassandraAuthenticationUtil.createRole(
          session, credential.username, false, true, credential.password);
      CassandraAuthenticationUtil.grantAccessToKeyspace(
          session, credential.primaryKey, credential.username);
      session.execute(
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_CLIENT_AUTH)
              .value(Constants.LOCAL_CLIENT_AUTH_COLUMNS.KEYSPACE_NAME, credential.primaryKey)
              .value(Constants.LOCAL_CLIENT_AUTH_COLUMNS.USERNAME, credential.username)
              .value(Constants.LOCAL_CLIENT_AUTH_COLUMNS.PASSWORD, credential.password));
    }
    return credential;
  }

  /**
   * @param session session object to delete to the database
   * @param credential credentials to delete
   */
  @Override
  public void delete(final Session session, final Credential<String> credential) {
    if (session != null && credential != null) {
      session.execute(
          QueryBuilder.delete()
              .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_CLIENT_AUTH)
              .where(
                  QueryBuilder.eq(
                      Constants.LOCAL_CLIENT_AUTH_COLUMNS.KEYSPACE_NAME, credential.primaryKey)));
      CassandraAuthenticationUtil.revokeAccessToKeyspace(
          session, credential.primaryKey, credential.username);
      CassandraAuthenticationUtil.dropRole(session, credential.username);
    }
  }
}

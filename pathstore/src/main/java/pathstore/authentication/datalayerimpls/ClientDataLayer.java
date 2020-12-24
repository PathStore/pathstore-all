package pathstore.authentication.datalayerimpls;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import pathstore.authentication.credentials.Credential;
import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.authentication.credentials.ClientCredential;
import pathstore.authentication.CredentialDataLayer;
import pathstore.common.Constants;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This is an impl of {@link CredentialDataLayer} and is used to manage the Local Client Auth Table
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientDataLayer implements CredentialDataLayer<String, ClientCredential> {

  /** Instance of this data layer */
  @Getter(lazy = true)
  private static final ClientDataLayer instance = new ClientDataLayer();

  /**
   * @param session session to execute on
   * @return map from primary key of Credential to credential object
   */
  @Override
  public ConcurrentMap<String, ClientCredential> load(@NonNull final Session session) {
    return StreamSupport.stream(
            session
                .execute(
                    QueryBuilder.select()
                        .all()
                        .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_CLIENT_AUTH))
                .spliterator(),
            true)
        .map(this::buildFromRow)
        .collect(Collectors.toConcurrentMap(Credential::getSearchable, Function.identity()));
  }

  /**
   * @param row row from pathstore_applications.local_client_auth
   * @return Credential credential parsed row
   */
  @Override
  public ClientCredential buildFromRow(@NonNull final Row row) {
    return new ClientCredential(
        row.getString(Constants.LOCAL_CLIENT_AUTH_COLUMNS.KEYSPACE_NAME),
        row.getString(Constants.LOCAL_CLIENT_AUTH_COLUMNS.USERNAME),
        row.getString(Constants.LOCAL_CLIENT_AUTH_COLUMNS.PASSWORD),
        row.getBool(Constants.LOCAL_CLIENT_AUTH_COLUMNS.IS_SUPER_USER));
  }

  /**
   * @param session session object to write to the database
   * @param credential credentials to write
   * @return credential object that was passed
   */
  @Override
  public ClientCredential write(
      @NonNull final Session session, @NonNull final ClientCredential credential) {
    CassandraAuthenticationUtil.createRole(
        session,
        credential.getUsername(),
        credential.isSuperUser(),
        true,
        credential.getPassword());
    if (!credential.isSuperUser())
      CassandraAuthenticationUtil.grantAccessToKeyspace(
          session, credential.getSearchable(), credential.getUsername());
    session.execute(
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_CLIENT_AUTH)
            .value(Constants.LOCAL_CLIENT_AUTH_COLUMNS.KEYSPACE_NAME, credential.getSearchable())
            .value(Constants.LOCAL_CLIENT_AUTH_COLUMNS.USERNAME, credential.getUsername())
            .value(Constants.LOCAL_CLIENT_AUTH_COLUMNS.PASSWORD, credential.getPassword())
            .value(Constants.LOCAL_CLIENT_AUTH_COLUMNS.IS_SUPER_USER, credential.isSuperUser()));

    return credential;
  }

  /**
   * @param session session object to delete to the database
   * @param credential credentials to delete
   */
  @Override
  public void delete(@NonNull final Session session, @NonNull final ClientCredential credential) {
    session.execute(
        QueryBuilder.delete()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_CLIENT_AUTH)
            .where(
                QueryBuilder.eq(
                    Constants.LOCAL_CLIENT_AUTH_COLUMNS.KEYSPACE_NAME,
                    credential.getSearchable())));
    if (!credential.isSuperUser())
      CassandraAuthenticationUtil.revokeAccessToKeyspace(
          session, credential.getSearchable(), credential.getUsername());
    CassandraAuthenticationUtil.dropRole(session, credential.getUsername());
  }
}

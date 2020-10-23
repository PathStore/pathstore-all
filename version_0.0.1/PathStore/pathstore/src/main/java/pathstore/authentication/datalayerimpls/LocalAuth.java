package pathstore.authentication.datalayerimpls;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.authentication.Credential;
import pathstore.authentication.CredentialDataLayer;
import pathstore.common.Constants;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** This is an impl of {@link CredentialDataLayer} and is used to manage the Local Auth Table */
public class LocalAuth implements CredentialDataLayer<Integer> {

  /** @return map from primary key of Credential to credential object */
  @Override
  public ConcurrentMap<Integer, Credential<Integer>> load(final Session session) {
    return StreamSupport.stream(
            session
                .execute(
                    QueryBuilder.select()
                        .all()
                        .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_AUTH))
                .spliterator(),
            true)
        .map(this::buildFromRow)
        .collect(
            Collectors.toConcurrentMap(credential -> credential.primaryKey, Function.identity()));
  }

  /**
   * @param row row from pathstore_applications.local_auth
   * @return Credential credential parsed row
   */
  @SuppressWarnings("ALL")
  public Credential<Integer> buildFromRow(final Row row) {
    return new Credential<>(
        row.getInt(Constants.LOCAL_AUTH_COLUMNS.NODE_ID),
        row.getString(Constants.LOCAL_AUTH_COLUMNS.USERNAME),
        row.getString(Constants.LOCAL_AUTH_COLUMNS.PASSWORD));
  }

  /**
   * @param session session object to write to the database
   * @param credential credentials to write
   * @return credential object that was passed
   */
  public Credential<Integer> write(final Session session, final Credential<Integer> credential) {
    if (session != null && credential != null)
      session.execute(
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_AUTH)
              .value(Constants.LOCAL_AUTH_COLUMNS.NODE_ID, credential.primaryKey)
              .value(Constants.LOCAL_AUTH_COLUMNS.USERNAME, credential.username)
              .value(Constants.LOCAL_AUTH_COLUMNS.PASSWORD, credential.password));
    return credential;
  }

  /**
   * @param session session object to delete to the database
   * @param credential credentials to delete
   */
  public void delete(final Session session, final Credential<Integer> credential) {
    if (session != null && credential != null)
      session.execute(
          QueryBuilder.delete()
              .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_AUTH)
              .where(QueryBuilder.eq(Constants.LOCAL_AUTH_COLUMNS.NODE_ID, credential.primaryKey)));
  }
}

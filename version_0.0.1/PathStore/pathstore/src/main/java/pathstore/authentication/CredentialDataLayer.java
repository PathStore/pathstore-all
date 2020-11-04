package pathstore.authentication;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.util.concurrent.ConcurrentMap;

/**
 * This class is used to denote how to store a credential object in the database depending on where
 * it is generated
 *
 * @see CredentialCache#getNodeAuth() For the pathstore_applications.local_auth table
 * @see CredentialCache#getClientAuth() For the pathstore_applications.local_client_auth table
 * @param <SearchableT> Data type of primary key
 * @param <CredentialT> Credential Type
 */
public interface CredentialDataLayer<SearchableT, CredentialT extends Credential<SearchableT>> {

  /** @return map from primary key of Credential to credential object */
  ConcurrentMap<SearchableT, CredentialT> load(final Session session);

  /**
   * @param row row from a keyspace and table that stores a credential object
   * @see Credential
   * @return Credential credential parsed row
   */
  CredentialT buildFromRow(final Row row);

  /**
   * @param session session object to write to the database
   * @param credential credentials to write
   * @return credential object that was passed
   */
  CredentialT write(final Session session, final CredentialT credential);

  /**
   * @param session session object to delete to the database
   * @param credential credentials to delete
   */
  void delete(final Session session, final CredentialT credential);
}

package pathstore.authentication.credentials;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This is the base credential class. All other credential references should extend this class.
 *
 * @param <SearchableT> type of class you want to be able to index by in {@link
 *     pathstore.util.ClusterCache}
 */
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = "searchable")
public class Credential<SearchableT> {
  /**
   * How to search for clusters in the cache
   *
   * @see NoopCredential If you wish to just compare a username and password to a credential in the
   *     cache without extracting the information from the credential and performing a manual
   *     comparison
   */
  @Getter @NonNull private final SearchableT searchable;

  /** Username of the credential */
  @Getter @NonNull private final String username;

  /** Password of the credential */
  @Getter @NonNull private final String password;
}

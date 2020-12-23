package pathstore.authentication;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This represents a row in the application_credentials table
 *
 * @see ClientAuthenticationUtil#getApplicationCredentialRow(String, String)
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class ApplicationCredential {
  /** keyspace for the application */
  @Getter @NonNull private final String keyspaceName;

  /** Master application credential password */
  @Getter @NonNull private final String password;

  /** are the application clients going to be super users */
  @Getter private final boolean isSuperUser;
}

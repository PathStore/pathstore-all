package pathstore.authentication.grpc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import pathstore.authentication.Credential;
import pathstore.authentication.CredentialCache;

import java.util.*;

/**
 * This class is used for the registration, storage and comparison of credentials related to a grpc
 * server.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthManager {

  /** This class is used to build the auth manager with proper credentials per service. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Builder {
    /** map from service name to collection of valid server credentials */
    private final Map<String, Collection<Credential<Integer>>> serverCredentials = new HashMap<>();

    /** map from service name to collection of valid client credentials */
    private final Map<String, Collection<Credential<String>>> clientCredentials = new HashMap<>();

    /**
     * Set of unauthenticated services. As in regardless of credentials provided you will be able to
     * call any endpoint in that service
     */
    private final Set<String> unauthenticated = new HashSet<>();

    /** Additional credentials */
    private final Map<String, Set<Credential<?>>> additionalCredentials = new HashMap<>();

    /**
     * This function is used to authenticate a service with a collection of server credentials
     *
     * @param serviceName service name to set permissions on
     * @param serverCredentials server credentials to provide
     * @return this
     * @see CredentialCache#getNodeAuth()
     */
    public Builder serverAuthenticatedEndpoint(
        final String serviceName, final Collection<Credential<Integer>> serverCredentials) {
      if (serverCredentials != null) this.serverCredentials.put(serviceName, serverCredentials);
      return this;
    }

    /**
     * This function is used to authenticate a service with a collection of client credentials
     *
     * @param serviceName service name to set permissions on
     * @param clientCredentials client credentials to provide
     * @return this
     * @see CredentialCache#getClientAuth()
     */
    public Builder clientAuthenticatedEndpoint(
        final String serviceName, final Collection<Credential<String>> clientCredentials) {
      if (clientCredentials != null) this.clientCredentials.put(serviceName, clientCredentials);
      return this;
    }

    /**
     * @param serviceName service name to set permissions on
     * @param serverCredentials server credentials to provide
     * @param clientCredentials client credentials to provide
     * @return this
     * @see #serverAuthenticatedEndpoint(String, Collection)
     * @see #clientAuthenticatedEndpoint(String, Collection)
     */
    public Builder serverAndClientAuthenticatedEndpoint(
        final String serviceName,
        final Collection<Credential<Integer>> serverCredentials,
        final Collection<Credential<String>> clientCredentials) {
      this.serverAuthenticatedEndpoint(serviceName, serverCredentials);
      this.clientAuthenticatedEndpoint(serviceName, clientCredentials);
      return this;
    }

    /**
     * @param serviceName endpoint to not have authentication
     * @return this
     */
    public Builder unauthenticatedEndpoint(final String serviceName) {
      this.unauthenticated.add(serviceName);
      return this;
    }

    /**
     * This function is used to add a specific username and password combo to the authentication
     *
     * @param serviceName service name to apply to
     * @param credential credential to add
     * @return this
     */
    public Builder addAdditionalCredentials(
        final String serviceName, final Credential<?> credential) {
      this.additionalCredentials.putIfAbsent(serviceName, new HashSet<>());
      this.additionalCredentials.get(serviceName).add(credential);
      return this;
    }

    /** @return built instance of the auth manager */
    public AuthManager build() {
      return new AuthManager(
          this.serverCredentials,
          this.clientCredentials,
          this.unauthenticated,
          this.additionalCredentials);
    }
  }

  /** @return new instance of the builder class */
  public static Builder newBuilder() {
    return new Builder();
  }

  /** All credentials between servers */
  private final Map<String, Collection<Credential<Integer>>> serverCredentials;

  /** All credentials between client */
  private final Map<String, Collection<Credential<String>>> clientCredentials;

  /** All unauthenticated endpoints */
  private final Set<String> unauthenticated;

  /** Additional specific credentials add to a service */
  private final Map<String, Set<Credential<?>>> additionalCredentials;

  /**
   * @param endpoint endpoint called
   * @param primaryKey primary key given
   * @param username username given
   * @param password and password given
   * @return true if authenticated false if not
   */
  public boolean isAuthenticated(
      String endpoint, final String primaryKey, final String username, final String password) {
    if (endpoint == null || primaryKey == null || username == null || password == null)
      return false;

    // get the service name of the endpoint as, authentication is at the service layer not the
    // endpoint layer
    endpoint = endpoint.substring(0, endpoint.indexOf('/'));

    // check to ensure the service is registered with the auth manager
    if (!this.serverCredentials.containsKey(endpoint)
        && !this.clientCredentials.containsKey(endpoint)
        && !this.unauthenticated.contains(endpoint)
        && !this.additionalCredentials.containsKey(endpoint)) return false;

    // check if the service is unauthenticated
    if (this.unauthenticated.contains(endpoint)) return true;

    // check to see if the credential is present in the additional credentials before doing the
    // linear search
    if (this.additionalCredentials.containsKey(endpoint)
        && this.additionalCredentials
            .get(endpoint)
            .contains(new Credential<>(null, username, password))) return true;

    // authenticated services
    if (NumberUtils.isCreatable(primaryKey)) { // server identity, as the primary key is a number
      Credential<Integer> serverCredential =
          new Credential<>(Integer.parseInt(primaryKey), username, password);

      for (Credential<Integer> server : this.serverCredentials.get(endpoint))
        if (server.equals(serverCredential)) return true;

    } else { // client identity, as the primary key is not a number
      Credential<String> clientCredential = new Credential<>(primaryKey, username, password);

      for (Credential<String> client : this.clientCredentials.get(endpoint))
        if (client.equals(clientCredential)) return true;
    }

    return false;
  }
}

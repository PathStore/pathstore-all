package pathstore.common;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pathstore.client.PathStoreSession;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class is used to cache application lease times on the server and the client.
 *
 * <p>This is so that when performing operations on the querycache we have a centralized place to
 * pull CLT / SLT information
 */
public class ApplicationLeaseCache {
  /** Instance of cache */
  @Getter(lazy = true)
  private static final ApplicationLeaseCache instance = new ApplicationLeaseCache();

  /** Lease Cache */
  private final Map<String, ApplicationLease> leaseCache = new HashMap<>();

  /**
   * This function is used to allow the witting of a application lease manually. This is because we
   * do not authorize clients to read from the table that stores leasing information.
   *
   * @param applicationName application name to set
   * @param applicationLease lease to set to
   * @apiNote This should only be called from {@link
   *     pathstore.client.PathStoreClientAuthenticatedCluster} and only works if the key is not set.
   */
  public void setLease(final String applicationName, final ApplicationLease applicationLease) {
    if (!this.leaseCache.containsKey(applicationName))
      this.leaseCache.put(applicationName, applicationLease);
  }

  /**
   * Allows user to query lease for some application name. If the name is not present in the cache
   * we load it into memory for future use.
   *
   * @param applicationName name to retrieve leasing information for
   * @return optional application lease. If present it is a valid application name, else it is an
   *     in-valid application name that is not registered within the pathstore network.
   * @apiNote We can perform this type of caching as this table is immutable. It is only written to
   *     by the admin-panel and once a record is written for a given application it never changes.
   *     This class will need to be changed if there is a possibility for lease time change in the
   *     future
   */
  public Optional<ApplicationLease> getLease(final String applicationName) {
    if (leaseCache.containsKey(applicationName))
      return Optional.of(leaseCache.get(applicationName));
    else {
      if (PathStoreProperties.getInstance().role != Role.CLIENT) {
        PathStoreSession session = PathStorePrivilegedCluster.getDaemonInstance().psConnect();

        Select selectApplicationLeaseInformation =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.APPLICATION_LEASE_TIME);

        selectApplicationLeaseInformation.where(
            QueryBuilder.eq(
                Constants.APPLICATION_LEASE_TIME_COLUMNS.KEYSPACE_NAME, applicationName));

        Optional<ApplicationLease> applicationLeaseOptional =
            session.execute(selectApplicationLeaseInformation).stream()
                .findFirst()
                .map(
                    row ->
                        new ApplicationLease(
                            row.getInt(Constants.APPLICATION_LEASE_TIME_COLUMNS.CLIENT_LEASE_TIME),
                            row.getInt(
                                Constants.APPLICATION_LEASE_TIME_COLUMNS.SERVER_ADDITIONAL_TIME)));

        applicationLeaseOptional.ifPresent(
            applicationLease -> this.leaseCache.put(applicationName, applicationLease));

        return applicationLeaseOptional;
      } else
        throw new RuntimeException(
            "Cannot get lease information for applications outside of the one registered to your client account");
    }
  }

  /**
   * This class is used to represent an application lease from {@link
   * Constants#APPLICATION_LEASE_TIME}
   */
  @RequiredArgsConstructor
  public static final class ApplicationLease {
    /** Additional server time */
    private final int serverAdditionalTime;

    /** CLT */
    @Getter private final int clientLeaseTime;

    /** SLT */
    @Getter(lazy = true)
    private final int serverLeaseTime = serverAdditionalTime + clientLeaseTime;
  }
}

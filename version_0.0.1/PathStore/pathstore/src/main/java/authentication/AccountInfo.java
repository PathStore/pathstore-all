package authentication;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.Set;

/**
 * This class is used to represent accounts that are already available within the local cassandra
 * node. This should not be confused with {@link CredentialInfo} which describes relative accounts
 * that are needed for database access to relative nodes (Parent Node, Child nodes)
 *
 * <p>TODO: Finish this class.
 */
public final class AccountInfo {

  private final Session privSession = PathStorePrivilegedCluster.getInstance().connect();

  public static final class Role {
    public final String role;
    public final boolean can_login;
    public final boolean is_superuser;
    public final Set<String> member_of;

    private Role(
        final String role,
        final boolean can_login,
        final boolean is_superuser,
        final Set<String> member_of) {
      this.role = role;
      this.can_login = can_login;
      this.is_superuser = is_superuser;
      this.member_of = member_of;
    }

    public static Role buildFromRow(final Row row) {
      return new Role(
          row.getString("role"),
          row.getBool("can_login"),
          row.getBool("is_superuser"),
          row.getSet("member_of", String.class));
    }
  }

  public static final class RoleMember {
    public final String role;
    public final String member;

    private RoleMember(final String role, final String member) {
      this.role = role;
      this.member = member;
    }

    public static RoleMember buildFromRow(final Row row) {
      return new RoleMember(row.getString("role"), row.getString("member"));
    }
  }
}

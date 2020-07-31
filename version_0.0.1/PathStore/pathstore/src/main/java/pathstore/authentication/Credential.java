package pathstore.authentication;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import java.util.Objects;

// row in local_keyspace.auth or generated from properties file.
public final class Credential {
  public final int node_id;
  public final String username;
  public final String password;

  public Credential(final int node_id, final String username, final String password) {
    this.node_id = node_id;
    this.username = username;
    this.password = password;
  }

  public static Credential buildFromRow(final Row row) {
    return new Credential(
        row.getInt("node_id"), row.getString("username"), row.getString("password"));
  }

  public static Credential writeCredentialToRow(
      final Session session, final Credential credential) {
    if (session != null && credential != null)
      session.execute(
          QueryBuilder.insertInto("local_keyspace", "auth")
              .value("node_id", credential.node_id)
              .value("username", credential.username)
              .value("password", credential.password));

    return credential;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Credential that = (Credential) o;
    return this.node_id == that.node_id
        && this.username.equals(that.username)
        && this.password.equals(that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.node_id, this.username, this.password);
  }
}

package authentication;

import com.datastax.driver.core.Row;

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
}

package authentication;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class is used to load authentication credentials for the parent process, for the
 * administrator account and for all children nodes created by this process. The connection
 * information for this given node is written in the properties file to bootstrap this process
 */
public final class CredentialInfo {

  private static CredentialInfo instance = null;

  public static synchronized CredentialInfo getInstance() {
    if (instance == null) instance = new CredentialInfo();
    return instance;
  }

  private final Map<Integer, Credential> credentials;

  private final Session privSession = PathStorePrivilegedCluster.getInstance().connect();

  private CredentialInfo() {
    this.credentials = this.load();
  }

  private Map<Integer, Credential> load() {
    return StreamSupport.stream(
            this.privSession
                .execute(QueryBuilder.select().all().from("local_keyspace", "auth"))
                .spliterator(),
            true)
        .map(Credential::buildFromRow)
        .collect(Collectors.toMap(credential -> credential.node_id, Function.identity()));
  }

  // will only work on server side because of authentication restriction of user accounts
  public void add(final int nodeId, final String username, final String password) {
    PathStorePrivilegedCluster.getInstance()
        .connect()
        .execute(
            QueryBuilder.insertInto("local_keyspace", "auth")
                .value("node_id", nodeId)
                .value("username", username)
                .value("password", password));

    this.credentials.put(nodeId, new Credential(nodeId, username, password));
  }

  // may return null
  public Credential getCredential(final int node_id) {
    return this.credentials.get(node_id);
  }
}

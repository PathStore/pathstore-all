package pathstore.authentication;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class CredentialInfo {

  private static CredentialInfo instance = null;

  public static synchronized CredentialInfo getInstance() {
    if (instance == null) instance = new CredentialInfo();
    return instance;
  }

  private final ConcurrentMap<Integer, Credential> credentials;

  private final Session privSession = PathStorePrivilegedCluster.getSuperUserInstance().connect();

  private CredentialInfo() {
    this.credentials = this.load();
  }

  private ConcurrentMap<Integer, Credential> load() {
    return StreamSupport.stream(
            this.privSession
                .execute(QueryBuilder.select().all().from("local_keyspace", "auth"))
                .spliterator(),
            true)
        .map(Credential::buildFromRow)
        .collect(Collectors.toConcurrentMap(credential -> credential.node_id, Function.identity()));
  }

  public void add(final int nodeId, final String username, final String password) {
    this.credentials.put(
        nodeId,
        Credential.writeCredentialToRow(
            this.privSession, new Credential(nodeId, username, password)));
  }

  public void remove(final int nodeId) {
    Credential credential = this.getCredential(nodeId);

    if (credential == null) return;

    this.credentials.remove(nodeId);

    this.privSession.execute(
        QueryBuilder.delete()
            .from("local_keyspace", "auth")
            .where(QueryBuilder.eq("node_id", nodeId)));
  }

  // may return null
  public Credential getCredential(final int node_id) {
    return this.credentials.get(node_id);
  }
}

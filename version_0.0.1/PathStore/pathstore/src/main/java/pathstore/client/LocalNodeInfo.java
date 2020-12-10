package pathstore.client;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pathstore.common.PathStoreProperties;
import pathstore.util.BlobObject;

/**
 * This class is used for denoting the information that some client will need from the server during
 * the initial handshake
 *
 * <p>Needs to include:
 *
 * <ul>
 *   <li>Node Id
 *   <li>Cassandra IP
 *   <li>Cassandra Port
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalNodeInfo implements BlobObject {

  /** instance of class */
  @Getter(lazy = true)
  private static final LocalNodeInfo instance = new LocalNodeInfo();

  /** @see PathStoreProperties#NodeID */
  @Getter private final int nodeId = PathStoreProperties.getInstance().NodeID;

  /**
   * @see PathStoreProperties#CassandraIP
   * @implNote So currently during orchestration we supply a new child node with the cassandra ip of
   *     127.0.0.1. Eventually this will exist on a separate server. To account for this if the
   *     provided address is 127.0.0.1 we can provide the external address of the machine for the
   *     client otherwise we will provide the cassandra ip directly
   */
  @Getter
  private final String cassandraIP =
      PathStoreProperties.getInstance().CassandraIP.equals("127.0.0.1")
          ? PathStoreProperties.getInstance().ExternalAddress
          : PathStoreProperties.getInstance().CassandraIP;

  /** @see PathStoreProperties#CassandraPort */
  @Getter private final int cassandraPort = PathStoreProperties.getInstance().CassandraPort;
}

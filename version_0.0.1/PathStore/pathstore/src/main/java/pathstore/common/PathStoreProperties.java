/**
 * ********
 *
 * <p>Copyright 2019 Eyal de Lara, Seyed Hossein Mortazavi, Mohammad Salehe
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*********
 */
package pathstore.common;

import pathstore.authentication.Credential;
import pathstore.client.PathStoreServerClient;

import java.io.FileInputStream;
import java.util.Properties;

import static pathstore.common.Constants.PROPERTIESFILE;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.*;

/**
 * This class is used to load runtime properties from a properties file located in {@link
 * Constants#PROPERTIESFILE}. This is used instead of run-time arguments to simplify the reading and
 * updating of the data. Also even though there are lots of options only certain options will get
 * loaded in based on what ROLE you have set. Thus the ROLE must be set in order to determine which
 * pieces of data get loaded in. The data that gets loading in is as follows:
 *
 * <p>SERVER {@link Role#SERVER}:
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#RMI_REGISTRY_PARENT_IP}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#RMI_REGISTRY_PARENT_PORT}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#CASSANDRA_PARENT_IP}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#CASSANDRA_PARENT_PORT}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#PULL_SLEEP}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#PUSH_SLEEP}
 *
 * <p>Plus all the values from {@link Role#ROOTSERVER} and {@link Role#CLIENT}
 *
 * <p>ROOTSERVER {@link Role#ROOTSERVER}:
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#EXTERNAL_ADDRESS}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#NODE_ID}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#PARENT_ID}
 *
 * <p>Plus all the values from {@link Role#CLIENT}
 *
 * <p>CLIENT {@link Role#CLIENT}:
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#RMI_REGISTRY_IP}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#RMI_REGISTRY_PORT}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#CASSANDRA_IP}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#CASSANDRA_PORT}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#SESSION_FILE}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#APPLICATION_NAME}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#APPLICATION_MASTER_PASSWORD}
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#USERNAME} Note: This is only used for privileged
 * clients, ones run by the network admin. Otherwise it is used for all servers
 *
 * <p>{@link Constants.PROPERTIES_CONSTANTS#PASSWORD} Note: This is only used for privileged
 * clients, ones run by the network admin. Otherwise it is used for all servers
 */
public class PathStoreProperties {

  /**
   * represents the instance of this class to be used more then once across the library without
   * passing the instance around
   */
  private static PathStoreProperties instance = null;

  /** @return either create new instance and return it or return existing instance */
  public static synchronized PathStoreProperties getInstance() {
    if (PathStoreProperties.instance == null) {
      PathStoreProperties.instance = new PathStoreProperties();

      // load node id if role is client
      if (instance.role == Role.CLIENT)
        instance.NodeID = PathStoreServerClient.getInstance().getLocalNodeId();
    }
    return PathStoreProperties.instance;
  }

  /** Used to verify the present of the super user credentials within the properties file */
  public void verifyCassandraSuperUserCredentials() {
    if (this.credential == null)
      throw new RuntimeException("Super user credentials are not present in the properties file");
  }

  /** Used to verify the presence of the cassandra ip and port information */
  public void verifyCassandraConnectionDetails() {
    if (this.CassandraIP == null)
      throw new RuntimeException("Cassandra IP is not present within the properties file");
    if (this.CassandraPort == -1)
      throw new RuntimeException("Cassandra port is not present within the properties file");
  }

  /** Used to verify the presence of the parent cassandra ip and port information */
  public void verifyParentCassandraConnectionDetails() {
    if (this.CassandraParentIP == null)
      throw new RuntimeException("Cassandra Parent IP is not present within the properties file");
    if (this.CassandraParentPort == -1)
      throw new RuntimeException("Cassandra Parent Port is not present within the properties file");
  }

  /** Used to verify client authentication details */
  public void verifyClientAuthenticationDetails() {
    if (this.applicationName == null)
      throw new RuntimeException("Application Name is not set in the properties file");

    if (this.applicationMasterPassword == null)
      throw new RuntimeException("Application Master Password is not set in the properties file");
  }

  /** Denotes the role of the server */
  public Role role;

  /** Denotes the publicly accessible ip of the server */
  public String ExternalAddress;

  /** Denotes the current node's id */
  public int NodeID;

  /** Denotes the current node's parent id (-1 if root) */
  public int ParentID;

  /** Denotes the current node's local rmi server normally 127.0.0.1 */
  public String RMIRegistryIP = null;

  /** Denotes the current node's local rmi server's port normally 1099 */
  public int RMIRegistryPort = -1;

  /** Denotes the current node's parent rmi server's address */
  public String RMIRegistryParentIP = null;

  /** Denotes the current node's parent rmi server's port */
  public int RMIRegistryParentPort = -1;

  /** Denote the node's local cassandra instance address */
  public String CassandraIP = null;

  /** Denotes the node's local cassandra instance port */
  public int CassandraPort = -1;

  /** Denotes the node's parent cassandra instance address */
  public String CassandraParentIP = null;

  /** Denotes the node's parent cassandra instance port */
  public int CassandraParentPort = -1;

  /** Denotes credential to local cassandra instance */
  public Credential credential = null;

  /**
   * Denotes batch size
   *
   * @see QueryCache
   */
  public int MaxBatchSize = 4096 * 10;

  /** Denotes how often the pull server pull's data */
  public int PullSleep = 1000;

  /** Denotes how often the push server pull's data */
  public int PushSleep = 1000;

  // client only properties

  /** where to store session tokens on client side */
  public String sessionFile = null;

  /** What is the name of the application a client is trying to connect for */
  public String applicationName = null;

  /** what is that applications master password */
  public String applicationMasterPassword = null;

  /**
   * Parses the data in accordance to what is needed per role. See class Java doc for description on
   * what is parsed
   */
  public PathStoreProperties() {
    try {
      Properties props = new Properties();
      FileInputStream in = new FileInputStream(PROPERTIESFILE);
      props.load(in);

      this.role = Role.valueOf(this.getProperty(props, ROLE));

      switch (this.role) {
        case SERVER:
          this.RMIRegistryParentIP = this.getProperty(props, RMI_REGISTRY_PARENT_IP);
          this.RMIRegistryParentPort =
              Integer.parseInt(this.getProperty(props, RMI_REGISTRY_PARENT_PORT));
          this.CassandraParentIP = this.getProperty(props, CASSANDRA_PARENT_IP);
          this.CassandraParentPort =
              Integer.parseInt(this.getProperty(props, CASSANDRA_PARENT_PORT));
          this.PullSleep = Integer.parseInt(this.getProperty(props, PULL_SLEEP));
          this.PushSleep = Integer.parseInt(this.getProperty(props, PUSH_SLEEP));
        case ROOTSERVER:
          this.ExternalAddress = this.getProperty(props, EXTERNAL_ADDRESS);
          this.NodeID = Integer.parseInt(this.getProperty(props, NODE_ID));
          this.ParentID = Integer.parseInt(this.getProperty(props, PARENT_ID));
        case CLIENT:
          this.RMIRegistryIP = this.getProperty(props, RMI_REGISTRY_IP);
          this.RMIRegistryPort = Integer.parseInt(this.getProperty(props, RMI_REGISTRY_PORT));
          this.CassandraIP = this.getProperty(props, CASSANDRA_IP);
          this.CassandraPort = Integer.parseInt(this.getProperty(props, CASSANDRA_PORT));
          this.credential =
              new Credential(
                  this.NodeID,
                  this.getProperty(props, USERNAME),
                  this.getProperty(props, PASSWORD));
          break;
        default:
          throw new Exception();
      }

      if (this.role == Role.CLIENT) {
        this.sessionFile = this.getProperty(props, SESSION_FILE);
        this.applicationName = this.getProperty(props, APPLICATION_NAME);
        this.applicationMasterPassword = this.getProperty(props, APPLICATION_MASTER_PASSWORD);
      }

      in.close();
    } catch (Exception ex) {
      System.err.println("Error parsing properties file with the stack trace:");
      ex.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Gets property from props file and trims the result
   *
   * @param properties {@link Constants#PROPERTIESFILE}
   * @param key key to get
   * @return trimmed response
   */
  private String getProperty(final Properties properties, final String key) {
    String response = properties.getProperty(key);
    if (response != null) return response.trim();
    else return "";
  }
  /** @return all values loaded in from properties file */
  @Override
  public String toString() {
    return "PathStoreProperties{"
        + "role="
        + role
        + ", ExternalAddress='"
        + ExternalAddress
        + '\''
        + ", NodeID="
        + NodeID
        + ", ParentID="
        + ParentID
        + ", RMIRegistryIP='"
        + RMIRegistryIP
        + '\''
        + ", RMIRegistryPort="
        + RMIRegistryPort
        + ", RMIRegistryParentIP='"
        + RMIRegistryParentIP
        + '\''
        + ", RMIRegistryParentPort="
        + RMIRegistryParentPort
        + ", CassandraIP='"
        + CassandraIP
        + '\''
        + ", CassandraPort="
        + CassandraPort
        + ", CassandraParentIP='"
        + CassandraParentIP
        + '\''
        + ", CassandraParentPort="
        + CassandraParentPort
        + ", credential="
        + credential
        + ", MaxBatchSize="
        + MaxBatchSize
        + ", PullSleep="
        + PullSleep
        + ", PushSleep="
        + PushSleep
        + ", sessionFile='"
        + sessionFile
        + '\''
        + ", applicationName='"
        + applicationName
        + '\''
        + ", applicationMasterPassword='"
        + applicationMasterPassword
        + '\''
        + '}';
  }
}

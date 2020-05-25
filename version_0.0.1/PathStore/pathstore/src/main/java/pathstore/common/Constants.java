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

/** TODO: Convert all literals to this class */
public final class Constants {

  public static final String PROPERTIESFILE = "/etc/pathstore/pathstore.properties";

  /** Where the properties file will be stored locally. */
  public static final String DESTINATION_TO_STORE =
      "../docker-files/pathstore/pathstore.properties";

  public static final class PROPERTIES_CONSTANTS {
    public static final String ROLE = "Role";
    public static final String EXTERNAL_ADDRESS = "ExternalAddress";
    public static final String NODE_ID = "NodeID";
    public static final String PARENT_ID = "ParentID";
    public static final String RMI_REGISTRY_IP = "RMIRegistryIP";
    public static final String RMI_REGISTRY_PORT = "RMIRegistryPort";
    public static final String RMI_REGISTRY_PARENT_IP = "RMIRegistryParentIP";
    public static final String RMI_REGISTRY_PARENT_PORT = "RMIRegistryParentPort";
    public static final String CASSANDRA_IP = "CassandraIP";
    public static final String CASSANDRA_PORT = "CassandraPort";
    public static final String CASSANDRA_PARENT_IP = "CassandraParentIP";
    public static final String CASSANDRA_PARENT_PORT = "CassandraParentPort";
    public static final String MAX_BATCH_SIZE = "MaxBatchSize";
    public static final String PULL_SLEEP = "PullSleep";
    public static final String PUSH_SLEEP = "PushSleep";
  }

  /** TODO: Change all references to this class */
  public static final class PATHSTORE_COLUMNS {
    public static final String PATHSTORE_VERSION = "pathstore_version";
    public static final String PATHSTORE_PARENT_TIMESTAMP = "pathstore_parent_timestamp";
    public static final String PATHSTORE_DELETED = "pathstore_deleted";
    public static final String PATHSTORE_DIRTY = "pathstore_dirty";
    public static final String PATHSTORE_NODE = "pathstore_node";
    public static final String PATHSTORE_INSERT_SID = "pathstore_insert_sid";
  }

  public static final String PATHSTORE_APPLICATIONS = "pathstore_applications";

  public static final String NODE_SCHEMAS = "node_schemas";

  public static final class NODE_SCHEMAS_COLUMNS {
    public static final String NODE_ID = "node_id";
    public static final String KEYSPACE_NAME = "keyspace_name";
    public static final String PROCESS_STATUS = "process_status";
    public static final String WAIT_FOR = "wait_for";
  }

  public static final String SERVERS = "servers";

  public static final class SERVERS_COLUMNS {
    public static final String SERVER_UUID = "server_uuid";
    public static final String IP = "ip";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SSH_PORT = "ssh_port";
    public static final String RMI_PORT = "rmi_port";
    public static final String NAME = "name";
  }

  public static final String DEPLOYMENT = "deployment";

  public static final class DEPLOYMENT_COLUMNS {
    public static final String NEW_NODE_ID = "new_node_id";
    public static final String PARENT_NODE_ID = "parent_node_id";
    public static final String PROCESS_STATUS = "process_status";
    public static final String WAIT_FOR = "wait_for";
    public static final String SERVER_UUID = "server_uuid";
  }

  public static final String AVAILABLE_LOG_DATES = "available_log_dates";

  public static final class AVAILABLE_LOG_DATES_COLUMNS {
    public static final String NODE_ID = "node_id";
    public static final String DATE = "date";
  }

  public static final String LOGS = "logs";

  public static final class LOGS_COLUMNS {
    public static final String NODE_ID = "node_id";
    public static final String DATE = "date";
    public static final String COUNT = "count";
    public static final String LOG_LEVEL = "log_level";
    public static final String LOG = "log";
  }

  public static final String APPS = "apps";

  public static final class APPS_COLUMNS {
    public static final String AUGMENTED_SCHEMA = "augmented_schema";
    public static final String KEYSPACE_NAME = "keyspace_name";
  }

  public static final String LOCAL_KEYSPACE = "local_keyspace";

  public static final String STARTUP = "startup";

  public static final class STARTUP_COLUMNS {
    public static final String TASK_DONE = "task_done";
  }
}

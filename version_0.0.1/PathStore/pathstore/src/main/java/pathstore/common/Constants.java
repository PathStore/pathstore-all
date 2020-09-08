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

  public static final String DEFAULT_CASSANDRA_USERNAME = "cassandra";
  public static final String DEFAULT_CASSANDRA_PASSWORD = "cassandra";

  public static final String PATHSTORE_SUPERUSER_USERNAME = "pathstoreadmin";
  public static final String PATHSTORE_DAEMON_USERNAME = "pathstore";

  public static final String PATHSTORE_PREFIX = "pathstore_";
  public static final String VIEW_PREFIX = "view_";
  public static final String LOCAL_PREFIX = "local_";

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
    public static final String USERNAME = "Username";
    public static final String PASSWORD = "Password";
    public static final String SESSION_FILE = "sessionFile";
    public static final String APPLICATION_NAME = "applicationName";
    public static final String APPLICATION_MASTER_PASSWORD = "applicationMasterPassword";
  }

  /** TODO: Change all references to this class */
  public static final class PATHSTORE_META_COLUMNS {
    public static final String PATHSTORE_VERSION = "pathstore_version";
    public static final String PATHSTORE_PARENT_TIMESTAMP = "pathstore_parent_timestamp";
    public static final String PATHSTORE_DELETED = "pathstore_deleted";
    public static final String PATHSTORE_DIRTY = "pathstore_dirty";
    public static final String PATHSTORE_NODE = "pathstore_node";
    public static final String PATHSTORE_VIEW_ID = "pathstore_view_id";
  }

  public static final String SYSTEM_SCHEMA = "system_schema";

  public static final String KEYSPACES = "keyspaces";

  public static final class KEYSPACES_COLUMNS {
    public static final String KEYSPACE_NAME = "keyspace_name";
  }

  public static final String TABLES = "tables";

  public static final class TABLES_COLUMNS {
    public static final String KEYSPACE_NAME = "keyspace_name";
    public static final String TABLE_NAME = "table_name";
    public static final String BLOOM_FILTER_FP_CHANCE = "bloom_filter_fp_chance";
    public static final String CACHING = "caching";
    public static final String CDC = "cdc";
    public static final String COMMENT = "comment";
    public static final String COMPACTION = "compaction";
    public static final String COMPRESSION = "compression";
    public static final String CRC_CHECK_CHANCE = "crc_check_chance";
    public static final String DCLOCAL_READ_REPAIR_CHANCE = "dclocal_read_repair_chance";
    public static final String DEFAULT_TIME_TO_LIVE = "default_time_to_live";
    public static final String EXTENSIONS = "extensions";
    public static final String FLAGS = "flags";
    public static final String GC_GRACE_SECONDS = "gc_grace_seconds";
    public static final String ID = "id";
    public static final String MAX_INDEX_INTERVAL = "max_index_interval";
    public static final String MEMTABLE_FLUSH_PERIOD_IN_MS = "memtable_flush_period_in_ms";
    public static final String MIN_INDEX_INTERVAL = "min_index_interval";
    public static final String READ_REPAIR_CHANCE = "read_repair_chance";
    public static final String SPECULATIVE_RETRY = "speculative_retry";
  }

  public static final String COLUMNS = "columns";

  public static final class COLUMNS_COLUMNS {
    public static final String KEYSPACE_NAME = "keyspace_name";
    public static final String TABLE_NAME = "table_name";
    public static final String COLUMN_NAME = "column_name";
    public static final String CLUSTERING_ORDER = "clustering_order";
    public static final String KIND = "kind";
    public static final String POSITION = "position";
    public static final String TYPE = "type";
  }

  public static final String INDEXES = "indexes";

  public static final class INDEXES_COLUMNS {
    public static final String KEYSPACE_NAME = "keyspace_name";
    public static final String TABLE_NAME = "table_name";
    public static final String INDEX_NAME = "index_name";
    public static final String KIND = "kind";
    public static final String OPTIONS = "options";
  }

  public static final String TYPES = "types";

  public static final class TYPES_COLUMNS {
    public static final String KEYSPACE_NAME = "keyspace_name";
    public static final String TYPE_NAME = "type_name";
    public static final String FIELD_NAMES = "field_names";
    public static final String FIELD_TYPES = "field_types";
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

  public static final String APPLICATION_CREDENTIALS = "application_credentials";

  public static final class APPLICATION_CREDENTIALS_COLUMNS {
    public static final String KEYSPACE_NAME = "keyspace_name";
    public static final String PASSWORD = "password";
  }

  public static final String APPS = "apps";

  public static final class APPS_COLUMNS {
    public static final String AUGMENTED_SCHEMA = "augmented_schema";
    public static final String KEYSPACE_NAME = "keyspace_name";
  }

  public static final String LOCAL_STARTUP = "local_startup";

  public static final class LOCAL_STARTUP_COLUMNS {
    public static final String TASK_DONE = "task_done";
  }

  public static final String LOCAL_AUTH = "local_auth";

  public static final class LOCAL_AUTH_COLUMNS {
    public static final String NODE_ID = "node_id";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
  }

  public static final String LOCAL_CLIENT_AUTH = "local_client_auth";

  public static final class LOCAL_CLIENT_AUTH_COLUMNS {
    public static final String KEYSPACE_NAME = "keyspace_name";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
  }

  public static final class REGISTER_APPLICATION {
    public static final String STATUS = "status";

    public enum STATUS_STATES {
      VALID,
      INVALID
    }

    public static final String REASON = "reason";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
  }

  public static final class SESSION_TOKEN {
    public static final String SESSION_UUID = "sessionUUID";
    public static final String SOURCE_NODE = "sourceNode";
    public static final String SESSION_NAME = "sessionName";
    public static final String SESSION_TYPE = "sessionType";
    public static final String DATA = "data";
  }
}

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

  public static final String PATHSTORE_APPLICATIONS = "pathstore_applications";

  public static final String TOPOLOGY = "topology";

  public static final class TOPOLOGY_COLUMNS {
    public static final String NODE_ID = "nodeid";
    public static final String PARENT_NODE_ID = "parent_nodeid";
  }

  public static final String NODE_SCHEMAS = "node_schemas";

  public static final class NODE_SCHEMAS_COLUMNS {
    public static final String NODE_ID = "nodeid";
    public static final String KEYSPACE_NAME = "keyspace_name";
    public static final String PROCESS_STATUS = "process_status";
    public static final String WAIT_FOR = "wait_for";
    public static final String PROCESS_UUID = "process_uuid";
  }

  public static final String SERVERS = "servers";

  public static final class SERVERS_COLUMNS {
    public static final String SERVER_UUID = "server_uuid";
    public static final String IP = "ip";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String NAME = "name";
  }

  public static final String APPS = "apps";

  public static final class APPS_COLUMNS {
    public static final String APP_ID = "app_id";
    public static final String AUGMENTED_SCHEMA = "augmented_schema";
    public static final String KEYSPACE_NAME = "keyspace_name";
  }

  public static final String LOCAL_KEYSPACE = "local_keyspace";

  public static final String STARTUP = "startup";

  public static final class STARTUP_COLUMNS {
    public static final String TASK_DONE = "task_done";
  }
}

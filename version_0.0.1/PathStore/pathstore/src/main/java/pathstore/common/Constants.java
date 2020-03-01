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
public class Constants {

  public static String PROPERTIESFILE = "/etc/pathstore/pathstore.properties";

  public static String PATHSTORE_APPLICATIONS = "pathstore_applications";

  public static String TOPOLOGY = "topology";

  public static class TOPOLOGY_COLUMNS {
    public static String NODE_ID = "nodeid";
    public static String PARENT_NODE_ID = "parent_nodeid";
  }

  public static String NODE_SCHEMAS = "node_schemas";

  public static class NODE_SCHEMAS_COLUMNS {
    public static String NODE_ID = "nodeid";
    public static String KEYSPACE_NAME = "keyspace_name";
    public static String PROCESS_STATUS = "process_status";
    public static String WAIT_FOR = "wait_for";
  }

  public static String APPS = "apps";

  public static class APPS_COLUMNS {
    public static String APP_ID = "app_id";
    public static String AUGMENTED_SCHEMA = "augmented_schema";
    public static String KEYSPACE_NAME = "keyspace_name";
  }
}

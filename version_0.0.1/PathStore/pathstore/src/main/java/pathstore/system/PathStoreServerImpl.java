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
package pathstore.system;

import java.io.IOException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCache;
import pathstore.common.Role;
import pathstore.exception.PathMigrateAlreadyGoneException;

import org.apache.commons.cli.*;
import pathstore.util.PathStoreSchema;
import pathstore.util.SchemaInfoV2;

/** TODO: Comment */
public class PathStoreServerImpl implements PathStoreServer {
  private final Logger logger = LoggerFactory.getLogger(PathStoreServerImpl.class);

  @Override // child calls this (maybe client or child node)
  public String addUserCommandEntry(
      String user, String keyspace, String table, byte[] clauses, int limit)
      throws RemoteException, PathMigrateAlreadyGoneException {

    // System.out.println("In addUserCommandEntry " + user + ":"+ keyspace + ":" + table + " " +
    // clauses);

    try {
      QueryCache.getInstance().updateDeviceCommandCache(user, keyspace, table, clauses, limit);
    } catch (Exception e) {
      if (e instanceof PathMigrateAlreadyGoneException)
        throw new RemoteException("PathMigrateAlreadyGoneException");
      else throw new RemoteException(e.getMessage());
    }

    return "server says hello! in user command entry";
  }

  public String addQueryEntry(String keyspace, String table, byte[] clauses, int limit)
      throws RemoteException {
    logger.info("In addQueryEntry " + keyspace + ":" + table + " " + clauses);

    long d = System.nanoTime();
    try {
      QueryCache.getInstance().updateCache(keyspace, table, clauses, limit);
      //			System.out.println("^^^^^^^^^^^^^^^^ time to reply took: " + Timer.getTime(d));

    } catch (ClassNotFoundException | IOException e) {
      throw new RemoteException(e.getMessage());
    }

    return "server says hello!";
  }

  @Override
  public UUID createQueryDelta(
      String keyspace, String table, byte[] clauses, UUID parentTimestamp, int nodeID, int limit)
      throws RemoteException {
    logger.info(
        "In createQueryDelta "
            + keyspace
            + ":"
            + table
            + " "
            + clauses
            + " pts:"
            + parentTimestamp.timestamp()
            + " "
            + nodeID);

    try {
      return QueryCache.getInstance()
          .createDelta(keyspace, table, clauses, parentTimestamp, nodeID, limit);
    } catch (ClassNotFoundException | IOException e) {
      throw new RemoteException(e.getMessage());
    }
  }

  private static void parseCommandLineArguments(String args[]) {
    Options options = new Options();

    // options.addOption( "a", "all", false, "do not hide entries starting with ." );

    options.addOption(
        Option.builder("r")
            .longOpt("role")
            .desc("[CLIENT|SERVER|ROOTSERVER]")
            .hasArg()
            .argName("ROLE")
            .build());

    options.addOption(
        Option.builder().longOpt("rmiport").desc("NUMBER").hasArg().argName("PORT").build());

    options.addOption(
        Option.builder().longOpt("rmiportparent").desc("NUMBER").hasArg().argName("PORT").build());

    options.addOption(
        Option.builder().longOpt("cassandraport").desc("NUMBER").hasArg().argName("PORT").build());

    options.addOption(
        Option.builder()
            .longOpt("cassandraportparent")
            .desc("NUMBER")
            .hasArg()
            .argName("PORT")
            .build());

    options.addOption(
        Option.builder("n").longOpt("nodeid").desc("Number").hasArg().argName("nodeid").build());

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("utility-name", options);

      System.exit(1);
      return;
    }

    if (cmd.hasOption("role")) {
      switch (cmd.getOptionValue("role")) {
        case "SERVER":
          PathStoreProperties.getInstance().role = Role.SERVER;
          break;
        case "ROOTSERVER":
          PathStoreProperties.getInstance().role = Role.ROOTSERVER;
          break;
        case "CLIENT":
          PathStoreProperties.getInstance().role = Role.CLIENT;
          break;
      }
    }

    if (cmd.hasOption("rmiport"))
      PathStoreProperties.getInstance().RMIRegistryPort =
          Integer.parseInt(cmd.getOptionValue("rmiport"));

    if (cmd.hasOption("rmiportparent"))
      PathStoreProperties.getInstance().RMIRegistryParentPort =
          Integer.parseInt(cmd.getOptionValue("rmiportparent"));

    if (cmd.hasOption("cassandraport"))
      PathStoreProperties.getInstance().CassandraPort =
          Integer.parseInt(cmd.getOptionValue("cassandraport"));

    if (cmd.hasOption("cassandraportparent"))
      PathStoreProperties.getInstance().CassandraParentPort =
          Integer.parseInt(cmd.getOptionValue("cassandraportparent"));

    if (cmd.hasOption("nodeid"))
      PathStoreProperties.getInstance().NodeID = Integer.parseInt(cmd.getOptionValue("nodeid"));
  }

  /**
   * TODO: Move to {@link pathstore.system.PathStoreServerImpl}
   *
   * @return whether the pathstore_applications exists or not
   */
  private static boolean checkIfApplicationsExist(final Session session) {
    ResultSet resultSet =
        session.execute(
            "SELECT * FROM system_schema.keyspaces where keyspace_name='pathstore_applications'");
    return resultSet.all().size() > 0;
  }

  /**
   * TODO: Allow for updates of pathstore_applications schema. Currently this only supports one
   * TODO: Move static strings TODO: Also insert the perculated schemas into the
   * pathstore_application.apps table
   *
   * @param parent parent direct connect session
   * @param local local direct connect session
   */
  private static void getApplicationSchemaFromParent(final Session parent, final Session local) {
    ResultSet resultSet = parent.execute("SELECT * FROM pathstore_applications.apps");

    SchemaInfoV2 localInfo = new SchemaInfoV2(local);

    for (String keyspace : localInfo.getAllKeySpaces()) local.execute("drop keyspace " + keyspace);

    for (Row row : resultSet) {
      int appId = row.getInt("appid");
      String appName = row.getString("app_name");
      String schemaName = row.getString("schema_name");
      String augmentedSchema = row.getString("augmented_schema");

      String[] commands = augmentedSchema.split(";");

      for (String s : commands) {
        s = s.trim();
        if (s.length() > 0) local.execute(s);
      }

      Insert insert = QueryBuilder.insertInto("pathstore_applications", "apps");
      insert.value("appid", appId);
      insert.value("app_name", appName);
      insert.value("schema_name", schemaName);
      insert.value("augmented_schema", augmentedSchema);

      local.execute(insert);
    }
  }

  public static void main(String args[]) {
    try {

      parseCommandLineArguments(args);

      PathStoreServerImpl obj = new PathStoreServerImpl();
      PathStoreServer stub = (PathStoreServer) UnicastRemoteObject.exportObject(obj, 0);

      System.setProperty(
          "java.rmi.server.hostname", PathStoreProperties.getInstance().RMIRegistryIP);
      Registry registry =
          LocateRegistry.createRegistry(PathStoreProperties.getInstance().RMIRegistryPort);

      try {
        registry.bind("PathStoreServer", stub);
      } catch (Exception ex) {
        registry.rebind("PathStoreServer", stub);
      }

      Session local = PathStorePriviledgedCluster.getInstance().connect();

      if (!new SchemaInfoV2(local).getAllKeySpaces().contains("pathstore_applications"))
        PathStoreSchemaLoader.loadApplicationSchema(local);

      System.err.println("PathStoreServer ready");

      if (PathStoreProperties.getInstance().role != Role.ROOTSERVER) {
        PathStoreSchemaLoader.getInstance();
        PathStorePullServer pullServer = new PathStorePullServer();
        pullServer.start();
        PathStorePushServer pushServer = new PathStorePushServer();
        pushServer.start();
        pushServer.join();
      }

    } catch (Exception e) {
      System.err.println("PathStoreServer exception: " + e.toString());
      e.printStackTrace();
    }
  }
}

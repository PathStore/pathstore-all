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

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import com.datastax.driver.core.Session;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreServer;
import pathstore.common.Role;

import org.apache.commons.cli.*;
import pathstore.system.schemaloader.PathStoreSchemaLoaderUtils;
import pathstore.util.SchemaInfo;

public class PathStoreServerImpl {

  private static void parseCommandLineArguments(final String args[]) {
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
   * Startup tasks:
   *
   * <p>0: setup rmi server 1: load applications keyspace 2: write to topology table 3: start
   * daemons
   *
   * @param args
   */
  public static void main(final String args[]) {
    try {

      parseCommandLineArguments(args);

      Session local = PathStorePriviledgedCluster.getInstance().connect();

      PathStoreServerImplRMI obj = new PathStoreServerImplRMI();
      PathStoreServer stub = (PathStoreServer) UnicastRemoteObject.exportObject(obj, 0);

      System.out.println(PathStoreProperties.getInstance().RMIRegistryIP);

      System.setProperty("java.rmi.server.hostname", "127.0.0.1");
      Registry registry =
          LocateRegistry.createRegistry(PathStoreProperties.getInstance().RMIRegistryPort);

      try {
        registry.bind("PathStoreServer", stub);
        PathStoreSchemaLoaderUtils.writeTaskDone(local, 0);
      } catch (Exception ex) {
        registry.rebind("PathStoreServer", stub);
      }

      if (!SchemaInfo.getInstance().getSchemaInfo().containsKey("pathstore_applications")) {
        PathStoreSchemaLoaderUtils.loadApplicationSchema(local);
        PathStoreSchemaLoaderUtils.writeTaskDone(local, 1);
      }

      SchemaInfo.getInstance().reset();

      new TopologyUpdater().updateTable();
      PathStoreSchemaLoaderUtils.writeTaskDone(local, 2);

      System.err.println("PathStoreServer ready");

      PathStoreSchemaLoaderUtils.writeTaskDone(local, 3);
      obj.startDaemons();

    } catch (Exception e) {
      System.err.println("PathStoreServer exception: " + e.toString());
      e.printStackTrace();
    }
  }
}

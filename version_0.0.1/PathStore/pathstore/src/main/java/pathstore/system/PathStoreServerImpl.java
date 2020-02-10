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
import pathstore.util.SchemaInfoV2;

/**
 * TODO: Comment
 *
 * <p>TODO: Change all daemons to inherit same parent class
 */
public class PathStoreServerImpl extends PathStoreServerImplRMI {

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

  public static void main(String args[]) {
    try {

      parseCommandLineArguments(args);

      PathStoreServerImpl obj = new PathStoreServerImpl();
      PathStoreServer stub = (PathStoreServer) UnicastRemoteObject.exportObject(obj, 0);

      System.out.println(PathStoreProperties.getInstance().RMIRegistryIP);

      System.setProperty("java.rmi.server.hostname", "127.0.0.1");
      Registry registry =
          LocateRegistry.createRegistry(PathStoreProperties.getInstance().RMIRegistryPort);

      try {
        registry.bind("PathStoreServer", stub);
      } catch (Exception ex) {
        registry.rebind("PathStoreServer", stub);
      }

      Session local = PathStorePriviledgedCluster.getInstance().connect();

      SchemaInfoV2 schemaInfoV2 = new SchemaInfoV2(local);

      // TODO: Temporary, implement reloading
      if (PathStoreProperties.getInstance().role != Role.ROOTSERVER) {
        for (String s : schemaInfoV2.getAllKeySpaces()) {
          local.execute("drop keyspace " + s);
        }
        schemaInfoV2.generate();
      }

      if (!schemaInfoV2.getAllKeySpaces().contains("pathstore_applications"))
        PathStoreSchemaLoader.loadApplicationSchema(local);

      obj.startDaemons();

      System.err.println("PathStoreServer ready");

    } catch (Exception e) {
      System.err.println("PathStoreServer exception: " + e.toString());
      e.printStackTrace();
    }
  }
}

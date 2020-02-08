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

import java.util.HashMap;
import java.util.List;

import com.datastax.driver.core.querybuilder.Clause;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pathstore.common.PathStoreProperties;
import pathstore.common.QueryCache;
import pathstore.common.QueryCacheEntry;

/** TODO: Comment */
public class PathStorePullServer extends Thread {
  private final Logger logger = LoggerFactory.getLogger(PathStorePullServer.class);

  public PathStorePullServer() {}

  private void pull() {
    //		logger.info("Run pull");
    HashMap<String, HashMap<String, List<QueryCacheEntry>>> entries =
        QueryCache.getInstance().getEntries();

    for (String keyspace : entries.keySet()) {
      HashMap<String, List<QueryCacheEntry>> tables = entries.get(keyspace);

      for (String table : tables.keySet()) {
        List<QueryCacheEntry> cache_entries = tables.get(table);

        try {
          for (QueryCacheEntry cache_entry : cache_entries) {
            if (cache_entry.isReady() && cache_entry.getIsCovered() == null)
              QueryCache.getInstance().fetchDelta(cache_entry);
          }
        } catch (Exception e) {
          System.out.println("problem while looping over cache_entries");
          e.printStackTrace();
        }
      }
    }
  }

  public synchronized void run() {
    logger.info("Spawned pathstore pull server thread");

    while (true) {
      try {
        pull();
        this.wait(PathStoreProperties.getInstance().PullSleep);
      } catch (InterruptedException e) {
        System.err.println("PathStorePullServer exception: " + e.toString());
        e.printStackTrace();
      }
    }
  }

  private static void parseCommandLineArguments(String args[]) {
    Options options = new Options();

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

    if (cmd.hasOption("nodeid"))
      PathStoreProperties.getInstance().NodeID = Integer.parseInt(cmd.getOptionValue("nodeid"));
  }

  public static void main(String args[]) {
    try {

      parseCommandLineArguments(args);

      PathStorePullServer server = new PathStorePullServer();
      server.run();
      server.join();

    } catch (Exception e) {
      System.err.println("PathStorePullServer exception: " + e.toString());
      e.printStackTrace();
    }
  }
}

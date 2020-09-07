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

import com.datastax.driver.core.Statement;
import org.apache.commons.cli.*;
import pathstore.common.PathStoreProperties;
import pathstore.common.QueryCache;
import pathstore.common.QueryCacheEntry;
import pathstore.sessions.SessionToken;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/** TODO: Comment */
public class PathStorePullServer implements Runnable {

  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStorePullServer.class);

  /**
   * For all entries in the qc that aren't covered fetch their delta.
   *
   * <p>Entries are added to the qc by {@link pathstore.client.PathStoreSession#execute(Statement)}
   * and {@link pathstore.client.PathStoreSession#execute(Statement, SessionToken)}
   */
  private void pull() {
    ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> entries =
        QueryCache.getInstance().getEntries();

    for (String keyspace : entries.keySet()) {
      ConcurrentMap<String, List<QueryCacheEntry>> tables = entries.get(keyspace);

      for (String table : tables.keySet()) {
        List<QueryCacheEntry> cache_entries = tables.get(table);

        try {
          for (QueryCacheEntry cache_entry : cache_entries) {
            if (cache_entry.isReady() && cache_entry.getIsCovered() == null)
              QueryCache.getInstance().fetchDelta(cache_entry);
          }
        } catch (Exception e) {
          System.out.println("problem while looping over cache_entries");
          this.logger.error(e);
        }
      }
    }
  }

  public synchronized void run() {
    logger.info("Pull Server spawned");
    while (true) {
      try {
        // logger.debug("Pull server ran");
        pull();
        Thread.sleep(PathStoreProperties.getInstance().PullSleep);
      } catch (Exception e) {
        System.err.println("PathStorePullServer exception: " + e.toString());
        this.logger.error(e);
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
}

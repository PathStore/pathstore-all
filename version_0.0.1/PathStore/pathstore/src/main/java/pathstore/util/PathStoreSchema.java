package pathstore.util;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import pathstore.common.PathStoreProperties;
import pathstore.system.PathStorePriviledgedCluster;

import com.datastax.driver.core.Session;

public class PathStoreSchema {

	
	private static void parseCommandLineArguments(String args[]) {
		Options options = new Options();

		options.addOption( Option.builder().longOpt( "cassandraport" )
                .desc( "NUMBER" )
                .hasArg()
                .argName("cassandraport")
                .build() );
		
		options.addOption( Option.builder().longOpt( "rmiport" )
                .desc( "NUMBER" )
                .hasArg()
                .argName("PORT")
                .build() );

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

        if (cmd.hasOption("cassandraport")) 
        	PathStoreProperties.getInstance().CassandraPort = Integer.parseInt(cmd.getOptionValue("cassandraport"));
     
        if (cmd.hasOption("rmiport")) 
        	PathStoreProperties.getInstance().RMIRegistryPort = Integer.parseInt(cmd.getOptionValue("rmiport"));
     
        
	}
	
	static void createPathStoreScheme() {
		PathStorePriviledgedCluster cluster = PathStorePriviledgedCluster.getInstance();
		Session session = cluster.connect();

		try{ 	
			session.execute("DROP KEYSPACE system_pathstore");
		}
		catch (Exception ex) {
			
		}
		
		session.execute("CREATE KEYSPACE IF NOT EXISTS system_pathstore WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1 }  AND durable_writes = false;");

		String table = "" +
		"CREATE TABLE system_pathstore.queries (" +
		"	    keyspace_name text," +
		"	    table_name text," +
		"	    clauses text," +
		"		PRIMARY KEY(keyspace_name,table_name,clauses)" +
		"	) WITH CLUSTERING ORDER BY (table_name DESC,clauses DESC)" +
		"		AND bloom_filter_fp_chance = 0.01" +
		"	    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}" +
		"	    AND comment = 'table definitions'" +
		"	    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}" +
		"	    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}" +
		"	    AND crc_check_chance = 1.0" +
		"	    AND dclocal_read_repair_chance = 0.0" +
		"	    AND default_time_to_live = 0" +
		"	    AND gc_grace_seconds = 604800" +
		"	    AND max_index_interval = 2048" +
		"	    AND memtable_flush_period_in_ms = 3600000" +
		"	    AND min_index_interval = 128" +
		"	    AND read_repair_chance = 0.0" +
		"	    AND speculative_retry = '99PERCENTILE';";

		session.execute(table);
				
		//session.close();

	}
	
	static public void main(String args[]) {
		parseCommandLineArguments(args);
        
		createPathStoreScheme();		
	}
	
}

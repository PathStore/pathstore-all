package pathstore.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import pathstore.client.PathStoreCluster;
import pathstore.common.PathStoreProperties;
import pathstore.common.Role;
import pathstore.system.PathStorePriviledgedCluster;
import pathstore.util.SchemaInfo.Column;
import pathstore.util.SchemaInfo.Table;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.io.Files;

public class ApplicationSchema {
	static private ApplicationSchema instance=null;

	static private String command;

	private static int appid=1;
	private static String filename="/tmp/pathstore_demo.cql";
	private static String schemaname="pathstore_demo";
	private static String appname="demo";



	static public ApplicationSchema getInstance() {
		if (ApplicationSchema.instance == null) 
			ApplicationSchema.instance = new ApplicationSchema();
		return ApplicationSchema.instance;
	}

	PathStorePriviledgedCluster priviledged_cluster;
	PathStoreCluster cluster;


	public ApplicationSchema() {
		priviledged_cluster = PathStorePriviledgedCluster.getInstance();
		cluster = PathStoreCluster.getInstance();
	}


	private static String readFileContents(String fileName) throws IOException {
		File file = new File(fileName);
		int lenght = (int) file.length();
		byte [] buffer = new byte[lenght];

		FileInputStream inStream = new FileInputStream(file);

		for (int offset=0; offset < lenght;) 
			offset += inStream.read(buffer, offset, lenght);

		inStream.close();
		return new String(buffer);
	}

	public void ImportFromFile(String fileName) {
		try {
			Session session = priviledged_cluster.connect();

			String script = readFileContents(fileName);

			String [] commands = script.split(";");

			for (String s : commands) {
				String s2 = s.trim();
				System.out.println(s2);
				if (s2.length() > 0)
					session.execute(s2);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Hossein
	public static void setParameters(String serverIp, int serverPort, String rmiServer, int rmiPort, String role)
	{
		if(role.equals("ROOTSERVER"))
		{
			PathStoreProperties.getInstance().RMIRegistryParentPort = rmiPort;
			PathStoreProperties.getInstance().RMIRegistryParentIP = rmiServer;
			PathStoreProperties.getInstance().CassandraParentIP = serverIp;
			PathStoreProperties.getInstance().CassandraParentPort = serverPort;
			PathStoreProperties.getInstance().RMIRegistryPort = rmiPort;
			PathStoreProperties.getInstance().RMIRegistryIP = rmiServer;
			PathStoreProperties.getInstance().CassandraIP = serverIp;
			PathStoreProperties.getInstance().CassandraPort = serverPort;
			PathStoreProperties.getInstance().role = Role.ROOTSERVER;
		}
		ApplicationSchema.getInstance();
	}


	private static void parseCommandLineArguments(String args[]) {
		Options options = new Options();

		options.addOption(Option.builder().longOpt( "command" )
				.desc( "STRING" )
				.hasArg()
				.argName("cmdname")
				.build() );


		options.addOption( Option.builder().longOpt( "appid" )
				.desc( "NUMBER" )
				.hasArg()
				.argName("id")
				.build() );

		options.addOption( Option.builder().longOpt( "filename" )
				.desc( "STRING" )
				.hasArg()
				.argName("filename")
				.build() );

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

		options.addOption( Option.builder().longOpt( "appname" )
				.desc( "STRING" )
				.hasArg()
				.argName("appname")
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

		if (cmd.hasOption("command")) 
			ApplicationSchema.command = cmd.getOptionValue("command");

		if (cmd.hasOption("appid")) 
			ApplicationSchema.appid = Integer.parseInt(cmd.getOptionValue("appid"));

		if (cmd.hasOption("filename")) 
			ApplicationSchema.filename = cmd.getOptionValue("filename");

		if (cmd.hasOption("appname"))
		{
			ApplicationSchema.appname = cmd.getOptionValue("appname");
			ApplicationSchema.schemaname= "pathstore_"+ApplicationSchema.appname;;
		}

		if (cmd.hasOption("cassandraport")) 
			PathStoreProperties.getInstance().CassandraPort = Integer.parseInt(cmd.getOptionValue("cassandraport"));

		if (cmd.hasOption("rmiport")) 
			PathStoreProperties.getInstance().RMIRegistryPort = Integer.parseInt(cmd.getOptionValue("rmiport"));


	}

	static private void initPathStoreApplicationSchema() throws IOException, InterruptedException {
		System.out.println("start drop schema");
		ApplicationSchema.getInstance().dropSchema("pathstore_applications");
		System.out.println("end drop schema");		
		ApplicationSchema.getInstance().createPathStoreApplicationSchema();
		//	String orginalSchema = ApplicationSchema.getInstance().getSchema("pathstore_applications");
		ApplicationSchema.getInstance().augmentSchema("pathstore_applications");
		//	ApplicationSchema.getInstance().newApp("applications", "pathstore_applications", orginalSchema);
		//	String augmentedSchema = ApplicationSchema.getInstance().getSchema("pathstore_applications");
		//	ApplicationSchema.getInstance().storeAugmentedSchema("applications",augmentedSchema);
	}

	//Hossein
	static public String importApplicationUsingSchemaString(int appId, String appName, String schemaName, String schema) throws IOException, InterruptedException {
		ApplicationSchema.getInstance().dropSchema(schemaName);
		//ApplicationSchema.getInstance().newApp(appId, appName, schemaName, schema);
		ApplicationSchema.getInstance().createNewDB(schema);
		ApplicationSchema.getInstance().augmentSchema(schemaName);
		return ApplicationSchema.getInstance().getSchema(schemaName);
		//ApplicationSchema.getInstance().storeAugmentedSchema(appId,schema);
	}


	static private void importApplication(int appId, String appName, String schemaName, String schemaFileName) throws IOException, InterruptedException {
		ApplicationSchema.getInstance().dropSchema(schemaName);
		String schema = readFileContents(schemaFileName);
		ApplicationSchema.getInstance().newApp(appId, appName, schemaName, schema);
		ApplicationSchema.getInstance().createOriginalDB(appId);
		ApplicationSchema.getInstance().augmentSchema(schemaName);
		schema = ApplicationSchema.getInstance().getSchema(schemaName);
		ApplicationSchema.getInstance().storeAugmentedSchema(appId,schema);
	}

	//Manager calls this
	static private void deployApplication(int appId, boolean dropSchema) {
		ApplicationSchema.getInstance().createAugmentedDB(appId, dropSchema);
	}



	static public void main(String args[]) {
		try {

			parseCommandLineArguments(args);

			switch(ApplicationSchema.command) {
			case "init":
				// call to startup a new pathstore node
				initPathStoreApplicationSchema();
				break;

			case "import":
				// import a 3rd party application into pathstore
				// arguments
				// --filename /opt/file.cql --appid 1
				System.out.println("importing application with appid: " + appid + " appname: "+appname + " schemaname: "+ schemaname + " filenam:" + filename);
				importApplication(ApplicationSchema.appid,ApplicationSchema.appname,ApplicationSchema.schemaname,ApplicationSchema.filename);
				break;

			case "deploy":
				// creates schema of a given 3rd party application on a specific pathstore node
				// arguments
				// --appid 1
				long d = System.nanoTime();
				deployApplication(ApplicationSchema.appid, true);
				System.out.println("deployment took: " + (System.nanoTime()-d)/1000000.0);
				break;
			}

			System.exit(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void storeAugmentedSchema(int appId, String augmentedSchema) {
		Session session = cluster.connect();

		Insert insert = QueryBuilder.insertInto("pathstore_applications", "apps");
		insert.value("appid", appId);
		insert.value("app_schema_augmented", augmentedSchema);


		session.execute(insert);
	}



	private String getSchema(String schemaName) throws IOException, InterruptedException {

		//		File file = File.createTempFile("pathstore", ".tmp");
		//		String command = "describe keyspace " + schemaName + "\n";
		//		Files.write(command.getBytes(), file);
		//	    file.deleteOnExit();
		//		
		//		String path =  PathStoreProperties.getInstance().CassandraPath +  "/bin";
		//		command = path + "/cqlsh.py " + PathStoreProperties.getInstance().CassandraParentIP  + "  " +
		//				PathStoreProperties.getInstance().CassandraPort + " -f " + file.getPath();
		//		
		//		System.out.println("command is: " + command);
		//		
		//		Process p = Runtime.getRuntime().exec(command);
		//	    p.waitFor();
		//	    
		//	    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		//	
		//	    String line = "";
		//	    String schema = "";
		//	    while ((line = reader.readLine())!= null) 
		//	    	schema += line + "\n";


		String schema = cluster.getMetadata().getKeyspace(schemaName).exportAsString();
		System.out.println("schema is: " + schema);	

		return schema;
	}

	private void augmentSchema(String schemaName) throws IOException, InterruptedException {
		HashMap<Table,List<Column>> keyspaceInfo = SchemaInfo.getInstance().getKeySpaceInfo(schemaName);

		for (Table table : keyspaceInfo.keySet()) {

			System.out.println(table.table_name);

			if (table.table_name.startsWith("local_") == false) {
				//System.out.println("table not starting with local!!");
				createAugmentedTable(table, keyspaceInfo.get(table));
				createViewTable(table, keyspaceInfo.get(table));
			}

		}
	}

	private void dropTable(Table table) {
		Session session = priviledged_cluster.connect();

		String query = "DROP TABLE " +  table.keyspace_name + "." + table.table_name;
		session.execute(query);
	}

	private void createAugmentedTable(Table table, List<Column> columns) {
		Session session = priviledged_cluster.connect();

		dropTable(table);

		String query = "CREATE TABLE " +  table.keyspace_name + "." + table.table_name + "(";

		for (Column col : columns) {
			String type = col.type.compareTo("counter")==0? "int" : col.type;
			query += col.column_name + " " + type + ",";
		}

		query += "pathstore_version timeuuid,";
		query += "pathstore_parent_timestamp timeuuid,";
		query += "pathstore_dirty boolean,";  
		query += "pathstore_deleted boolean,";   
		query += "pathstore_insert_sid text,";
		query += "pathstore_node int,";   
		query += "PRIMARY KEY(";

		for (Column col : columns) 
			if (col.kind.compareTo("partition_key") == 0) 
				query += col.column_name +  ",";
		for (Column col : columns) 
			if (col.kind.compareTo("clustering") == 0) 
				query += col.column_name +  ",";

		query += "pathstore_version) ";

		query += ")";

		query += "WITH CLUSTERING ORDER BY ("; 


		//Hossein
		int i=0;
		for (Column col : columns) 
			if (col.kind.compareTo("clustering") == 0)
				query += col.column_name +  " " + col.clustering_order + ",";

		query += "pathstore_version DESC) ";

		query +="	    AND caching = " + mapToString(table.caching) + 
				"	    AND comment = '" + table.comment + "'" +
				"	    AND compaction = " + mapToString(table.compaction) +
				"	    AND compression = " + mapToString(table.compression) + 
				"	    AND crc_check_chance = " + table.crc_check_chance + 
				"	    AND dclocal_read_repair_chance = " + table.dclocal_read_repair_chance +
				"	    AND default_time_to_live = " + table.default_time_to_live + 
				"	    AND gc_grace_seconds = " + table.gc_grace_seconds +
				"	    AND max_index_interval = " + table.max_index_interval +
				"	    AND memtable_flush_period_in_ms = " + table.memtable_flush_period_in_ms +
				"	    AND min_index_interval = " + table.min_index_interval +
				"	    AND read_repair_chance = " + table.read_repair_chance +
				"	    AND speculative_retry = '" + table.speculative_retry + "'";

		System.out.println("query: \n\n\n" + query);
		session.execute(query);

		query = "CREATE INDEX ON " +  table.keyspace_name + "." + table.table_name + " (pathstore_dirty)";

		session.execute(query);

		query = "CREATE INDEX ON " +  table.keyspace_name + "." + table.table_name + " (pathstore_deleted)";

		session.execute(query);

		query = "CREATE INDEX ON " +  table.keyspace_name + "." + table.table_name + " (pathstore_insert_sid)";

		session.execute(query);

	}

	private void createViewTable(Table table, List<Column> columns) {
		Session session = priviledged_cluster.connect();

		String query = "CREATE TABLE " +  table.keyspace_name + ".view_" + table.table_name + "(";

		query += "pathstore_view_id uuid,";

		for (Column col : columns) {
			String type = col.type.compareTo("counter")==0? "int" : col.type;
			query += col.column_name + " " + type + ",";
		}

		//BUG?!
		query += "pathstore_version timeuuid,";
		query += "pathstore_parent_timestamp timeuuid,";
		query += "pathstore_dirty boolean,";  
		query += "pathstore_deleted boolean,";
		//		query += "pathstore_insert_device,";
		query += "pathstore_node int,";   

		query += "PRIMARY KEY(pathstore_view_id,";

		for (Column col : columns) 
			if (col.kind.compareTo("regular") != 0) 
				query += col.column_name +  ",";

		query += "pathstore_version) ";

		query += ")";

		session.execute(query);
	}


	private String mapToString(Object map) {

		Map<String,String>m = (Map<String,String>)map;

		String result = "{";

		for (String key : m.keySet()) 
			result += "'" + key + "':'" + m.get(key) + "',";

		return result.substring(0, result.length()-1) + "}";
	}

	private void newApp(int appId, String appName, String schemaName, String script) throws IOException {
		Session session = cluster.connect();

		Insert insert = QueryBuilder.insertInto("pathstore_applications", "apps");
		insert.value("appid", appId);
		insert.value("app_name", appName);
		insert.value("schema_name", schemaName);
		insert.value("app_schema", script);

		session.execute(insert);

	}

	public void dropSchema(String schemaName) {
		long d = System.nanoTime();

		Session session = priviledged_cluster.connect();
		System.out.println(((System.nanoTime()-d)/1000000.0)+"preparing...");
		PreparedStatement prepared = session.prepare("drop keyspace if exists " + schemaName);
		BoundStatement bound = prepared.bind();
		System.out.println(((System.nanoTime()-d)/1000000.0)+"executing...");
		session.execute(bound);

		//		String query = ("drop keyspace if exists " + schemaName);
		//		Session session = priviledged_cluster.connect();
		//		System.out.println(((System.nanoTime()-d)/1000000.0)+"connected...");
		//		session.execute(query);

		System.out.println(((System.nanoTime()-d)/1000000.0)+"executing done");
	}


	public void createOriginalDB(int appId) {
		Session session = cluster.connect();

		Select select = QueryBuilder.select().all().from("pathstore_applications", "apps");
		select.where(QueryBuilder.eq("appid", appId));
		ResultSet results = session.execute(select);

		// switch to priviledged session
		session = priviledged_cluster.connect();


		for (Row row : results) {
			String script = row.getString("app_schema");
			String schemaName = row.getString("schema_name");

			dropSchema(schemaName);

			String [] commands = script.split(";");

			for (String s : commands) {
				String s2 = s.trim();
				//System.out.println(s2);
				if (s2.length() > 0)
					session.execute(s2);
			}
		}
	}

	//Hossein
	public void createNewDB(String schema) {
		Session session = priviledged_cluster.connect();
		String [] commands = schema.split(";");

		for (String s : commands) {
			String s2 = s.trim();
			System.out.println(s2);
			if (s2.length() > 0)
				session.execute(s2);
		}
	}

	public void createAugmentedDB(int appId, boolean dropSchema) {
		Session session = cluster.connect();

		Select select = QueryBuilder.select().all().from("pathstore_applications", "apps");
		select.where(QueryBuilder.eq("appid", appId));
		ResultSet results = session.execute(select);

		// switch to priviledged session
		session = priviledged_cluster.connect();


		for (Row row : results) {
			String script = row.getString("app_schema_augmented");
			String schemaName = row.getString("schema_name");

			if(dropSchema)
			{
				dropSchema(schemaName);
			}


			String [] commands = script.split(";");
			int i=0;
			for (String s : commands) {
				String s2 = s.trim();
				System.out.println(s2);
				if (s2.length() > 0)
				{
					if(i==0)
					{
						session.execute(s2);
						i++;
					}
					else
						session.execute(s2);
				}
			}
		}
	}


	private void createPathStoreApplicationSchema() {
		Session session = priviledged_cluster.connect();

		try{ 	
			session.execute("DROP KEYSPACE pathstore_applications");
		}
		catch (Exception ex) {

		}


		session.execute("CREATE KEYSPACE IF NOT EXISTS pathstore_applications WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1 }  AND durable_writes = false;");

		String table = "" +
				"CREATE TABLE pathstore_applications.apps (" +
				"		appid int PRIMARY KEY," +
				"		code blob," +
				"		funcs list<int>," +
				"		owner text," +
				"		root_domain text," +
				"	    app_name text," +
				"	    schema_name text," +
				"	    app_schema text," +
				"	    app_schema_augmented text" +
				"	) WITH bloom_filter_fp_chance = 0.01" +
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


		session.execute("CREATE INDEX rroot_domain ON pathstore_applications.apps (root_domain);");


		table = "" +
				"CREATE TABLE pathstore_applications.funcs (" +
				"	    funcid int PRIMARY KEY," +
				"	    appid int," +
				"	    deploy_strategy text," +
				"	    path text," +
				"	    sub_domain text" +
				"	) WITH bloom_filter_fp_chance = 0.01" +
				"	    AND caching = {'keys':'ALL', 'rows_per_partition':'NONE'}" +
				"	    AND comment = ''" +
				"	    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'}" +
				"	    AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}" +
				"	    AND dclocal_read_repair_chance = 0.1" +
				"	    AND default_time_to_live = 0" +
				"	    AND gc_grace_seconds = 864000" +
				"	    AND max_index_interval = 2048" +
				"	    AND memtable_flush_period_in_ms = 0" +
				"	    AND min_index_interval = 128" +
				"	    AND read_repair_chance = 0.0" +
				"	    AND speculative_retry = '99.0PERCENTILE';";

		session.execute(table);

		session.execute("CREATE INDEX aappid ON pathstore_applications.funcs (appid);");



		//creating path-monitor table:


		table = "" +
				"CREATE TABLE pathstore_applications.metrics (" +
				"    entity_type text," + 
				"    entity_name text," + 
				"    time_logged timestamp," + 
				"    metric_id text," + 
				"    metric_value double," + 
				"    PRIMARY KEY (entity_type, entity_name, time_logged, metric_id)" + 
				"	) WITH bloom_filter_fp_chance = 0.01" +
				"	    AND caching = {'keys':'ALL', 'rows_per_partition':'NONE'}" +
				"	    AND comment = ''" +
				"	    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'}" +
				"	    AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}" +
				"	    AND dclocal_read_repair_chance = 0.1" +
				"	    AND default_time_to_live = 0" +
				"	    AND gc_grace_seconds = 864000" +
				"	    AND max_index_interval = 2048" +
				"	    AND memtable_flush_period_in_ms = 0" +
				"	    AND min_index_interval = 128" +
				"	    AND read_repair_chance = 0.0" +
				"	    AND speculative_retry = '99.0PERCENTILE';";


		System.out.println(table);




		table="CREATE TABLE pathstore_applications.logs ("
				+ "    entity_type text,"
				+ "    entity_name text,"
				+ "    time_logged timestamp,"
				+ "    log text,"
				+ "    PRIMARY KEY (entity_type, entity_name, time_logged))"
				+ "    WITH bloom_filter_fp_chance = 0.01"
				+ "    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}"
				+ "    AND comment = ''"
				+ "    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}"
				+ "    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}"
				+ "    AND crc_check_chance = 1.0"
				+ "    AND dclocal_read_repair_chance = 0.1"
				+ "    AND default_time_to_live = 0"
				+ "    AND gc_grace_seconds = 864000"
				+ "    AND max_index_interval = 2048"
				+ "    AND memtable_flush_period_in_ms = 0"
				+ "    AND min_index_interval = 128"
				+ "    AND read_repair_chance = 0.0"
				+ "    AND speculative_retry = '99PERCENTILE';";

		session.execute(table);
		
		

		table="CREATE TABLE pathstore_applications.users ("
				+ "    name text,"
				+ "    lastname text,"
				+ "    username text,"
				+ "    password text,"
				+ "    email text,"
				+ "    log text,"
				+ "    PRIMARY KEY (username))"
				+ "    WITH bloom_filter_fp_chance = 0.01"
				+ "    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}"
				+ "    AND comment = ''"
				+ "    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}"
				+ "    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}"
				+ "    AND crc_check_chance = 1.0"
				+ "    AND dclocal_read_repair_chance = 0.1"
				+ "    AND default_time_to_live = 0"
				+ "    AND gc_grace_seconds = 864000"
				+ "    AND max_index_interval = 2048"
				+ "    AND memtable_flush_period_in_ms = 0"
				+ "    AND min_index_interval = 128"
				+ "    AND read_repair_chance = 0.0"
				+ "    AND speculative_retry = '99PERCENTILE';";

		session.execute(table);
		
		

		table="CREATE TABLE pathstore_applications.session ("
				+ "    sid text,"
				+ "    username text,"
				+ "    sesstion_timestamp timestamp,"
				+ "    state text,"
				+ "    current_edge text,"
				+ "    PRIMARY KEY (sid))"
				+ "    WITH bloom_filter_fp_chance = 0.01"
				+ "    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}"
				+ "    AND comment = ''"
				+ "    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}"
				+ "    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}"
				+ "    AND crc_check_chance = 1.0"
				+ "    AND dclocal_read_repair_chance = 0.1"
				+ "    AND default_time_to_live = 0"
				+ "    AND gc_grace_seconds = 864000"
				+ "    AND max_index_interval = 2048"
				+ "    AND memtable_flush_period_in_ms = 0"
				+ "    AND min_index_interval = 128"
				+ "    AND read_repair_chance = 0.0"
				+ "    AND speculative_retry = '99PERCENTILE';";

		session.execute(table);
		
		
		table ="CREATE TABLE pathstore_applications.state(" + 
				"    nodeid int," + 
				"    hostid int," + 
				"    containerid int," + 
				"    appid int," + 
				"    functionid int," + 
				"    PRIMARY KEY (nodeid, hostid, containerid, appid, functionid))   "
				+ "    WITH bloom_filter_fp_chance = 0.01"
				+ "    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}"
				+ "    AND comment = ''"
				+ "    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}"
				+ "    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}"
				+ "    AND crc_check_chance = 1.0"
				+ "    AND dclocal_read_repair_chance = 0.1"
				+ "    AND default_time_to_live = 0"
				+ "    AND gc_grace_seconds = 864000"
				+ "    AND max_index_interval = 2048"
				+ "    AND memtable_flush_period_in_ms = 0"
				+ "    AND min_index_interval = 128"
				+ "    AND read_repair_chance = 0.0"
				+ "    AND speculative_retry = '99PERCENTILE';";
		
		session.execute(table);
		
		
		table ="CREATE TABLE pathstore_applications.nodes (" + 
				"    nodeid int PRIMARY KEY," + 
				"    name text," +
				"    ps_ip text," +
				"    ps_port int," +
				"    role text," +
				"    parentid int)" 
				+ "    WITH bloom_filter_fp_chance = 0.01"
				+ "    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}"
				+ "    AND comment = ''"
				+ "    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}"
				+ "    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}"
				+ "    AND crc_check_chance = 1.0"
				+ "    AND dclocal_read_repair_chance = 0.1"
				+ "    AND default_time_to_live = 0"
				+ "    AND gc_grace_seconds = 864000"
				+ "    AND max_index_interval = 2048"
				+ "    AND memtable_flush_period_in_ms = 0"
				+ "    AND min_index_interval = 128"
				+ "    AND read_repair_chance = 0.0"
				+ "    AND speculative_retry = '99PERCENTILE';";
		
		session.execute(table);

	}


}

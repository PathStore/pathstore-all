/**********
*
* Copyright 2019 Eyal de Lara, Seyed Hossein Mortazavi, Mohammad Salehe
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
***********/
package pathstore.system;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.utils.UUIDs;

import pathstore.client.PathStoreCluster;
import pathstore.client.PathStoreResultSet;
import pathstore.client.PathStoreSession;
import pathstore.common.CommandEntryReply;
import pathstore.common.PathStoreMigrate;
import pathstore.common.PathStoreProperties;
import pathstore.common.QueryCache;
import pathstore.common.QueryCacheEntry;
import pathstore.common.Role;
import pathstore.common.Timer;
import pathstore.util.ConnectionEntry;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;
import pathstore.util.SchemaInfo.Table;
import pathstore.util.LoggingHelperClass;

public class PathStoreMigrateImpl implements PathStoreMigrate{
	static PathStoreMigrateImpl pathStoreMigrate=null;

	//config.addConfiguration(configz);
	public static boolean onlyDifferences=true;//delta list

	private String keyspace="pathstore_demo";

	private PathStoreMigrate serverstub;
	private PathStoreMigrate parentServerStub;

	ConcurrentHashMap<String, ConnectionEntry> connectionCache;
	ConcurrentHashMap<String, String> sessionLocks;
	ConcurrentHashMap<String, Session> pathStoreConnectionCache;
	ExecutorService migratorService ;

	public static PathStoreMigrateImpl getInstance() {
		if (PathStoreMigrateImpl.pathStoreMigrate == null) 
			PathStoreMigrateImpl.pathStoreMigrate = new PathStoreMigrateImpl();
		return PathStoreMigrateImpl.pathStoreMigrate;
	}

	public PathStoreMigrateImpl()	{

		PropertiesConfiguration config = new PropertiesConfiguration();
		try {
			config.load(new File("/home/mortazavi/nfs/traces/rubbos_exp.properties"));
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String only = (String)config.getProperty("onlyDifferences");//"/home/mortazavi/nfs/traces/trace-asli.txt";
		keyspace = (String)config.getProperty("keyspace");
		System.out.println("(((((((((((((((((( " + only);
		if(only.equals("false"))
			onlyDifferences=false;


		sessionLocks = new ConcurrentHashMap<>();
		connectionCache = new ConcurrentHashMap<String, ConnectionEntry>();
		migratorService= Executors.newFixedThreadPool(5);
		pathStoreConnectionCache = new ConcurrentHashMap<String, Session>();

		int parentId = PathStoreProperties.getInstance().ParentID;
		if(parentId==-1)
			setParentId();


		if(PathStoreProperties.getInstance().role!=Role.CLIENT)
		{
			System.out.println("creating PathStoreMigrateImpl registry");

			PathStoreMigrate obj = this;

			// Bind the remote object's stub in the registry
			//Registry registry = LocateRegistry.createRegistry(PathStoreProperties.getInstance().RMIRegistryPort);

			Registry registry = null;
			PathStoreMigrate stub;
			try {
				stub = (PathStoreMigrate) UnicastRemoteObject.exportObject(obj, 0);
				registry = LocateRegistry.getRegistry(PathStoreProperties.getInstance().RMIRegistryIP, PathStoreProperties.getInstance().RMIRegistryPort);
				registry.bind("PathStoreMigrate", stub);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		PathStoreSession localSession = PathStoreCluster.getInstance().connect();
		Session localSession2 = PathStorePriviledgedCluster.getInstance().connect();
		if(PathStoreProperties.getInstance().role==Role.SERVER)
			getConnectionWithParent();


	}


	public String migrate(String sid, String previousEdge) {

		//System.out.println("in migrate, going for: " + sid + " from: " + previousEdge);
		Registry registry=null;

		PathStoreProperties p = PathStoreProperties.getInstance();
		//I'm a client I should call the server
		if(p.role == Role.CLIENT)
		{
			//rSystem.out.println("PathMigrate: role is client");
			if(serverstub == null)
			{
				try {
					registry = LocateRegistry.getRegistry(PathStoreProperties.getInstance().RMIRegistryIP,PathStoreProperties.getInstance().RMIRegistryPort);
					serverstub = (PathStoreMigrate) registry.lookup("PathStoreMigrate");
					System.out.println("migration is done - client");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}

			try {
				return serverstub.migrate(sid, previousEdge);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		//server
		else
		{

			//			/System.out.println("PathMigrate: role is server/rootserver");

			sessionLocks.compute(sid,(k, v) ->		
			serverHanldeCookie(k,v,previousEdge));

			return sessionLocks.get(sid);

		}

		return null;
	}//end of migrate


	private String serverHanldeCookie(String sid, String status, String previousEdge) {
		//		System.out.println("((((((((sid: " + sid + " status: " +status + " prevEdge: "+ previousEdge);

		if(status==null)
		{		
			System.out.println("status was null, now starting to move ");

			//if not seen the cookie, then start a new thread, set it to moving
			migratorService.execute(new Runnable() {
				public void run() {
					long d = System.nanoTime();
					ConnectionEntry ce = connectionCache.get(previousEdge);
					if(ce==null)
						ce =createNewNeighborConnection(previousEdge);
					int parentId = PathStoreProperties.getInstance().ParentID;

					try {
						System.out.println("connecting to src PathstoreMigrate at: " + ce.RmiIP + ":" + ce.PortRmi);
						HashMap<String, HashMap<Object, UUID>> differenceList =null;
						long wastedDst=0;
						if(onlyDifferences)
						{
							long dd = System.nanoTime();
							differenceList  = createLatestListForKeyspace(keyspace);

							System.out.println("sending out only differences differenceList size: " + differenceList.get("users").size());
							ByteArrayOutputStream bOutput = new ByteArrayOutputStream();
							ObjectOutputStream ooutput = new ObjectOutputStream(bOutput);
							ooutput.writeObject(differenceList);
							ooutput.flush();
							byte[] bb = bOutput.toByteArray();
							System.out.println("total size in bytes of difference list is: " + bb.length);
							System.out.println("######processing time for difference list at destination: " + Timer.getTime(dd));
						}
						if(parentId!=ce.parentId)
						{
							System.out.println("not neighbors");
							long howisthispossible=System.nanoTime();


							long totalBytes=0;
							if(onlyDifferences || sid.equals("moveall"))
							{
								totalBytes = LoggingHelperClass.getTotalBytes("neighbor");
								LoggingHelperClass.writeToMainDB("153", totalBytes);
								wastedDst+=Timer.getTime(d);
								System.out.println("######processing time getting totalBytes : " + wastedDst);
							}

							ArrayList<CommandEntryReply> replies = ce.stub.sendData(sid, PathStoreProperties.getInstance().NodeID+"", false, differenceList);
							long timme = (long)Timer.getTime(d);
							System.out.println("how is this possible: " + Timer.getTime(howisthispossible) + " " + replies.size());
							System.out.println("&&&&&&&! sending data from " + sid  + " at: " + Timer.getTime(d));

							if(onlyDifferences || sid.equals("moveall"))
							{
								if(sid.equals("moveall"))
								{
									long currentTotalBytes = LoggingHelperClass.getTotalBytes("neighbor");
									System.out.println("^^^^^^^^ FINAL total bytes for transfer: " + (currentTotalBytes-totalBytes));
								}
								else
								{
									long currentTotalBytes = LoggingHelperClass.getTotalBytes("neighbor");
									long prevTotalBytes = LoggingHelperClass.readFromMainDB("007");
									long wastedOnSrc=LoggingHelperClass.readFromMainDB("wasted");
									long finalTime = timme -wastedOnSrc -wastedDst;
									System.out.println("MMMMMMMMM: " + timme + " " +  wastedDst +  " " + wastedOnSrc);
									System.out.println("^^^^^^^^ FINAL totalTime = " + finalTime);
									System.out.println("^^^^^^^^ FINAL total bytes for transfer: " + (currentTotalBytes-prevTotalBytes));
								}
							}

							long queryTime=0, commandTime=0;
							int i=0;
							for(CommandEntryReply rr : replies)
							{
								rr.convertClauses();
								long tmp= System.nanoTime();
								QueryCache.getInstance().updateDeviceCommandCache(rr.getSid(), rr.getKeyspace(), rr.getTable(), rr.getConverted(),rr.getLimit());
								commandTime+=(System.nanoTime()-tmp)/1000;
								tmp=System.nanoTime();
								QueryCache.getInstance().updateCacheByMigration(rr.getKeyspace(), rr.getTable(), rr.getConverted(),rr.getLimit());
								queryTime+=(System.nanoTime()-tmp)/1000;
								//System.out.println("query " + i++ +" took: " + (System.nanoTime()-tmp)/1000);
							}
							System.out.println("&&&&&&&&&! done updating caches at: " + Timer.getTime(d) + "  query: " + (queryTime/1000.0)+ " command: " + (commandTime/1000.0));
						}
						else
						{
							long queryTime=0, commandTime=0;
							System.out.println("neighbors");
							ArrayList<CommandEntryReply> replies = ce.stub.sendData(sid, PathStoreProperties.getInstance().NodeID+"",  true, differenceList);
							System.out.println("got this many replies: (QCentries): " + replies.size()+ " /" + Timer.getTime(d));
							long tmp= System.nanoTime();
							ArrayList<CommandEntryReply> differences= QueryCache.getInstance().reconsolidateWithNeighbor(replies);
							System.out.println("after reconsilidation "+ Timer.getTime(d) + " need to change: " + differences.size());
							if(differences!=null && differences.size()>0)
								parentServerStub.fetchFromParent(differences, PathStoreProperties.getInstance().NodeID+"");
							queryTime+=(System.nanoTime()-tmp)/1000;
							System.out.println("&&&&&&&&&! done updating caches at: " + Timer.getTime(d) + "  query: " + (queryTime/1000.0)+ " command: " + (commandTime/1000.0));

						}
						System.out.println("&&&&&&&&&&&&&&! setting " + sid  + " current edge session: " + Timer.getTime(d));
						setSessionCurrentEdge(sid);
						System.out.println("&&&&&&&&&&&&&&&&&! finished moving " + sid  + " took: " + Timer.getTime(d));
						sessionLocks.put(sid, "MOVED");

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			return "MOVING";
		}

		else if(status.equals("MOVING"))
			return "MOVING";

		else if(status.equals("MOVED"))
			return "MOVED";

		return null;
		//sessionLocks.computeIfAbsent(sid, x -> {

		//System.out.println("asking PathStoreMigrateImpl for a migration");
		//connectionCache.computeIfAbsent(previousEdge , xx-> 

	}

	private PathStoreMigrate getConnectionWithParent()
	{
		Registry registry = null;
		if(parentServerStub== null)
		{
			try {
				registry = LocateRegistry.getRegistry(PathStoreProperties.getInstance().RMIRegistryParentIP,PathStoreProperties.getInstance().RMIRegistryParentPort);
				parentServerStub = (PathStoreMigrate) registry.lookup("PathStoreMigrate");
				System.out.println("migration is done - client");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		return parentServerStub;
	}

	private void setParentId() {
		// TODO Auto-generated method stub
		Session session = PathStoreCluster.getInstance().connect();
		Select ss = QueryBuilder.select().from("pathstore_applications","nodes");
		ss.where(QueryBuilder.eq("nodeid", PathStoreProperties.getInstance().NodeID));
		System.out.println("aaasddds " + ss );	

		Row rr = session.execute(ss).one();
		int parentId = -1;
		try{
			parentId = rr.getInt("parentid");
			System.out.println("setting parent id here: " + parentId);
		}
		catch(Exception e)
		{
			System.out.println("couldn't set paretnId");
		}
		PathStoreProperties.getInstance().ParentID = parentId;
		System.out.println("parentId is: " + parentId);
	}


	private void setSessionCurrentEdge(String sid) {
		// TODO Auto-generated method stub
		PathStoreSession session = PathStoreCluster.getInstance().connect();

		Select slct = QueryBuilder.select().from("pathstore_applications", "session");
		slct.where(QueryBuilder.eq("sid", sid));
		ResultSet rr = session.executeLocal(slct,sid);
		Row r = rr.one();

		if(r==null)
		{
			Insert ins = QueryBuilder.insertInto("pathstore_applications","session").value("sid", sid)
					.value("current_edge", PathStoreProperties.getInstance().NodeID+"");
			session.executeLocal(ins,sid);
		}
	}

	public ArrayList<CommandEntryReply> sendData(String sid, String dstEdge, boolean neighbors,  
			HashMap<String, HashMap<Object, UUID>> rowsOnDest) {


		long totalTime = System.nanoTime();

		if(onlyDifferences)
		{
			long currentTotalBytes = LoggingHelperClass.getTotalBytes("neighbor");
			long prevTotalBytes = LoggingHelperClass.readFromMainDB("153");
			System.out.println("^^^^^^^ total bytes for delta list transfer: " + (currentTotalBytes-prevTotalBytes));
		}


		long totalDifferenceListTime = 0;
		double defaultTime=0;
		//System.out.println("requirement requested for sid:" + sid + " from: " + dstEdge);

		ArrayList<CommandEntryReply> replies = new ArrayList<>();

		List<ResultSetFuture> tasks = new ArrayList<>();


		// TODO Auto-generated method stub
		HashMap<String, HashMap<String, HashMap<String, List<QueryCacheEntry>>>> commands = QueryCache.getInstance().getDeviceCommands();
		HashMap<String, HashMap<String, List<QueryCacheEntry>>> keypsaces = commands.get(sid);
		System.out.println("request to move data for: " + sid );



		ConnectionEntry ce = connectionCache.get(dstEdge);
		if(ce==null)
			ce= createNewNeighborConnection(dstEdge);

		Session dstSession=pathStoreConnectionCache.get(dstEdge);
		if(dstSession==null)
		{	
			Cluster dst = Cluster.builder().addContactPoints(ce.PathStoreIP).withPort(ce.PathStorePort).build();
			dstSession= dst.connect();
			pathStoreConnectionCache.put(dstEdge, dstSession);
		}

		//Session dstSession = ce.session;


		//read from local
		PathStoreSession localSession = PathStoreCluster.getInstance().connect();
		Session localSession2 = PathStorePriviledgedCluster.getInstance().connect();


		//System.out.println("connected to: " + dstIp + ":"+dstPort);
		//		BatchStatement finalBatch = new BatchStatement();
		//		AtomicInteger batchSize = new AtomicInteger(0);
		//Batch finalBatch = QueryBuilder.batch();

		if(onlyDifferences)
		{
			long currentTotalBytes = LoggingHelperClass.getTotalBytes("neighbor");
			LoggingHelperClass.writeToMainDB("007", currentTotalBytes);
			double wasted = Timer.getTime(totalTime);
			LoggingHelperClass.writeToMainDB("wasted", (long)wasted);
			System.out.println("TOTAL TIME WASTED BEFORE ACTUALLY STARTING ********* DEDUCT IF ONLY DIFFERENCES: " + wasted);
		}
		System.out.println("timer1 : " + Timer.getTime(totalTime));
		int totalSkippedRows=0;
		int totalInserted=0;

		if(sid.equals("moveall"))
		{
			System.out.println("special case moveall!!!!");
			ArrayList<String> tableNames = new ArrayList<>();
			tableNames.add("comments");
			tableNames.add("old_comments");
			tableNames.add("categories");
			tableNames.add("stories");
			tableNames.add("old_stories");
			tableNames.add("users");
			tableNames.add("submissions");
			tableNames.add("moderator_log");
			HashMap<Table,List<Column>> tables = SchemaInfo.getInstance().getSchemaInfo().get(keyspace);
			int allRowsNum=0;
			for (String table: tableNames) {
				if(table.startsWith("view_"))
					continue;
				Table t = null;
				for(Table tmp: tables.keySet())
				{

					if(tmp.getTable_name().equals(table))
					{
						t=tmp;
						break;
					}
				}


				//List<Column> columns = tables.get(table);
				List<Column> columns = tables.get(t);
				System.out.println("Select * from pathstore_rubbos."+table+";");
				ResultSet results = localSession2.execute("Select * from pathstore_rubbos."+table+";");
				for (Row row : results) {
					allRowsNum++;
					long dd = System.nanoTime();
					Insert insert = QueryBuilder.insertInto(keyspace, table);
					for(Column c: columns)
					{
						if(c.column_name.equals("pathstore_parent_timestamp"))
						{
							insert.value(c.column_name, UUIDs.startOf(0));
							continue;
						}

						if(keyspace.equals("pathstore_applications") && table.equals("session") && c.column_name.equals("current_edge"))
						{
							System.out.println("assigning new edge!!!!  for: " + sid + " :"+ dstEdge);
							insert.value(c.column_name, dstEdge);
							continue;
						}

						if(!c.column_name.equals("pathstore_dirty"))
							insert.value(c.column_name, row.getObject(c.column_name));

					}
					dd = System.nanoTime();
					//oldSolution
					//finalBatch = checkAndAdd(insert,finalBatch,dstSession,batchSize);

					//newSolution:
					checkAndAddUpdated(insert, dstSession, tasks);
					//finalBatch.add(insert);
				}
			}
			System.out.println("numer of rows: " + allRowsNum);
		}

		else // not all tables!
		{
			for (String keyspace: keypsaces.keySet()) {
				HashMap<String, List<QueryCacheEntry>> tableNames = keypsaces.get(keyspace);
				HashMap<Table,List<Column>> tables = SchemaInfo.getInstance().getSchemaInfo().get(keyspace);

				for (String table: tableNames.keySet()) {
					List<QueryCacheEntry> cache_entries = tableNames.get(table);


					//TODO: FIX ME 
					Table t = null;
					for(Table tmp: tables.keySet())
					{

						if(tmp.getTable_name().equals(table))
						{
							t=tmp;
							break;
						}
					}


					//List<Column> columns = tables.get(table);
					List<Column> columns = tables.get(t);

					//Reading from QueryEntryCache
					if(!neighbors)
					{
						System.out.println("not neighbors");
						HashMap<Object, UUID> tableOnDestination=null;
						String primary=null;
						if(rowsOnDest!=null)
						{
							long tmp = System.nanoTime();
							tableOnDestination = rowsOnDest.get(table);
							primary = localSession2.getCluster().getMetadata().getKeyspace(keyspace).getTable(table).getPrimaryKey().get(0).getName();
							totalDifferenceListTime+=Timer.getTime(tmp);
							System.out.println("tableOnDestination: " + tableOnDestination.size());
						}

						try{
							for (QueryCacheEntry cache_entry : cache_entries)
							{
								//if (cache_entry.isReady())
								replies.add(new CommandEntryReply(keyspace, sid, table, cache_entry.getClausesSerialized(),cache_entry.limit));

								Select selectt = QueryBuilder.select().all().from(keyspace, table);
								selectt.allowFiltering();

								for (Clause clause : cache_entry.getClauses())
									selectt.where(clause);

								if(cache_entry.limit>0)
									selectt.limit(cache_entry.limit);

								//System.out.println(" select is: " + selectt.toString());
								long d = System.nanoTime();
								long checkAndAddTime=0;
								//PathStoreResultSet results =  (PathStoreResultSet) localSession.executeLocal(selectt,null);
								//hosseinhere:
								selectt.setFetchSize(1000);
								PathStoreResultSet results =  new PathStoreResultSet(localSession2.execute(selectt),keyspace,table);
								//System.out.println("querying cache entry took..: " +Timer.getTime(d));
								double insertTime=0;
								d=System.nanoTime();

								int ccnt = 0;
								int skippedRows=0;

								for (Row row : results) {

									if(onlyDifferences)
									{
										UUID row_timestamp = row.getUUID("pathstore_version");
										UUID timestamp_on_dest = tableOnDestination.get(row.getObject(primary));
										//System.out.println(row.getObject(primary) + "  " +  row_timestamp + " " + timestamp_on_dest);

										if (timestamp_on_dest != null && timestamp_on_dest.timestamp() >= row_timestamp.timestamp())
										{
											//System.out.println("skipping: " + row.getObject(primary));
											skippedRows++;
											continue;
										}
									}

									ccnt++;
									long dd = System.nanoTime();
									Insert insert = QueryBuilder.insertInto(keyspace, table);
									for(Column c: columns)
									{
										if(c.column_name.equals("pathstore_parent_timestamp"))
										{
											insert.value(c.column_name, UUIDs.startOf(0));
											continue;
										}

										if(keyspace.equals("pathstore_applications") && table.equals("session") && c.column_name.equals("current_edge"))
										{
											System.out.println("assigning new edge!!!!  for: " + sid + " :"+ dstEdge);
											insert.value(c.column_name, dstEdge);
											continue;
										}

										if(!c.column_name.equals("pathstore_dirty"))
											insert.value(c.column_name, row.getObject(c.column_name));

									}
									insertTime+=Timer.getTime(dd);
									dd = System.nanoTime();
									//oldSolution
									//finalBatch = checkAndAdd(insert,finalBatch,dstSession,batchSize);

									//newSolution:
									checkAndAddUpdated(insert, dstSession, tasks);
									checkAndAddTime += Timer.getTime(dd);

									//finalBatch.add(insert);
								}
								totalSkippedRows+=skippedRows;
								totalInserted+=ccnt;
								defaultTime +=Timer.getTime(d);
								//System.out.println("skippedRows: "+skippedRows );
								//System.out.println("creating +   " + ccnt +" insert entries and execution took: " +Timer.getTime(d) + " total time in checkAndAdd: " + checkAndAddTime + "   total time in insert: " + insertTime);
								//System.out.println("timer2 : " + Timer.getTime(totalTime));
							}

						}
						catch(Exception e){
							System.out.println("problem while looping over cache_entries");
							e.printStackTrace();
						}
					}
					else //(neighbors so only send dirty data)
					{
						System.out.println("neighbors");

						for (QueryCacheEntry cache_entry : cache_entries)
						{
							//if (cache_entry.isReady())
							try {
								replies.add(new CommandEntryReply(keyspace, sid, table, cache_entry.getClausesSerialized(), cache_entry.limit));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}


						//Dirty ones
						Select selectt = QueryBuilder.select().all().from(keyspace, table);
						selectt.where(QueryBuilder.eq("pathstore_dirty", true));
						ResultSet results =  localSession.executeLocal(selectt,null);
						for (Row row : results) {
							Insert insert = QueryBuilder.insertInto(keyspace, table);
							for(Column c: columns)
							{
								insert.value(c.column_name, row.getObject(c.column_name));
							}
							//finalBatch.add(insert);
							//newSolution:
							checkAndAddUpdated(insert, dstSession, tasks);


							//Oldsolution
							//finalBatch = checkAndAdd(insert,finalBatch,dstSession,batchSize);

						}
					}

					//System.out.println("timer3: " + Timer.getTime(ddd));
					System.out.println("total inserted = " + totalInserted + " total skipped: " + totalSkippedRows);

					//The pathstore_insert_sid columns
					Select select = QueryBuilder.select().all().from(keyspace,table);
					select.where(QueryBuilder.eq("pathstore_insert_sid", sid));
					PathStoreResultSet results  = (PathStoreResultSet)localSession.executeLocal(select,null);
					for (Row row : results) {
						Insert insert = QueryBuilder.insertInto(keyspace, table);
						for(Column c: columns)
						{
							if(c.column_name.equals("pathstore_parent_timestamp"))
							{
								insert.value(c.column_name, UUIDs.startOf(0));
								continue;
							}

							if(!c.column_name.equals("pathstore_dirty"))
								insert.value(c.column_name, row.getObject(c.column_name));


						}

						//oldsolution:
						//finalBatch = checkAndAdd(insert,finalBatch,dstSession,batchSize);

						//newSolution:
						checkAndAddUpdated(insert, dstSession, tasks);
						//finalBatch.add(insert);
					}

				}

			}
		}
		//		Select select = QueryBuilder.select().all().from("pathstore_applications","session");
		//		select.where(QueryBuilder.eq("sid", Integer.parseInt(sid)));
		//		Row results  = localSession.execute(select).one();
		//		
		//		Insert insert = QueryBuilder.insertInto("pathstore_applications", "session");
		//		insert.value("sid",results.getInt("sid")).value("username", results.getString("username"));


		//Oldsolution:
		//		if(finalBatch.size()>0)
		//		{
		//			//System.out.println("executing final batch" + finalBatch.size());
		//			dstSession.execute(finalBatch);
		//		}
		double time4 = Timer.getTime(totalTime);
		System.out.println("timer4 : " + time4);

		//new solution:
		if (tasks.size() > 10) {
			for (ResultSetFuture t:tasks)
				try {
					t.getUninterruptibly(50, TimeUnit.MILLISECONDS);
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

		defaultTime+=(Timer.getTime(totalTime) - time4);
		System.out.println(sid + " ***********total time to move: " + Timer.getTime(totalTime));
		System.out.println(sid + " ***********total default time " + defaultTime);
		System.out.println(sid + " ***********difference (processing time): " + (Timer.getTime(totalTime)-defaultTime));
		sessionLocks.remove(sid);
		//BandwidthCalculator.getTotalBytes("/home/mortazavi/mobisysTest-native/stats-neighbor.sh");


		return replies;
	}

	private ConnectionEntry createNewNeighborConnection(String edge) {
		// TODO Auto-generated method stub
		System.out.println("creating new neighbor connection ... " + edge);
		Select s = QueryBuilder.select().from("pathstore_applications","nodes");
		Session session = PathStoreCluster.getInstance().connect();
		s.where(QueryBuilder.eq("nodeid", Integer.parseInt(edge)));
		System.out.println("tmmmmmmp: " + s);
		Row r = session.execute(s).one();
		String dstIpRMI = r.getString("ps_ip");		
		int dstPortPS = r.getInt("ps_port");
		int destParentId = r.getInt("parentid");
		int dstPortRMI = 1099;
		PathStoreMigrate stub = null;
		try {
			Registry reg = LocateRegistry.getRegistry(dstIpRMI,dstPortRMI);
			stub = (PathStoreMigrate) reg.lookup("PathStoreMigrate");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(stub==null)
			System.err.println("new stub to neighbor is null");


		ConnectionEntry ce = new ConnectionEntry(destParentId, stub, null, dstIpRMI, dstPortRMI, dstIpRMI, dstPortPS);

		//connectionCache.put(previousEdge, ce);
		System.out.println("connection created to " + dstIpRMI + " " + dstPortRMI);
		//		return ce;
		//	});
		connectionCache.put(edge, ce);
		return ce;
	}

	public static BatchStatement checkAndAdd(Statement statement, BatchStatement batch, Session dstSession, AtomicInteger batchSize)
	{

		//		dstSession.execute(statement);
		//		return batch;
		int length = statement.toString().length();
		if (length> 81920)
			dstSession.execute(statement);
		else {
			if (batchSize.get() + length > 81920) {
				dstSession.execute(batch);
				batch.clear();
				//batch = new BatchStatement();
				batchSize.set(0);
			}
			batch.add(statement);
			batchSize.addAndGet(statement.toString().length());
		}
		return batch;
	}

	public static void checkAndAddUpdated(Statement statement, Session sessionWriter, List<ResultSetFuture> tasks)
	{
		ResultSetFuture future = sessionWriter.executeAsync(statement);
		tasks.add(future);
		if (tasks.size() < 40000)
			return;
		for (ResultSetFuture t:tasks)
			try {
				t.getUninterruptibly(100, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		tasks.clear();
	}

	public HashMap<String, HashMap<Object, UUID>>  createLatestListForKeyspace(String keyspace)
	{
		PathStorePriviledgedCluster cluster = PathStorePriviledgedCluster.getInstance();
		HashMap<Table,List<Column>> tables = SchemaInfo.getInstance().getSchemaInfo().get(keyspace);
		HashMap<String, HashMap<Object, UUID>> differenceList = new HashMap<>();

		for(Table table: tables.keySet())
		{
			String tableName = table.getTable_name();
			if(tableName.startsWith("view_"))
				continue;
			String primary= cluster.getMetadata().getKeyspace(keyspace).getTable(tableName).getPrimaryKey().get(0).getName();

			Select ss = QueryBuilder.select(primary, "pathstore_version").from(keyspace,tableName);
			ResultSet results = PathStorePriviledgedCluster.getInstance().connect().execute(ss);

			//List<Column> columns = SchemaInfo.getInstance().getTableColumns(keyspace,tableName);
			UUID highest_timestamp = null;

			//DataType type = cluster.getMetadata().getKeyspace("pathstore_demo").getTable("users").getPrimaryKey().get(0).getType();

			HashMap<Object, UUID> tableList = new HashMap<>();

			//check this
			Object previousKey=null;
			Object currentKey=null;
			for (Row row : results) {
				currentKey = row.getObject(primary);
				if(!currentKey.equals(previousKey))
				{
					tableList.put(currentKey, row.getUUID("pathstore_version"));
					//					highest_timestamp = null;
					previousKey=currentKey;
				}
				else
					continue;

				//UUID row_timestamp = row.getUUID("pathstore_version");

				//				if (highest_timestamp == null || highest_timestamp.timestamp() < row_timestamp.timestamp())
				//					highest_timestamp = row_timestamp;
			}
			//			if(currentKey!=null)
			//				finalList.add(new Pair<Object, UUID>(currentKey, highest_timestamp));
			differenceList.put(tableName, tableList);

			//			System.out.println(finalList);
		}
		return differenceList;

	}

	@Override
	//should only be called by another server
	public String fetchFromParent(ArrayList<CommandEntryReply> queriesToExecute, String previousEdge) throws RemoteException {
		// TODO Auto-generated method stub

		if(queriesToExecute ==null)
			return null;

		if(queriesToExecute.size() ==0)
			return null;

		System.out.println("fetching from the parent: " + queriesToExecute.size());

		PathStoreProperties p = PathStoreProperties.getInstance();
		Registry registry=null;
		
		PropertiesConfiguration config = new PropertiesConfiguration();
		try {
			config.load(new File("/home/mortazavi/nfs/traces/rubbos_exp.properties"));
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int extraBytes = Integer.valueOf((String)config.getProperty("extraBytes"));
		if (extraBytes==0)
				extraBytes=1;
		byte[] bytes = new byte[extraBytes];
		Arrays.fill( bytes, (byte) 1 );
		ByteBuffer buf = ByteBuffer.wrap(bytes);


		if(p.role == Role.CLIENT)
		{
			//rSystem.out.println("PathMigrate: role is client");
			if(serverstub == null)
			{
				try {
					registry = LocateRegistry.getRegistry(PathStoreProperties.getInstance().RMIRegistryIP,PathStoreProperties.getInstance().RMIRegistryPort);
					serverstub = (PathStoreMigrate) registry.lookup("PathStoreMigrate");
					System.out.println("fetching is done - client");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}

			try {
				return serverstub.fetchFromParent(queriesToExecute, previousEdge);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		//server
		else
		{


			long d = System.nanoTime();
			List<ResultSetFuture> tasks = new ArrayList<>();


			//read from local
			PathStoreSession localSession = PathStoreCluster.getInstance().connect();
			Session localSession2 = PathStorePriviledgedCluster.getInstance().connect();


			//System.out.println("connected to: " + dstIp + ":"+dstPort);
			//		BatchStatement finalBatch = new BatchStatement();
			//		AtomicInteger batchSize = new AtomicInteger(0);
			//Batch finalBatch = QueryBuilder.batch();
			System.out.println(" in the middle 1: " + Timer.getTime(d));


			HashMap<Table,List<Column>> tables = SchemaInfo.getInstance().getSchemaInfo().get(queriesToExecute.get(0).getKeyspace());

			ConnectionEntry ce = connectionCache.get(previousEdge);
			if(ce==null)
				ce =createNewNeighborConnection(previousEdge);

			Session dstSession=pathStoreConnectionCache.get(previousEdge);
			if(dstSession==null)
			{	
				Cluster dst = Cluster.builder().addContactPoints(ce.PathStoreIP).withPort(ce.PathStorePort).build();
				dstSession= dst.connect();
				pathStoreConnectionCache.put(previousEdge, dstSession);
			}


			System.out.println(" in the middle 2: " + Timer.getTime(d));

			for(CommandEntryReply rr : queriesToExecute)
			{

				//get Table and its columns 
				Table t = null;
				for(Table tmp: tables.keySet())
				{

					if(tmp.getTable_name().equals(rr.getTable()))
					{
						t=tmp;
						break;
					}
				}
				List<Column> columns = tables.get(t);

				rr.convertClauses();
				List<Clause> clauses = rr.getConverted();
				Select selectt = QueryBuilder.select().from(rr.getKeyspace(),rr.getTable());
				for(Clause clause: clauses)
					selectt.where(clause);
				if(rr.getLimit()>0)
					selectt.limit(rr.getLimit());
				else
					System.out.println("limit was: " + rr.getLimit());
				selectt.allowFiltering();
				System.out.println("executing " + selectt);


				ResultSet bb=localSession2.execute(selectt);
				System.out.println("executing took: " + Timer.getTime(d));

				PathStoreResultSet results =  new PathStoreResultSet(bb,rr.getKeyspace(), rr.getTable());
				System.out.println("creating pathstore resultset took: " + Timer.getTime(d));

				int co=0;
				for (Row row : results) {

					long dd = System.nanoTime();
					Insert insert = QueryBuilder.insertInto(rr.getKeyspace(), rr.getTable());
					for(Column c: columns)
					{
						if(c.column_name.equals("pathstore_parent_timestamp"))
						{
							//insert.value(c.column_name, UUIDs.startOf(0));
							insert.value(c.column_name, QueryBuilder.now());
							continue;
						}
						
						if(c.column_name.equals("data"))
						{
							insert.value(c.column_name,buf);
							continue;
						}

						if(rr.getKeyspace().equals("pathstore_applications") && rr.getTable().equals("session") )//&& c.column_name.equals("current_edge"))
						{
							//needs to be checked
							continue;
						}

						if(!c.column_name.equals("pathstore_dirty"))
							insert.value(c.column_name, row.getObject(c.column_name));
						
						

					}
					//System.out.println("inserting for row: " + co++ + " before check and update took : " + Timer.getTime(dd));
					checkAndAddUpdated(insert, dstSession, tasks);
					//System.out.println("inserting for row: " + co++ + " took : " + Timer.getTime(dd));
					//finalBatch.add(insert);
				}
			}

			System.out.println(" the amount time it took befor the final part: " + Timer.getTime(d));

			if (tasks.size() > 50) {
				for (ResultSetFuture t:tasks)
					try {
						t.getUninterruptibly(30, TimeUnit.MILLISECONDS);
					} catch (TimeoutException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			System.out.println(" the amount time it took: " + Timer.getTime(d));
		}

		return null;

	}


}



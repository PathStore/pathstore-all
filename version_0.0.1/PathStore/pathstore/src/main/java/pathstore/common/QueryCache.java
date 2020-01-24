package pathstore.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import pathstore.client.PathStoreServerClient;
import pathstore.exception.PathMigrateAlreadyGoneException;
import pathstore.exception.PathStoreRemoteException;
import pathstore.system.PathStoreParentCluster;
import pathstore.system.PathStorePriviledgedCluster;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;
import pathstore.util.SchemaInfo.Table;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.kenai.jffi.Array;

public class QueryCache {

	HashMap<String,HashMap<String,List<QueryCacheEntry>>> entries = new HashMap<String, HashMap<String, List<QueryCacheEntry>>>();

	//device, keyspace, table, clauses
	HashMap<String,HashMap<String,HashMap<String,List<QueryCacheEntry>>>> deviceCommands = new HashMap<String,HashMap<String,HashMap<String,List<QueryCacheEntry>>>>();


	public HashMap<String, HashMap<String, List<QueryCacheEntry>>> getEntries() {
		return entries;
	}

	public HashMap<String,HashMap<String,HashMap<String,List<QueryCacheEntry>>>> getDeviceCommands() {
		return deviceCommands;
	}

	static private QueryCache instance=null;

	static public QueryCache getInstance() {
		if (QueryCache.instance == null) 
			QueryCache.instance = new QueryCache();
		return QueryCache.instance;
	}

	public QueryCache() {
	}

	private void addKeyspace(String keyspace) {
		if (entries.containsKey(keyspace)==false) { 
			synchronized(entries) {
				if (entries.containsKey(keyspace)==false)  
					entries.put(keyspace, new HashMap<String, List<QueryCacheEntry>>());
			}
		}
	}


	private void addTable(String keyspace, String table) {
		addKeyspace(keyspace);
		if(entries.get(keyspace).containsKey(table) == false) {
			synchronized(entries.get(keyspace)) {
				if(entries.get(keyspace).containsKey(table) == false)
					entries.get(keyspace).put(table, new ArrayList<QueryCacheEntry>());
			}
		}
	}

	private void addDeviceId(String device)
	{
		//Hossein
		if (deviceCommands.containsKey(device)==false) { 
			synchronized(deviceCommands) {
				if (deviceCommands.containsKey(device)==false)
					deviceCommands.put(device, new HashMap<String,HashMap<String,List<QueryCacheEntry>>>());
			}
		}
	}


	private void addKeySpaceForDevice(String device, String keyspace)
	{
		addDeviceId(device);
		//Hossein
		if (deviceCommands.get(device).containsKey(keyspace)==false) { 
			synchronized(deviceCommands) {
				if (deviceCommands.get(device).containsKey(keyspace)==false)
					deviceCommands.get(device).put(keyspace, new HashMap<String, List<QueryCacheEntry>>());
			}
		}
	}

	private void addTableForDeviceKeyspace(String device, String keyspace, String table)
	{
		addKeySpaceForDevice(device, keyspace);
		//Hossein
		if (deviceCommands.get(device).get(keyspace).containsKey(table)==false) { 
			synchronized(deviceCommands) {
				if (deviceCommands.get(device).get(keyspace).containsKey(table)==false) 
					deviceCommands.get(device).get(keyspace).put(table,new ArrayList<QueryCacheEntry>());
			}
		}
	}

	//called by children
	public QueryCacheEntry updateDeviceCommandCache(String device, String keyspace, String table, byte[] clausesSerialized, int limit) throws IOException, ClassNotFoundException, PathMigrateAlreadyGoneException {
		ByteArrayInputStream bytesIn = new ByteArrayInputStream(clausesSerialized);
		ObjectInputStream ois = new ObjectInputStream(bytesIn);
		List<Clause> clauses = (List<Clause>)ois.readObject();

		QueryCacheEntry entry = getEntryFromDeviceCache(device, keyspace, table, clauses, limit);

		if (entry == null)
			entry =  addEntryToDeviceCommands(device, keyspace, table, clauses, clausesSerialized, limit);

		entry.waitUntilReady();

		return entry;

	}

	public QueryCacheEntry updateDeviceCommandCache(String device, String keyspace, String table, List<Clause> clauses, int limit) throws PathMigrateAlreadyGoneException, PathStoreRemoteException {

		QueryCacheEntry entry =null;
		if((PathStoreProperties.getInstance().role != Role.CLIENT ))
			entry= getEntryFromDeviceCache(device, keyspace, table, clauses,limit);

		if (entry == null)
			entry =  addEntryToDeviceCommands(device, keyspace, table, clauses, null, limit);

		//entry.waitUntilReady();

		return entry;

	}


	//called by child
	public QueryCacheEntry updateCache(String keyspace, String table, byte[] clausesSerialized, int limit) throws ClassNotFoundException, IOException {

		ByteArrayInputStream bytesIn = new ByteArrayInputStream(clausesSerialized);
		ObjectInputStream ois = new ObjectInputStream(bytesIn);
		List<Clause> clauses = (List<Clause>)ois.readObject();

		QueryCacheEntry entry = getEntry(keyspace, table, clauses, limit);

		if (entry == null)
			entry =  addEntry(keyspace, table, clauses, clausesSerialized, false, limit);

		entry.waitUntilReady();

		return entry;
	}


	//executed on client
	public QueryCacheEntry updateCache(String keyspace, String table, List<Clause> clauses, int limit) {
		QueryCacheEntry entry = getEntry(keyspace, table, clauses, limit);

		if (entry == null)
			entry =  addEntry(keyspace, table, clauses, null, false, limit);

		entry.waitUntilReady();

		return entry;

	}

	//Only called for migration
	public QueryCacheEntry updateCacheByMigration(String keyspace, String table, List<Clause> clauses, int limit) {
		QueryCacheEntry entry = getEntry(keyspace, table, clauses, limit);

		if (entry == null)
			entry =  addEntry(keyspace, table, clauses, null, true, limit);

		//entry.waitUntilReady();
		return entry;
	}
//
//	//Only called for migration
//	public QueryCacheEntry updateCacheByMigrationAll(ArrayList<CommandEntryReply> replies) {
//		ArrayList<QueryCacheEntry> results = new ArrayList<>();
//		for (CommandEntryReply r : replies)
//		{
//
//			QueryCacheEntry entry = getEntry(r.getKeyspace(), r.getTable(), r.getConverted(), r.getLimit());
//
//			if (entry == null)
//				results.add(entry);
//		}
//
//		addEntries(results);
//
//		//entry.waitUntilReady();
//	}



	//Hossein here making this public
	public QueryCacheEntry getEntry(String keyspace, String table, List<Clause> clauses, int limit) {
		HashMap<String, List<QueryCacheEntry>> tableMap = entries.get(keyspace);
		if (tableMap == null)
			return null;

		List<QueryCacheEntry> entryList = tableMap.get(table);
		if (entryList == null)
			return null;

		for  (QueryCacheEntry e :  entryList) 
			if (e.isSame(clauses)) 
				if(e.limit==-1 || e.limit>limit) //we already have a bigger query so don't add this one
					return e;

		return null;
	}


	private QueryCacheEntry getEntryFromDeviceCache(String deviceId, String keyspace, String table, List<Clause> clauses, int limit) {

		HashMap<String, HashMap<String, List<QueryCacheEntry>>> keyspaceMap = deviceCommands.get(deviceId);
		if (keyspaceMap == null)
			return null;

		HashMap<String, List<QueryCacheEntry>> tableMap = keyspaceMap.get(keyspace);

		if (tableMap == null)
			return null;

		List<QueryCacheEntry> entryList = tableMap.get(table);
		if (entryList == null)
			return null;

		for  (QueryCacheEntry e :  entryList) 
			if (e.isSame(clauses)) 
				if(e.limit==-1 || e.limit>limit) //we already have a bigger query so don't add this one
					return e;

		return null;
	}



	private QueryCacheEntry addEntryToDeviceCommands(String device, String keyspace, String table, List<Clause> clauses, byte[] clausesSerialized, int limit) throws PathMigrateAlreadyGoneException, PathStoreRemoteException{

		boolean deviceHere=true;
		if(!deviceHere)
			throw new PathMigrateAlreadyGoneException();

		addTableForDeviceKeyspace(device, keyspace, table);

		HashMap<String, List<QueryCacheEntry>> tableMap = deviceCommands.get(device).get(keyspace);
		List<QueryCacheEntry> entryList = tableMap.get(table);

		QueryCacheEntry newEntry = new QueryCacheEntry(keyspace,table,clauses, limit);
		if (clausesSerialized != null)
			newEntry.setClausesSerialized(clausesSerialized);


		synchronized(entryList) {
			for  (QueryCacheEntry entry :  entryList) { 
				if (entry.isSame(clauses))
				{
					if(entry.limit==newEntry.limit)
						return entry;
					else if(entry.limit==-1 && newEntry.limit>0)
					{
						newEntry.isCovered = entry;
						entry.covers.add(newEntry);
					}
					else if(entry.limit>0 && newEntry.limit>0 && entry.limit<newEntry.limit)
					{
						entry.isCovered = newEntry;
						newEntry.covers.add(entry);
					}
				}

				if (newEntry.isCovered == null && entry.isSuperSet(clauses)) {
					newEntry.isCovered = entry;
					entry.covers.add(newEntry);
				}

				if (entry.isCovered == null &&  entry.isSubSet(clauses)) {
					entry.isCovered = newEntry;
					newEntry.covers.add(entry);
				}
			}
			if(PathStoreProperties.getInstance().role != Role.CLIENT)
				entryList.add(newEntry);
		}

		try{
			if (PathStoreProperties.getInstance().role == Role.CLIENT ) 
				PathStoreServerClient.getInstance().addCommandEntry(device,newEntry);
			//}
			//catch( Exception E) {
			//	throw E;
		}
		finally{
			newEntry.setReady();
		}

		return newEntry;

	}
	

	private QueryCacheEntry addEntry(String keyspace, String table, List<Clause> clauses, byte[] clausesSerialized, boolean fromMigration, int limit) {

		addTable(keyspace,table);

		HashMap<String, List<QueryCacheEntry>> tableMap = entries.get(keyspace);
		List<QueryCacheEntry> entryList = tableMap.get(table);

		QueryCacheEntry newEntry = new QueryCacheEntry(keyspace,table,clauses, limit);
		if (clausesSerialized != null)
			newEntry.setClausesSerialized(clausesSerialized);

		synchronized(entryList) {
			for  (QueryCacheEntry entry :  entryList) { 
				if (entry.isSame(clauses))
				{
					if(entry.limit==newEntry.limit)
						return entry;
					else if(entry.limit==-1 && newEntry.limit>0)
					{
						newEntry.isCovered = entry;
						entry.covers.add(newEntry);
					}
					else if(entry.limit>0 && newEntry.limit>0 && entry.limit<newEntry.limit)
					{
						entry.isCovered = newEntry;
						newEntry.covers.add(entry);
					}
				}

				if (newEntry.isCovered == null && entry.isSuperSet(clauses)) {
					newEntry.isCovered = entry;
					entry.covers.add(newEntry);
				}

				if (entry.isCovered == null &&  entry.isSubSet(clauses)) {
					entry.isCovered = newEntry;
					newEntry.covers.add(entry);
				}
			}

			entryList.add(newEntry);
		}


		// TODO add entry to DB (of your parent)
		try {
			
			if (PathStoreProperties.getInstance().role != Role.ROOTSERVER && newEntry.isCovered == null) {
				
				if(!fromMigration)
				PathStoreServerClient.getInstance().addQueryEntry(newEntry);
				//Hossein: don't update your parents query cache (for now)


				if (PathStoreProperties.getInstance().role == Role.SERVER && !fromMigration)
				{
					//fetchData(newEntry);
					fetchDelta(newEntry);
				}
			}
		}
		finally {
			newEntry.setReady();
		}


		return newEntry;

	}


	public UUID createDelta(String keyspace, String table, byte[] clausesSerialized, UUID parentTimestamp, int nodeID, int limit) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bytesIn = new ByteArrayInputStream(clausesSerialized);
		ObjectInputStream ois = new ObjectInputStream(bytesIn);
		@SuppressWarnings("unchecked")
		List<Clause> clauses = (List<Clause>)ois.readObject();

		UUID deltaID = UUID.randomUUID();

		if(limit==-1)
			limit=Integer.MAX_VALUE;

		Select select = QueryBuilder.select().all().from(keyspace, table);
		select.allowFiltering();

		for (Clause clause : clauses)
			select.where(clause);


		Session local = PathStorePriviledgedCluster.getInstance().connect();

		try {

			//hossein here:
			select.setFetchSize(1000);

			ResultSet results = local.execute(select);

			List<Column> columns = SchemaInfo.getInstance().getTableColumns(keyspace,table);

			Batch batch = QueryBuilder.batch();

			int batchSize = 0;

			PathStorePriviledgedCluster cluster = PathStorePriviledgedCluster.getInstance();

			String primary= cluster.getMetadata().getKeyspace(keyspace).getTable(table).getPrimaryKey().get(0).getName();

			Object previousKey=null;
			Object currentKey=null;
			int count=-1;

			int totalRowsChanged=0;
			for (Row row : results) {
				currentKey = row.getObject(primary);
				if(!currentKey.equals(previousKey))
					count++;
				if(count>=limit)
					break;

				//probable bug ... changed && to || and >= to <=
				if (row.getInt("pathstore_node") == nodeID ||
						row.getUUID("pathstore_parent_timestamp").timestamp() <= parentTimestamp.timestamp())
					continue;


				totalRowsChanged++;
				Insert insert = QueryBuilder.insertInto(keyspace, "view_" + table);

				insert.value("pathstore_view_id", deltaID);

				//Hossein
				for (Column column : columns) {
					if (row.isNull(column.column_name) == false
							&& column.column_name.compareTo("pathstore_dirty") != 0
							&& column.column_name.compareTo("pathstore_insert_sid") != 0)
						insert.value(column.column_name, row.getObject(column.column_name));
				}

				String statement = insert.toString();

				if (statement.length() > PathStoreProperties.getInstance().MaxBatchSize)
					local.execute(insert);
				
				else {
					if (batchSize + statement.length() > PathStoreProperties.getInstance().MaxBatchSize) {
						local.execute(batch);
						batch = QueryBuilder.batch();
						batchSize = 0;
					}
					batch.add(insert);
					batchSize += statement.length();
				}
			}
			if (batchSize > 0)
				local.execute(batch);

			if(totalRowsChanged==0)
				return null;
			
			
			return deltaID;
		}
		finally {
			//local.close();
		}
		
	}


	public void fetchDelta(QueryCacheEntry entry) {
		UUID deltaId = null;
		long d= System.nanoTime();
		
//		System.out.println("     inside fetch delta " + entry.keyspace + " " + entry.table);
		
		if (entry.getParentTimeStamp() != null)
		{
			
			deltaId = PathStoreServerClient.getInstance().cretateQueryDelta(entry);
			if(deltaId==null)
			{
//				System.out.println("no change, return");
				return;
			}
//			System.out.println(" creating queryDelta took: " +Timer.getTime(d));
		}

		fetchData(entry,deltaId);
//		System.out.println(" after querydelta took: " +Timer.getTime(d));
	}


	private void fetchData(QueryCacheEntry entry) {
		fetchData(entry,null);
	}	



	private void fetchData(QueryCacheEntry entry, UUID deltaID) {
		Session parent = PathStoreParentCluster.getInstance().connect();
		Session local = PathStorePriviledgedCluster.getInstance().connect();

		try {
			String table = deltaID != null? "view_" + entry.table : entry.table;

			Select select = QueryBuilder.select().all().from(entry.keyspace, table);

			select.allowFiltering();

			if (deltaID != null)
				select.where(QueryBuilder.eq("pathstore_view_id", deltaID));

			for (Clause clause : entry.clauses)
				select.where(clause);

			//hossein here again
			select.setFetchSize(1000);
			
			ResultSet results = parent.execute(select);

			List<Column> columns = SchemaInfo.getInstance().getTableColumns(entry.keyspace,entry.table);

			Batch batch = QueryBuilder.batch();

			int batchSize = 0;

			UUID highest_timestamp = null;


			//check this
			for (Row row : results) {

				Insert insert = QueryBuilder.insertInto(entry.keyspace, entry.table);

				for (Column column : columns) {
					if (column.column_name.compareTo("pathstore_parent_timestamp")==0) {
						UUID row_timestamp = row.getUUID("pathstore_parent_timestamp");

						if (highest_timestamp == null || highest_timestamp.timestamp() < row_timestamp.timestamp())
							highest_timestamp = row_timestamp;

						insert.value("pathstore_parent_timestamp", QueryBuilder.now());
					}
					else 	
					{
						try{
							if (column.column_name.compareTo("pathstore_insert_sid") != 0 && column.column_name.compareTo("pathstore_dirty") != 0
									&& row.isNull(column.column_name) == false )
								insert.value(column.column_name, row.getObject(column.column_name));
						}
						catch(Exception e)
						{
							e.printStackTrace();
							System.err.println(" some error here: entry.keyspace entry.table" +  entry.keyspace + " " + entry.table);
						}
					}
				}

				String statement = insert.toString();

				if (statement.length() > PathStoreProperties.getInstance().MaxBatchSize)
					local.execute(insert);
				else {
					if (batchSize + statement.length() > PathStoreProperties.getInstance().MaxBatchSize) {
						local.execute(batch);
						batch = QueryBuilder.batch();
						batchSize = 0;
					}
					batch.add(insert);
					batchSize += statement.length();
				}
			}
			if (batchSize > 0)
				local.execute(batch);


			UUID entry_timestamp = entry.getParentTimeStamp();

			assert(entry_timestamp == null || entry_timestamp.timestamp() < highest_timestamp.timestamp());

			entry.setParentTimeStamp(highest_timestamp);
		}
		finally {
			//local.close();
			//parent.close();
		}

	}


	//Hossein: this is used in migration
	public ArrayList<CommandEntryReply>  reconsolidateWithNeighbor(ArrayList<CommandEntryReply> replies) throws IOException {
		ArrayList<CommandEntryReply> result = new ArrayList<>(); 
		// TODO Auto-generated method stub
		for(CommandEntryReply rr: replies)
		{
			//rr.convertClauses();
			//System.out.println("reconsolidateWithNeighbor: " + rr.getKeyspace() + " " + rr.getTable() + "  " + rr.getConverted());
			ByteArrayInputStream bytesIn = new ByteArrayInputStream(rr.getClauses());
			ObjectInputStream ois;
			List<Clause> clauses = null;
			try {
				ois = new ObjectInputStream(bytesIn);
				clauses = (List<Clause>)ois.readObject();
			} catch (IOException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if(rr.getKeyspace().equals("pathstore_applications") && rr.getTable().equals("session"))
			{
				updateCache(rr.getKeyspace(), rr.getTable(), clauses, rr.limit);
				continue;
			}
			//QueryCacheEntry QCentry = QueryCache.getInstance().getEntry(rr.getKeyspace(), rr.getTable(), clauses);

			

			HashMap<String, List<QueryCacheEntry>> tables = getEntries().get(rr.getKeyspace());
			if(tables==null)
				continue;
			List<QueryCacheEntry> entries = tables.get(rr.getTable());
			if(entries==null)
				continue;
			//System.out.println("consolidating ... " + rr.getKeyspace() + "." + rr.getTable());

			QueryCacheEntry fromSource= new QueryCacheEntry(rr.getKeyspace(), rr.getTable(), clauses, rr.getLimit());

			for(QueryCacheEntry qce: entries)
			{
				long d =System.nanoTime();
				if(qce.isSame(clauses)) //if the same query exists, fetch delta from parent, no need to look further
				{
					if((fromSource.limit==-1 && qce.limit>-1) 
							|| (fromSource.limit>qce.limit))
					{
						//fetchDelta(fromSource);
						result.add(rr);
						//System.out.println("fetch delta " + qce.keyspace + " " + qce.table +  " " + qce.clauses+  "took: " + Timer.getTime(d));
						continue;
					}
					//fetchDelta(qce);
					byte[] cl = qce.getClausesSerialized();
					result.add(new CommandEntryReply(qce.keyspace, rr.sid, qce.table, cl, qce.limit));
					
					break;
					//System.out.println("fetch delta " + qce.keyspace + " " + qce.table +  " " + qce.clauses+  "took: " + Timer.getTime(d));
				}
				else if(qce.isSubSet(clauses)) // if a subset query exists, fetch delta on the query
				{
					//fetchDelta(qce);
					byte[] cl = qce.getClausesSerialized();
					result.add(new CommandEntryReply(qce.keyspace, rr.sid, qce.table, cl, qce.limit));
				}
				else if(qce.isSuperSet(clauses)) // if a 
				{
					//fetchDelta(fromSource);
					result.add(rr);
				}
				//System.out.println("fetch delta " + qce.keyspace + " " + qce.table +  " " + qce.clauses+  "took: " + Timer.getTime(d));

			}
			//System.out.println("result size that I need to fetch from parent: " + result.size());
			updateDeviceCommandCache(rr.getSid(), rr.getKeyspace(), rr.getTable(), clauses, rr.getLimit());
		}
		return result;

	}



}

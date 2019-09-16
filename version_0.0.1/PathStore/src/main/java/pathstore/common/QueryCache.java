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
package pathstore.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import pathstore.client.PathStoreServerClient;
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

public class QueryCache {

	HashMap<String,HashMap<String,List<QueryCacheEntry>>> entries = new HashMap<String, HashMap<String, List<QueryCacheEntry>>>();
	
	public HashMap<String, HashMap<String, List<QueryCacheEntry>>> getEntries() {
		return entries;
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
	

	public QueryCacheEntry updateCache(String keyspace, String table, byte[] clausesSerialized) throws ClassNotFoundException, IOException {
		
		ByteArrayInputStream bytesIn = new ByteArrayInputStream(clausesSerialized);
	    ObjectInputStream ois = new ObjectInputStream(bytesIn);
		List<Clause> clauses = (List<Clause>)ois.readObject();
	
		QueryCacheEntry entry = getEntry(keyspace, table, clauses);
		
		if (entry == null)
			entry =  addEntry(keyspace, table, clauses, clausesSerialized);
		
		entry.waitUntilReady();
		
		return entry;
	}

	
	
	public QueryCacheEntry updateCache(String keyspace, String table, List<Clause> clauses) {
		QueryCacheEntry entry = getEntry(keyspace, table, clauses);
		
		if (entry == null)
			entry =  addEntry(keyspace, table, clauses, null);
		
		entry.waitUntilReady();
		
		return entry;

	}
	
	private QueryCacheEntry getEntry(String keyspace, String table, List<Clause> clauses) {
		HashMap<String, List<QueryCacheEntry>> tableMap = entries.get(keyspace);
		if (tableMap == null)
			return null;
		
		List<QueryCacheEntry> entryList = tableMap.get(table);
		if (entryList == null)
			return null;
		
		for  (QueryCacheEntry e :  entryList) 
			if (e.isSame(clauses)) 
				return e;
				
		return null;
	}
	
	private QueryCacheEntry addEntry(String keyspace, String table, List<Clause> clauses, byte[] clausesSerialized) {

		addTable(keyspace,table);
		  
		HashMap<String, List<QueryCacheEntry>> tableMap = entries.get(keyspace);
		List<QueryCacheEntry> entryList = tableMap.get(table);

		QueryCacheEntry newEntry = new QueryCacheEntry(keyspace,table,clauses);
		if (clausesSerialized != null)
			newEntry.setClausesSerialized(clausesSerialized);

		synchronized(entryList) {
			for  (QueryCacheEntry entry :  entryList) { 
				if (entry.isSame(clauses))
					return entry;
				
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

		// TODO add entry to DB

		try {
			if (PathStoreProperties.getInstance().role != Role.ROOTSERVER && newEntry.isCovered == null) {
				PathStoreServerClient.getInstance().addQueryEntry(newEntry);
	
				if (PathStoreProperties.getInstance().role == Role.SERVER)
					fetchData(newEntry);
			}
		}
		finally {
			newEntry.setReady();
		}
		
		
		return newEntry;
		
	}
	

	public UUID createDelta(String keyspace, String table, byte[] clausesSerialized, UUID parentTimestamp, int nodeID) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bytesIn = new ByteArrayInputStream(clausesSerialized);
	    ObjectInputStream ois = new ObjectInputStream(bytesIn);
		@SuppressWarnings("unchecked")
		List<Clause> clauses = (List<Clause>)ois.readObject();

		UUID deltaID = UUID.randomUUID();
		
		
		Select select = QueryBuilder.select().all().from(keyspace, table);
		select.allowFiltering();
		
		for (Clause clause : clauses)
			select.where(clause);
		
		
		Session local = PathStorePriviledgedCluster.getInstance().connect();
		
		try {
		
			ResultSet results = local.execute(select);
	
			List<Column> columns = SchemaInfo.getInstance().getTableColumns(keyspace,table);
			
			Batch batch = QueryBuilder.batch();
			
			int batchSize = 0;
			
			for (Row row : results) {
				
				if (row.getInt("pathstore_node") == nodeID &&
				    row.getUUID("pathstore_parent_timestamp").timestamp() >= parentTimestamp.timestamp())
					continue;
				
					
				Insert insert = QueryBuilder.insertInto(keyspace, "view_" + table);
	
				insert.value("pathstore_view_id", deltaID);
				
				for (Column column : columns) {
						if (row.isNull(column.column_name) == false
							&& column.column_name.compareTo("pathstore_dirty") != 0)
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
			
			return deltaID;
		}
		finally {
			//local.close();
		}
	}

	
	public void fetchDelta(QueryCacheEntry entry) {
		UUID deltaId = null;
		
		if (entry.getParentTimeStamp() != null)
			deltaId = PathStoreServerClient.getInstance().cretateQueryDelta(entry);
		
		fetchData(entry,deltaId);
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
			
			ResultSet results = parent.execute(select);
	
			List<Column> columns = SchemaInfo.getInstance().getTableColumns(entry.keyspace,entry.table);
			
			Batch batch = QueryBuilder.batch();
			
			int batchSize = 0;
			
			UUID highest_timestamp = null;
			
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
						if (row.isNull(column.column_name) == false && column.column_name.compareTo("pathstore_dirty") != 0)
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
			
			
			UUID entry_timestamp = entry.getParentTimeStamp();
			
			assert(entry_timestamp == null || entry_timestamp.timestamp() < highest_timestamp.timestamp());
				
			entry.setParentTimeStamp(highest_timestamp);
		}
		finally {
			//local.close();
			//parent.close();
		}
		
	}


	
		
	
	
	
}

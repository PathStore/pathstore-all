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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.UUID;

import com.datastax.driver.core.querybuilder.Clause;

public class QueryCacheEntry  {
	String keyspace;
	String table;
	List<Clause> clauses;
	byte[] clausesSerialized = null;
	QueryCacheEntry isCovered = null;
	List<QueryCacheEntry> covers = null;
	private boolean ready = false;
	private UUID parentTimeStamp = null;
	
	public QueryCacheEntry(String keyspace, String table, List<Clause> clauses) {
		this.keyspace = keyspace;
		this.table = table;
		this.clauses = clauses;
	}

	public QueryCacheEntry getIsCovered()
	{
		return this.isCovered;
	}

	public boolean isSame(List<Clause> clauses2) {
		if (clauses.size() != clauses2.size())
			return false;
		
		for (int i=0; i < clauses.size(); i++)
			if (clauses.get(i).toString().compareTo(clauses2.get(i).toString())!=0)
				return false;
			
		return true;
	}

	synchronized public void setReady() {
		this.ready = true;
		this.notifyAll();
	}
	
	public void waitUntilReady() {
		if (this.ready)
			return;
		synchronized(this) {
			while(this.ready==false)
				try {
					this.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
	public boolean isReady() {
		return this.ready;
	}

	
	private boolean firstContainsSecond(List<Clause> clauses1, List<Clause> clauses2) {
		// Trivial case.  First query does not have where clauses, and second has at least one clause
		if (clauses1.size()==0 && clauses2.size() > 0)
			return true;
		
		
		
		
		return false;
		
	}
	
	public boolean isSuperSet(List<Clause> clauses2) {
		return firstContainsSecond(this.clauses,clauses2);
	}

	public boolean isSubSet(List<Clause> clauses2) {
		return firstContainsSecond(clauses2,this.clauses);
	}

	public UUID getParentTimeStamp() {
		return parentTimeStamp;
	}

	public void setParentTimeStamp(UUID parentTimeStamp) {
		this.parentTimeStamp = parentTimeStamp;
	}

	public String getKeyspace() {
		return keyspace;
	}

	public void setKeyspace(String keyspace) {
		this.keyspace = keyspace;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public List<Clause> getClauses() {
		return clauses;
	}

	public void setClauses(List<Clause> clauses) {
		this.clauses = clauses;
	}

	public byte[] getClausesSerialized() throws IOException {
		
		if (clausesSerialized == null) {
			ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		    ObjectOutputStream oos = new ObjectOutputStream(bytesOut);
		    oos.writeObject(this.clauses);
		    oos.flush();
		    byte[] bytes = bytesOut.toByteArray();
		    bytesOut.close();
		    oos.close();
		    this.clausesSerialized = bytes;
		}
		
		return this.clausesSerialized;
	}

	public void setClausesSerialized(byte[] clausesSerialized) {
		this.clausesSerialized = clausesSerialized;
	}

	
	
	
}

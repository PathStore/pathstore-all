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
package pathstore.client;

import java.util.List;
import java.util.Map;

import pathstore.common.QueryCache;
import pathstore.common.QueryCacheEntry;
import pathstore.exception.InvalidKeyspaceException;
import pathstore.exception.InvalidStatementTypeException;
import pathstore.util.PathStoreStatus;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;

import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Assignment;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.core.querybuilder.Update.Assignments;
import com.google.common.util.concurrent.ListenableFuture;


public class PathStoreSession implements Session {

	private Session session; 

	public PathStoreSession(Cluster cluster) {
		session = cluster.connect();
	}

	public String getLoggedKeyspace() {
		throw new UnsupportedOperationException();
	}

	public Session init() {
		// TODO Auto-generated method stub
		return null;
	}

	public ListenableFuture<Session> initAsync() {
		throw new UnsupportedOperationException();
	}

	public ResultSet execute(String query) {
		if(query.toLowerCase().contains("local_".toLowerCase()))
			return session.execute(query);
		else
			throw new UnsupportedOperationException();
	}

	public ResultSet execute(String query, Object... values) {
		throw new UnsupportedOperationException();
	}

	public ResultSet execute(String query, Map<String, Object> values) {
		throw new UnsupportedOperationException();
	}  

	public ResultSet execute(Statement statement){

		String keyspace = statement.getKeyspace();
		String table="";

		if (keyspace.startsWith("pathstore")==false)
			throw new InvalidKeyspaceException("Keyspace does not start with pathstore prefix");

		if (statement instanceof Select) {
			Select select = (Select)statement;
			table = select.getTable();

			if (table.startsWith("local_") == false) {
				List<Clause> clauses = select.where().getClauses();
				QueryCache.getInstance().updateCache(keyspace, table, clauses);
			}
		} 
		else if (statement instanceof Insert) {
			Insert insert = (Insert)statement;

			table = insert.getTable();

			if (table.startsWith("local_") == false) {
				insert.value("pathstore_version", QueryBuilder.now());
				insert.value("pathstore_parent_timestamp",  QueryBuilder.now());
				insert.value("pathstore_dirty", true);
			}
		} 
		else if (statement instanceof Delete) {
			Delete delete = (Delete)statement;

			table = delete.getTable();
			if (table.startsWith("local_") == false) {
				Insert insert = QueryBuilder.insertInto(delete.getKeyspace(), delete.getTable());

				insert.value("pathstore_version", QueryBuilder.now());
				insert.value("pathstore_parent_timestamp",  QueryBuilder.now());
				insert.value("pathstore_dirty", true);
				insert.value("pathstore_deleted", true);

				List<Clause> clauses = delete.where().getClauses();

				for (Clause clause : clauses) {
					String name = clause.getName();
					Object value = clause.getValue();
					insert.value(name, value);
				}

				statement = insert;
			}
		} 
		else if (statement instanceof Update) {
			Update update = (Update)statement;

			table = update.getTable();

			if (table.startsWith("local_") == false) {


				Insert insert = QueryBuilder.insertInto(update.getKeyspace(), update.getTable());

				insert.value("pathstore_version", QueryBuilder.now());
				insert.value("pathstore_parent_timestamp",  QueryBuilder.now());
				insert.value("pathstore_dirty", true);


				Assignments assignment = update.with();

				for(Assignment a : assignment.getAssignments()) {
					String name = a.name;
					Object value = ((Assignment.SetAssignment)a).value;

					insert.value(name,value);

				}


				List<Clause> clauses = Update.where().getClauses();

				for (Clause clause : clauses) {
					String name = clause.getName();
					Object value = clause.getValue();
					insert.value(name, value);
				}

				statement = insert;
			}
		}
		else throw new InvalidStatementTypeException();


		return new PathStoreResultSet(session.execute(statement), keyspace, table);
	}

	public ResultSetFuture executeAsync(String query) {
		if(query.toLowerCase().contains("local_".toLowerCase()))
			return session.executeAsync(query);
		else
			throw new UnsupportedOperationException();	}

	public ResultSetFuture executeAsync(String query, Object... values) {
		throw new UnsupportedOperationException();
	}

	public ResultSetFuture executeAsync(String query, Map<String, Object> values) {
		throw new UnsupportedOperationException();
	}

	public ResultSetFuture executeAsync(Statement statement) {
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepare(String query) {
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepare(RegularStatement statement) {
		throw new UnsupportedOperationException();
	}

	public ListenableFuture<PreparedStatement> prepareAsync(String query) {
		throw new UnsupportedOperationException();
	}

	public ListenableFuture<PreparedStatement> prepareAsync(
			RegularStatement statement) {
		throw new UnsupportedOperationException();
	}

	public CloseFuture closeAsync() {
		throw new UnsupportedOperationException();
	}

	public void close() {
		session.close();
	}

	public boolean isClosed() {
		return session.isClosed();
	}

	public Cluster getCluster() {
		throw new UnsupportedOperationException();
	}

	public State getState() {
		throw new UnsupportedOperationException();
	}

}

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

import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.ListenableFuture;

public class PathStoreResultSet implements ResultSet {
	
	private ResultSet resultSet;
	private String keyspace;
	private String table;
	
	public PathStoreResultSet(ResultSet resultSet, String keyspace, String table) {
		this.resultSet = resultSet;
		this.keyspace = keyspace;
		this.table = table;
	}
	
	public boolean isExhausted() {
		// TODO Auto-generated method stub
		return resultSet.isExhausted();
	}

	public boolean isFullyFetched() {
		// TODO Auto-generated method stub
		return resultSet.isFullyFetched();
	}

	public int getAvailableWithoutFetching() {
		// TODO Auto-generated method stub
		return resultSet.getAvailableWithoutFetching();
	}

	public ListenableFuture<ResultSet> fetchMoreResults() {
		// TODO Auto-generated method stub
		return resultSet.fetchMoreResults();
	}

	public List<Row> all() {
		// TODO Auto-generated method stub
		return resultSet.all();
	}

	public Iterator<Row> iterator() {
		// TODO Auto-generated method stub
		return new PathStoreIterator(resultSet.iterator(), keyspace, table);
	}

	public ExecutionInfo getExecutionInfo() {
		// TODO Auto-generated method stub
		return resultSet.getExecutionInfo();
	}

	public List<ExecutionInfo> getAllExecutionInfo() {
		// TODO Auto-generated method stub
		return resultSet.getAllExecutionInfo();
	}

	public Row one() {
		// TODO Auto-generated method stub
		return resultSet.one();
	}

	public ColumnDefinitions getColumnDefinitions() {
		// TODO Auto-generated method stub
		return resultSet.getColumnDefinitions();
	}

	public boolean wasApplied() {
		// TODO Auto-generated method stub
		return resultSet.wasApplied();
	}

}

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

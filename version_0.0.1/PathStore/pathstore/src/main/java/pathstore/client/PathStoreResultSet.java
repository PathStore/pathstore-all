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
package pathstore.client;

import com.datastax.driver.core.*;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Iterator;
import java.util.List;

/** TODO: Comment */
public class PathStoreResultSet implements ResultSet {

  private final Session session;
  private final ResultSet resultSet;
  private final String keyspace;
  private final String table;
  private final boolean allowFiltering;

  public PathStoreResultSet(
      final Session session,
      final ResultSet resultSet,
      final String keyspace,
      final String table,
      final boolean allowFiltering) {
    this.session = session;
    this.resultSet = resultSet;
    this.keyspace = keyspace;
    this.table = table;
    this.allowFiltering = allowFiltering;
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
    return new PathStoreIterator(
        this.session, this.resultSet.iterator(), this.keyspace, this.table, this.allowFiltering);
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

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
import com.datastax.driver.core.querybuilder.Clause;
import com.google.common.util.concurrent.ListenableFuture;
import pathstore.common.Constants;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This result set is used to extend the regular result set to allow for log compression using
 * {@link PathStoreIterator}
 */
public class PathStoreResultSet implements ResultSet {

  /** Session to execute additional queries in the iterator class */
  private final Session session;

  /** Response from database */
  private final ResultSet resultSet;

  /** Keyspace original query was on */
  private final String keyspace;

  /** Table original query was on */
  private final String table;

  /** If the query could break the log (allow filtering clause or secondary index column clause) */
  private final boolean logBreaking;

  /** List of clauses for the query */
  private final List<Clause> originalClauses;

  /**
   * @param session {@link #session}
   * @param resultSet {@link #resultSet}
   * @param keyspace {@link #keyspace}
   * @param table {@link #table}
   * @param logBreaking {@link #logBreaking}
   */
  public PathStoreResultSet(
      final Session session,
      final ResultSet resultSet,
      final String keyspace,
      final String table,
      final boolean logBreaking,
      final List<Clause> originalClauses) {
    this.session = session;
    this.resultSet = resultSet;
    this.keyspace = keyspace;
    this.table = table;
    this.logBreaking = logBreaking;
    this.originalClauses = originalClauses;
  }

  public boolean isExhausted() {
    // TODO Auto-generated method stub
    return resultSet.isExhausted();
  }

  public boolean isFullyFetched() {
    // TODO Auto-generated method stub
    return resultSet.isFullyFetched();
  }

  // TODO: Myles: We need to implement this
  public int getAvailableWithoutFetching() {
    throw new UnsupportedOperationException();
  }

  // TODO: Myles: We need to implement this
  public ListenableFuture<ResultSet> fetchMoreResults() {
    throw new UnsupportedOperationException();
  }

  // TODO: Myles: We need to implement this
  public List<Row> all() {
    return this.stream().collect(Collectors.toList());
  }

  /**
   * This function behaves differently depending on what kind of table is used. This is because if a
   * table has a local_ prefix we know that the table does not contain ps meta columns and is not
   * meant to be transferred around the network. This we will provide the standard result set
   * iterator if the table is a local table. Else if the table is a regular ps table we need to
   * 'compress' the log through {@link PathStoreIterator}
   *
   * @return iterator determined by above.
   */
  public Iterator<Row> iterator() {
    return this.table.startsWith(Constants.LOCAL_PREFIX)
        ? resultSet.iterator()
        : new PathStoreIterator(
            this.session,
            this.resultSet.iterator(),
            this.keyspace,
            this.table,
            this.logBreaking,
            this.originalClauses);
  }

  public ExecutionInfo getExecutionInfo() {
    // TODO Auto-generated method stub
    return resultSet.getExecutionInfo();
  }

  public List<ExecutionInfo> getAllExecutionInfo() {
    // TODO Auto-generated method stub
    return resultSet.getAllExecutionInfo();
  }

  /**
   * Unsupported as this will break our immutable log. If its a local table this will work as normal
   *
   * @return see above
   */
  public Row one() {
    if (this.table.startsWith(Constants.LOCAL_PREFIX)) return resultSet.one();
    return this.stream().findFirst().orElse(null);
  }

  /**
   * TODO: Cache iterator calls.
   *
   * @apiNote This should only be used if you don't plan on using the iterator afterwards as the
   *     iterator will skip the first value
   * @return true if empty, else false
   */
  public boolean empty() {
    return !this.iterator().hasNext();
  }

  /** @return Converts the iterator, to an ordered spliterator to allow stream suppourt */
  public Stream<Row> stream() {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(this.iterator(), Spliterator.ORDERED), false);
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

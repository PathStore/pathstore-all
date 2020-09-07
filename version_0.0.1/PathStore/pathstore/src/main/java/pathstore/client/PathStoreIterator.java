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

import com.datastax.driver.core.ArrayBackedRow;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.common.Constants;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;

import java.util.Collection;
import java.util.Iterator;

/** This class is responsible for log compression of cassandra responses */
public class PathStoreIterator implements Iterator<Row> {

  /**
   * Session to connect to. This is because additional database queries will be required if the
   * original select query contained an allow filtering clause or references a secondary index
   */
  private final Session session;

  /** iterator from result set */
  private final Iterator<Row> iter;

  /** What keyspace was the query performed on */
  private final String keyspace;

  /** What table was the query performed on */
  private final String table;

  /**
   * Can the query potentially break our log structure (Does it contain an allow filtering clause or
   * is it using a secondary index)
   */
  private final boolean allowFiltering;

  /** Next row for comparison */
  private ArrayBackedRow row_next = null;

  /** Current row for comparison */
  private ArrayBackedRow row = null;

  /**
   * @param session {@link #session}
   * @param iter {@link #iter}
   * @param keyspace {@link #keyspace}
   * @param table {@link #table}
   * @param allowFiltering {@link #allowFiltering}
   */
  public PathStoreIterator(
      final Session session,
      final Iterator<Row> iter,
      final String keyspace,
      final String table,
      final boolean allowFiltering) {
    this.session = session;
    this.iter = iter;
    this.keyspace = keyspace;
    this.table = table;
    this.allowFiltering = allowFiltering;
  }

  /**
   * This function is used to determine if there is a next row to iterator over.
   *
   * @return true or false, based on if {@link #row} is null or not
   */
  @Override
  public boolean hasNext() {

    if (this.row != null) return true;

    if (this.row_next == null) this.row = (ArrayBackedRow) this.iter.next();
    else this.row = this.row_next;

    this.row_next = (ArrayBackedRow) this.iter.next();

    // handle deleted rows
    while (this.row != null && is_deleted(this.row)) {
      while (this.row_next != null && same_key(this.row, this.row_next))
        this.row_next = (ArrayBackedRow) this.iter.next();
      this.row = this.row_next;
      this.row_next = (ArrayBackedRow) this.iter.next();
    }

    // handle partial rows (updates)
    while (this.row_next != null && same_key(this.row, this.row_next)) {
      merge(this.row, this.row_next);
      this.row_next = (ArrayBackedRow) this.iter.next();
    }

    // handle if a row is out dated
    if (this.allowFiltering && this.hasNewerRows(this.row)) {
      this.row = null;
      return this.hasNext();
    }

    return this.row != null;
  }

  /**
   * Determines if a row is deleted or node
   *
   * @param row row from {@link #hasNext()}
   * @return true or false
   */
  private boolean is_deleted(final ArrayBackedRow row) {
    return row.getBool(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_DELETED);
  }

  /**
   * Determine if two rows have the same primary key
   *
   * @param row row 1 to compare
   * @param row_next row 2 to compare
   * @return true if same primary key else false
   */
  private boolean same_key(final ArrayBackedRow row, final ArrayBackedRow row_next) {
    Collection<Column> columns =
        SchemaInfo.getInstance().getTableColumns(this.keyspace, this.table);

    for (Column col : columns) {
      if (col.kind.compareTo("regular") != 0
          && !col.column_name.startsWith(Constants.PATHSTORE_PREFIX)) {

        Object value1 = row.getObject(col.column_name);
        Object value2 = row_next.getObject(col.column_name);

        if (!value1.equals(value2)) return false;
      }
    }

    return true;
  }

  /**
   * Merge two rows together if a given row is a partial write (update)
   *
   * @param row row 1
   * @param row_next row 2
   */
  private void merge(final ArrayBackedRow row, final ArrayBackedRow row_next) {
    int num_columns = Math.max(row.metadata.asList().size(), row_next.metadata.asList().size());

    for (int x = 0; x < num_columns; x++)
      if (row.data.get(x) == null) row.data.set(x, row_next.data.get(x));
  }

  /**
   * This function is used to determine if a row has a newer version
   *
   * @param row row to check
   * @return false if the row is valid to return to the user, else true if the row is out dated
   * @implNote This only gets called if the log may be broken (allow filtering clause or secondary
   *     index in query)
   */
  private boolean hasNewerRows(final ArrayBackedRow row) {

    if (row == null) return false;

    Select checkForNewerRows = QueryBuilder.select().all().from(this.keyspace, this.table);

    SchemaInfo schemaInfo = SchemaInfo.getInstance();

    // produce a set of all primary key column names
    Collection<String> primaryKeyColumns =
        schemaInfo.getPartitionColumnNames(this.keyspace, this.table);
    primaryKeyColumns.addAll(schemaInfo.getClusterColumnNames(this.keyspace, this.table));

    primaryKeyColumns.forEach(
        columnName ->
            checkForNewerRows.where(QueryBuilder.eq(columnName, row.getObject(columnName))));

    // add greater than clause to pathstore_version
    checkForNewerRows.where(
        QueryBuilder.gt(
            Constants.PATHSTORE_META_COLUMNS.PATHSTORE_VERSION,
            row.getUUID(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_VERSION)));

    return this.session.execute(checkForNewerRows).one() != null;
  }

  /**
   * TODO: Re add removal of pathstore hidden columns
   *
   * <p>This will return the next row and update the internal row
   *
   * @return row to user
   */
  @Override
  public Row next() {

    /*
    // remove pathstore metacolumns
    List<Definition>columns_query = row.metadata.asList();
    for (int x = columns_query.size()-1; x > -1; x--) {
    	String name = columns_query.get(x).getName();
    	if (columns_query.get(x).getName().startsWith("pathstore_")) {
    		row.data.set(x, null);
    	}
    }
    */
    ArrayBackedRow tempRow = this.row;
    this.row = null;
    return tempRow;
  }

  /** Remove from iterator */
  @Override
  public void remove() {
    this.iter.remove();
  }
}

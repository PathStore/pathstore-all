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

import java.util.Collection;
import java.util.Iterator;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.common.Constants;
import pathstore.system.PathStorePriviledgedCluster;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;

import com.datastax.driver.core.ArrayBackedRow;
import com.datastax.driver.core.Row;

/** TODO: Comment */
public class PathStoreIterator implements Iterator<Row> {

  private static final Session priv = PathStorePriviledgedCluster.getInstance().connect();

  private final Iterator<Row> iter;
  private final String keyspace;
  private final String table;
  private final boolean allowFiltering;

  // internal rows for iterator transitions
  private ArrayBackedRow row_next = null;
  private ArrayBackedRow row = null;

  public PathStoreIterator(
      final Iterator<Row> iter,
      final String keyspace,
      final String table,
      final boolean allowFiltering) {
    this.iter = iter;
    this.keyspace = keyspace;
    this.table = table;
    this.allowFiltering = allowFiltering;
  }

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

    // If the user made a query with allow filtering then there is a chance that the row from
    // the database is not the most up to date row. If this is the case we need to check to see if
    // there is a more up to date row. If there is then we cannot return that row to the user
    // because it doesn't actually exist and is part of the row set to a particular primary key.
    if (this.allowFiltering && this.hasNewerRows(this.row)) {
      this.row = null;
      return this.hasNext();
    }

    return this.row != null;
  }

  // TODO: Confirm the only two states pathstore_deleted can be are true and null (there may be a
  // chance it is false)
  private boolean is_deleted(final ArrayBackedRow row) {
    Object value = row.getObject("pathstore_deleted");
    return value != null;
  }

  private boolean same_key(final ArrayBackedRow row, final ArrayBackedRow row_next) {
    Collection<Column> columns =
        SchemaInfo.getInstance().getTableColumns(this.keyspace, this.table);

    for (Column col : columns) {
      if (col.kind.compareTo("regular") != 0 && !col.column_name.startsWith("pathstore_")) {

        Object value1 = row.getObject(col.column_name);
        Object value2 = row_next.getObject(col.column_name);

        if (!value1.equals(value2)) return false;
      }
    }

    return true;
  }

  private void merge(final ArrayBackedRow row, final ArrayBackedRow row_next) {
    int num_columns = Math.max(row.metadata.asList().size(), row_next.metadata.asList().size());

    for (int x = 0; x < num_columns; x++)
      if (row.data.get(x) == null) row.data.set(x, row_next.data.get(x));
  }

  /**
   * This function takes a given row and queries the database iff
   *
   * @param row
   * @return
   */
  private boolean hasNewerRows(final ArrayBackedRow row) {

    if (row == null) return false;

    Select checkForNewerRows = QueryBuilder.select().all().from(this.keyspace, this.table);

    // add where clauses to fix all primary key columns excluding pathstore_version.
    SchemaInfo.getInstance().getTableColumns(this.keyspace, this.table).stream()
        .filter(
            column ->
                column.kind.compareTo("regular") != 0
                    && !column.column_name.startsWith("pathstore_"))
        .map(column -> column.column_name)
        .forEach(
            columnName ->
                checkForNewerRows.where(QueryBuilder.eq(columnName, row.getObject(columnName))));

    // add greater than clause to pathstore_version
    checkForNewerRows.where(
        QueryBuilder.gt(
            Constants.PATHSTORE_COLUMNS.PATHSTORE_VERSION,
            row.getUUID(Constants.PATHSTORE_COLUMNS.PATHSTORE_VERSION)));

    return priv.execute(checkForNewerRows).one() != null;
  }

  // TODO: Re add removal of pathstore hidden columns
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

  @Override
  public void remove() {
    this.iter.remove();
  }
}

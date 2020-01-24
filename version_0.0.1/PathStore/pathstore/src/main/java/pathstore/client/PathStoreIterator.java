package pathstore.client;

import java.util.Iterator;
import java.util.List;

import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;

import com.datastax.driver.core.ArrayBackedRow;
import com.datastax.driver.core.ColumnDefinitions.Definition;
import com.datastax.driver.core.Row;


public class PathStoreIterator implements Iterator<Row> {

	private Iterator<Row> iter;
	private String keyspace;
	private String table;
	private ArrayBackedRow row_next = null;
	private ArrayBackedRow row = null;
	
	public PathStoreIterator(Iterator<Row> iter, String keyspace, String table) {
		this.iter = iter;
		this.keyspace = keyspace;
		this.table = table;
	}

	@Override
	public boolean hasNext() {

		if (row!=null)
			return true;
		
		if (row_next == null)
			row = (ArrayBackedRow) iter.next();
		else
			row = row_next;
		
		row_next = (ArrayBackedRow) iter.next();
		
		// handle deleted rows
		while(row!= null && is_deleted(row)){
			while(row_next != null && same_key(row,row_next)) {
				row_next = (ArrayBackedRow) iter.next();
			}
			row = row_next;
			row_next = (ArrayBackedRow) iter.next();
		}
		
		while(row_next != null && same_key(row,row_next)) {
			merge(row,row_next);
			row_next = (ArrayBackedRow) iter.next();
		}
		
		return row != null;
	}


	
	private boolean is_deleted(ArrayBackedRow row) {
		Object value = row.getObject("pathstore_deleted");
		return value != null;
	}
	
	private boolean same_key(ArrayBackedRow row, ArrayBackedRow row_next) {
		List<Column> columns = SchemaInfo.getInstance().getTableColumns(keyspace, table);
		
		for (Column col : columns ) {
			if (col.kind.compareTo("regular") != 0 && 
				col.column_name.startsWith("pathstore_") == false) {

				Object value1 = row.getObject(col.column_name);
				Object value2 = row_next.getObject(col.column_name);
				
				if (value1.equals(value2) == false)
					return false;
			}
		}
		
		return true;
	}

	private void merge(ArrayBackedRow row, ArrayBackedRow row_next) {
		int num_columns = row.metadata.asList().size() > row_next.metadata.asList().size()? 
				row.metadata.asList().size() : 
				row_next.metadata.asList().size();
		
		for (int x = 0; x < num_columns; x++) {
			if (row.data.get(x)==null)
				row.data.set(x,row_next.data.get(x));
		}
	}

	
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
		ArrayBackedRow tempRow = row;
		row = null;
		return tempRow;
	}

	@Override
	public void remove() {
		iter.remove();
	}

}

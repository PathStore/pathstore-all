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
package smartpath;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

public class AppEntry {

	int appId;
	List<Integer> funcs;
	String owner;
	String root_domain;
	ByteBuffer code;
	String augmentedSchema;
	

	public AppEntry(Session session, int appid)
	{
		this.appId=appid;
		Select stmt = QueryBuilder
				.select()
				.from("pathstore_applications","apps");
		stmt.where(QueryBuilder.eq("appid", appId));
		Row fileRow = session.execute(stmt).one();

		if ( fileRow != null ) {
			code = fileRow.getBytes("code");
			owner = fileRow.getString("owner");
			root_domain= fileRow.getString("root_domain");
			funcs = fileRow.getList("funcs", Integer.class);
		}

	}

	public AppEntry(Session session, String root_domain) {
		this.root_domain=root_domain;
		Select stmt = QueryBuilder
				.select()
				.from("pathstore_applications","apps");
		stmt.where(QueryBuilder.eq("root_domain", root_domain));
		stmt.allowFiltering();
		
		Row fileRow = session.execute(stmt).one();

		if ( fileRow != null ) {
			code = fileRow.getBytes("code");
			owner = fileRow.getString("owner");
			appId= fileRow.getInt("appid");
			funcs = fileRow.getList("funcs", Integer.class);
		}
	}

	public ArrayList<FuncEntry> getFuncEntries(Session session)
	{
		ArrayList<FuncEntry> fentries = new ArrayList<>();
		Select stmt = QueryBuilder.select().from("pathstore_applications","funcs");
		stmt.where(QueryBuilder.eq("appid", appId));
		stmt.allowFiltering();
		session.execute(stmt);
		ResultSet rows= session.execute(stmt);
	    while (!rows.isExhausted()) {
	    	Row fileRow = rows.one(); 
	    	fentries.add(new FuncEntry(fileRow));
	    }
	    
	    return fentries;
	}
}

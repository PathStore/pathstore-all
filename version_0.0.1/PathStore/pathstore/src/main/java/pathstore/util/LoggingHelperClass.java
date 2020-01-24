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
package pathstore.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

public class LoggingHelperClass {

	
	static Cluster cluster = Cluster.builder().addContactPoint("10.70.20.154").withPort(9052).build();
	static Session 	session = cluster.connect();

	public static long getTotalBytes(String name)
	{

		long total=0;

		if(name ==null)
			name="stats-2";
		try {

			ProcessBuilder pb = new ProcessBuilder("/home/mortazavi/nfs/smartpath/mobisys-"+name+".sh");
			//			pb.redirectOutput(Redirect.INHERIT);
			//			pb.redirectError(Redirect.INHERIT);
			Process process = pb.start();

			//			String[] command = { "/home/hossein/workspaceCloud/mobisys-stats.sh"};         
			//			Process process = Runtime.getRuntime().exec(command);                    
			BufferedReader reader = new BufferedReader(new InputStreamReader(        
					process.getInputStream()));                                          
			String s;                                                                
			while ((s = reader.readLine()) != null) {
				//System.out.println(s);
				if(s.equals("INPUT") || s.equals("OUTPUT"))
					continue;
				long p = Long.parseLong(s);
				total+=p;


			}
			process.waitFor();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("tamume " + total);
		return total;
	}
	
	static public void writeToMainDB(String text, long value)
	{
		Insert ins = QueryBuilder.insertInto("HosseinLogging","bandwidth")
				.value("id", text)
				.value("value", value);
		
		session.execute(ins);
	}
	
	static public long readFromMainDB(String text)
	{
		Select s = QueryBuilder.select().from("HosseinLogging","bandwidth");
		s.where(QueryBuilder.eq("id",text));
		Row r = session.execute(s).one();
		return r.getLong("value");

	}
	
	public static void main (String[] args)
	{
		Statement statement = new SimpleStatement("select * from pathstore_demo.users");
		statement.setFetchSize(1000);
		ResultSet rs = session.execute(statement);

		int i=0;
		for (Row row : rs) {
			i++;
		}
		
		System.out.println(i);
	}

}

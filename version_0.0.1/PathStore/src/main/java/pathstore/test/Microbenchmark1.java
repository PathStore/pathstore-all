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
package pathstore.test;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

import pathstore.client.PathStoreCluster;
import pathstore.common.PathStoreProperties;

public class Microbenchmark1 {


	private static void parseCommandLineArguments(String args[]) {
		Options options = new Options();

		options.addOption( Option.builder().longOpt( "rmiport" )
				.desc( "NUMBER" )
				.hasArg()
				.argName("PORT")
				.build() );

		options.addOption( Option.builder().longOpt( "cassandraport" )
				.desc( "NUMBER" )
				.hasArg()
				.argName("PORT")
				.build() );


		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);
			System.exit(1);
			return;
		}

		if (cmd.hasOption("rmiport")) 
			PathStoreProperties.getInstance().RMIRegistryPort = Integer.parseInt(cmd.getOptionValue("rmiport"));

		if (cmd.hasOption("cassandraport")) 
			PathStoreProperties.getInstance().CassandraPort = Integer.parseInt(cmd.getOptionValue("cassandraport"));

	}



	static public void main(String args[]) throws InterruptedException {
		//parseCommandLineArguments(args);

		

		PathStoreProperties p1 = new PathStoreProperties();
		p1.CassandraIP="10.70.20.156";
		p1.RMIRegistryIP=p1.CassandraIP;
		PathStoreCluster cluster = new PathStoreCluster(p1);
		Session session = cluster.connect();

		PathStoreProperties p2 = new PathStoreProperties();
		p2.CassandraIP="10.70.20.1";
		p2.RMIRegistryIP=p2.CassandraIP;

		PathStoreCluster cluster2 = new PathStoreCluster(p2);
		Session session2 = cluster2.connect();
		
//		Cluster cluster2 = Cluster.builder().addContactPoint("10.70.20.154").withPort(9052).build();
//		Session session2 = cluster2.connect();
		
		
		String name = "hossein";

		Insert insert = QueryBuilder.insertInto("pathstore_demo", "users");
		insert.value("name", name);
		insert.value("sport", "test");
		insert.value("years", 2);
		insert.value("vegetarian", true);

		session.execute(insert);

//		Thread.sleep(1000);
		long d= System.nanoTime();



		Select select = QueryBuilder.select().all().from("pathstore_demo", "users");
		//select.allowFiltering();
		//select.where(QueryBuilder.eq("name", name));

		int i=0;
		try {
			while(true) {
				ResultSet results = session2.execute(select);
				if(i>0)
					break;
				for(Row row : results)
				{
					System.out.println("time took: " + (System.nanoTime()-d)/1000000.0);
					System.out.print(row.getString("name"));
					i++;
					break;

				}
				System.out.print("\t");
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("");


		for(int k=0;k<100;k++)
		{
			name = "eyal"+k;
			insert = QueryBuilder.insertInto("pathstore_demo", "users");
			insert.value("name", name);
			insert.value("sport", "test");
			insert.value("years", 2);
			insert.value("vegetarian", true);

			session.execute(insert);
			d= System.nanoTime();

			select = QueryBuilder.select().all().from("pathstore_demo", "users");
			//select.allowFiltering();
			//select.where(QueryBuilder.eq("name", name));

			i=0;
			try {
				while(true) {
					ResultSet results = session2.execute(select);
					if(i>0)
						break;
					for(Row row : results)
					{
						if(row.getString("name").equals(name))
						{
							System.out.println(row.getString("name"));
							System.out.println("time took: " + (System.nanoTime()-d)/1000000.0);
							i++;
							break;
						}

					}
				}
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
		}

		//		session.close();

		System.exit(0);
	}
}

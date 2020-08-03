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

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

import pathstore.client.PathStoreCluster;

public class Microbenchmark7 {


	static public void main(String args[]) throws InterruptedException {
		//parseCommandLineArguments(args);



		//PathStoreProperties p1 = new PathStoreProperties();
		//p1.CassandraIP="10.70.20.156";
		//p1.RMIRegistryIP=p1.CassandraIP;
		//PathStoreCluster cluster = new PathStoreCluster(p1);

		PathStoreCluster cluster = PathStoreCluster.getDaemonInstance();
		Session sessionReader = cluster.connect();

		long sum =0;
		int entrynum = 100;
		for(int i=0; i<entrynum;i++)
		{
//			Insert insert = QueryBuilder.insertInto("pathstore_demo", "users");
//			insert.value("name", "Test"+i);
//			insert.value("sport", sb.toString());
//			System.out.println("Running insert!");
//			sessionWriterRoot.execute(insert);

			Select select = QueryBuilder.select().from("pathstore_demo", "users");
			select.where(QueryBuilder.eq("name", "Test"+i));
			long d = System.nanoTime();
			//select = QueryBuilder.select().all().from("pathstore_demo", "users");
			//		Row r=null;
			//		while(r==null)
			//		{
			//			r = sessionWriter2.execute(select).one();
			//		}


			//


			//select.allowFiltering();
			//System.out.println("Checking!");
			try {
				while(true) {
					Row r =  sessionReader.execute(select).one();
//					System.out.print(".");
					if(r !=null)
					{
						//Row row = results.one();
						//if(row.getString("spo").equals(i))
						//{
						//System.out.println();
						System.out.println((System.nanoTime()-d)/1000000.0);
					
						break;
						//}
					}
					else
						Thread.sleep(20);
				}
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		//		session.close();
		System.out.println("sum : " + sum/100);
		System.exit(0);
	}
}
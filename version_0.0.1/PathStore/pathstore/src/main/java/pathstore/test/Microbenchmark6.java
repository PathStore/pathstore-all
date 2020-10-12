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

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import pathstore.client.PathStoreCluster;
import pathstore.common.PathStoreProperties;

public class Microbenchmark6 {


	static public void main(String args[]) throws InterruptedException {
		//parseCommandLineArguments(args);


		PathStoreProperties psRoot = new PathStoreProperties();
		psRoot.CassandraIP="10.70.20.154";
		psRoot.GRPCIP =psRoot.CassandraIP;
		PathStoreCluster clusterRoot = new PathStoreCluster(psRoot);

//		
//		Cluster clusterRoot;
//		clusterRoot = Cluster.builder()
//				.addContactPoint("10.70.20.154") 
//				.withPort(9052).build();

		
		Session sessionWriterRoot = clusterRoot.connect();

		Thread.sleep(100);

		//int lvl=11;
		int count =1024;
		StringBuilder sb = new StringBuilder(count);
		for( int i=0; i<count; i++ ) {
			sb.append("*"); 
		}

		long sum =0;
		int entrynum = (args.length==0) ? 0: Integer.parseInt(args[0]); 

		for(int i=0; i<entrynum;i++)
		{
			Insert insert = QueryBuilder.insertInto("pathstore_demo", "users");
			insert.value("name", "Test"+i);
			insert.value("sport", sb.toString());
			//System.out.println("Running insert!");
			long d = System.nanoTime();
			sessionWriterRoot.execute(insert);
			//System.out.println((System.nanoTime()-d)/1000000.0);
			//Thread.sleep(100);
		}
		
		System.exit(0);

	}
}
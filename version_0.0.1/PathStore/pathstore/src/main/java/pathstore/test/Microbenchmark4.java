package pathstore.test;

import java.util.UUID;

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

public class Microbenchmark4 {


	static public void main(String args[]) throws InterruptedException {
		//parseCommandLineArguments(args);



	
		
		Select select = QueryBuilder.select().from("pathstore_demo", "users");
		
//		Session sessionReader= cluster.connect();
		
		
//		for(int i=0; i<1;i++)
//		{
//			//select.where(QueryBuilder.eq("name", "Test"+i));
//			try{
//				System.out.println("new select:" + select);
//				//sessionReader.execute(select);
//			}
//			catch(Exception e)
//			{
//				e.printStackTrace();
//			}
//			
//		}
		
		PathStoreCluster clusterR = PathStoreCluster.getInstance();
		Session sessionR = clusterR.connect();
		select = QueryBuilder.select().from("pathstore_demo", "users");
		sessionR.execute(select);

		//writer
		PathStoreProperties p1 = new PathStoreProperties();
		p1.CassandraIP="10.70.20.154";
		p1.RMIRegistryIP=p1.CassandraIP;
		PathStoreCluster clusterW = new PathStoreCluster(p1);
		Session sessionWriter = clusterW.connect();

		
		//reader
		PathStoreProperties psRoot = new PathStoreProperties();
		psRoot.CassandraIP="10.70.20.153";
		psRoot.RMIRegistryIP=psRoot.CassandraIP;
		PathStoreCluster cluster2= new PathStoreCluster(psRoot);
		Session sessionReader= cluster2.connect();


//		Cluster clusterReaderNode;
//		clusterReaderNode = Cluster.builder()
//				.addContactPoint("10.70.20.154") 
//				.withPort(9052).build();
//		Session sessionReaderNode = clusterReaderNode.connect();
//		sessionReaderNode.execute(select);

		//		Cluster clusterWriter2;
		//		clusterWriter2 = Cluster.builder() 
		//				.addContactPoint("10.70.20.156") 
		//				.withPort(9052).build();
		//		Session sessionWriter2 = clusterWriter2.connect();

		//int lvl=11;
		int count =1024;
		StringBuilder sb = new StringBuilder(count);
		for( int i=0; i<count; i++ ) {
			sb.append("*"); 
		}

		long sum =0;
		int entrynum = 100;
		select = QueryBuilder.select().from("pathstore_demo", "users");
		Row r =  sessionReader.execute(select).one();

		String key = "bzzmn";
		for(int i=0; i<entrynum;i++)
		{
			Insert insert = QueryBuilder.insertInto("pathstore_demo", "users");
			insert.value("name", key+i);
			insert.value("sport", sb.toString());
			System.out.println("Running insert!");
			long d = System.nanoTime();
			sessionWriter.execute(insert);
			select.where(QueryBuilder.eq("name", key+i));
			System.out.println("Elapsed since insert : " + (System.nanoTime()-d)/1000000.0);
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
					r =  sessionReader.execute(select).one();
//					System.out.print(".");
					if(r !=null)
					{
						//Row row = results.one();
						//if(row.getString("spo").equals(i))
						//{
						//System.out.println();
						System.out.println("Elapsed since insert : " + (System.nanoTime()-d)/1000000.0);
						UUID UUID1 = r.getUUID("pathstore_parent_timestamp");
						UUID UUID2 = r.getUUID("pathstore_version"); 
						System.out.println((UUID1.timestamp()-UUID2.timestamp())/10000.0);
						break;
						//}
					}
					else
					{
						//Thread.sleep(20);
					}
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
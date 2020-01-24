package pathstore.test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

public class Microbenchmark5 {

	static int entryNum=100;

	static public void main(String args[]) throws InterruptedException {
		//parseCommandLineArguments(args);



		//PathStoreProperties p1 = new PathStoreProperties();
		//p1.CassandraIP="10.70.20.156";
		//p1.RMIRegistryIP=p1.CassandraIP;
		//PathStoreCluster cluster = new PathStoreCluster(p1);

		PathStoreCluster cluster = PathStoreCluster.getInstance();
		Session sessionReader = cluster.connect();

		for(int i=0; i<1;i++)
		{
			Select select = QueryBuilder.select("sport").from("pathstore_demo", "users");
			//select.where(QueryBuilder.eq("name", "Test"+i));
			System.out.println("query: " + i  + select.toString());

			try{
				sessionReader.execute(select);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

		}
		AtomicInteger current = new AtomicInteger(0);

		PathStoreProperties p2 = new PathStoreProperties();
		p2.CassandraIP="10.70.20.154";
		p2.RMIRegistryIP=p2.CassandraIP;

		PathStoreCluster cluster2 = new PathStoreCluster(p2);
		Session sessionWriter = cluster2.connect();




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

		myLong d=new myLong();
		new Thread(new ReadThread(current,d)).start();
		long sum =0;
		int wrote=current.get();
		while(current.get()<entryNum)
		{
			if( current.get()==wrote)
			{
				Insert insert = QueryBuilder.insertInto("pathstore_demo", "users");
				insert.value("name", "Test"+current);
				insert.value("sport", sb.toString());
				//System.out.println("writing!");
				//long d = System.nanoTime();
				//System.out.println("preparing inserting: "+ current.get() + " " + wrote);
				d.d = System.nanoTime();
				sessionWriter.execute(insert);
				//System.out.println("inserting: "+ current.get() + " " + wrote);
				wrote++;
			}
			
		}

		for(int i=0; i<entryNum;i++)
		{

			//select = QueryBuilder.select().all().from("pathstore_demo", "users");
			//		Row r=null;
			//		while(r==null)
			//		{
			//			r = sessionWriter2.execute(select).one();
			//		}


			//

			//select.allowFiltering();

		}
		//		session.close();
		System.out.println("sum : " + sum/100);
		System.exit(0);
	}
}


class ReadThread implements Runnable {
	Cluster clusterReader;
	Session sessionReader2;
	AtomicInteger current;
	Select select;
	myLong d;
	public ReadThread(AtomicInteger current, myLong d) {

		clusterReader = Cluster.builder() 
				.addContactPoint("10.70.20.156") 
				.withPort(9052).build();
		sessionReader2 = clusterReader.connect();
		this.current=current;
		// store parameter for later user
		select= QueryBuilder.select("pathstore_version","pathstore_parent_timestamp").from("pathstore_demo", "users");
		select.where(QueryBuilder.eq("name", "Test"+current));
		this.d=d;
	}

	public void run() {

		try {
			while(current.get()<Microbenchmark5.entryNum) {

				Row r =  sessionReader2.execute(select).one();
				if(r !=null)
				{
					System.out.println("nano: " + (System.nanoTime()-d.d)/1000000.0);
					//Row row = results.one();
					//if(row.getString("spo").equals(i))
					//{
					//System.out.println("nanotime : " + (System.nanoTime()-d)/1000000.0);
					int c = current.incrementAndGet();
					//System.out.println("increamented");
					UUID UUID1 = r.getUUID("pathstore_parent_timestamp");  
					UUID UUID2 = r.getUUID("pathstore_version"); 
					System.out.println((UUID1.timestamp()-UUID2.timestamp())/10000.0);
					//current.incrementAndGet();
					//sum +=(UUID1.timestamp()-UUID2.timestamp())/10000.0;
					select= QueryBuilder.select("pathstore_version","pathstore_parent_timestamp").from("pathstore_demo", "users");
					select.where(QueryBuilder.eq("name", "Test"+c));
					//break;
					//}
				}
				//else
					//Thread.sleep(50);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}


class myLong
{
	public Long d;
	public myLong()
	{
		d= new Long(System.nanoTime());
	}
}

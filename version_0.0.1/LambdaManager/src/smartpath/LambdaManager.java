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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.Configuration.Parameters;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.sun.net.httpserver.HttpServer;

import pathstore.client.PathStoreCluster;
import pathstore.util.ApplicationSchema;

public class LambdaManager {


	public static String CassandraIP = "localhost";
	public static String CassandraKEYSPACE = "pathstore_applications";
	public static String LambdaManagerLOCATION= "edge";
	public static String LambdaManagerIP = "lambdamanager";

	Session session;
	//Cluster cluster;
	PathStoreCluster cluster;
	
	HttpServer server;
	ScheduledExecutorService scheduledExecutorService;
	List<Integer> installedFunctions;
	Configuration config;


	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		

		LambdaManager lm = new LambdaManager();
		lm.initConfiguration();
		lm.initDB();
		lm.initHttp();
		lm.initScheduler();
		
		
	}
	
	private void initConfiguration() {
		  try {
			config = new PropertiesConfiguration("config.properties");
			
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  
		CassandraIP = config.getString("Cassandra.ip");
		CassandraKEYSPACE = config.getString("Cassandra.keyspace");
		LambdaManagerLOCATION = config.getString("LambdaManager.location");
		LambdaManagerIP = config.getString("LambdaManager.ip");
	}

	private void initScheduler() {
		// TODO Auto-generated method stub
		scheduledExecutorService =
		        Executors.newScheduledThreadPool(5);

		ScheduledFuture scheduledFuture =
		    scheduledExecutorService.scheduleAtFixedRate( new Runnable(){
		    	public void run(){
		    		
			    //TODO: Check status of each application here
		    	//System.out.println("inside scheduled future");
				for(int funcId: installedFunctions)
				{
					Select stmt = QueryBuilder
							.select("deploy_strategy")
							.from("pathstore_applications","funcs");
					stmt.where(QueryBuilder.eq("funcid", funcId));
					stmt.allowFiltering();
					
					Row fileRow = session.execute(stmt).one();
					
					String response = fileRow.getString(0);
					if(!response.equals("edge"))
					{
						//SALEHE:
						//packet should now be routed to the core, tell proxy
					}
				}
			}},
		    0,
		    1,
		    TimeUnit.SECONDS);

	}

	private void initDB()
	{
//		cluster = Cluster.builder() 
//				.addContactPoint(CassandraIP) 
//				.withPort(9042).build();
        cluster = PathStoreCluster.getInstance();
		session = cluster.connect();


		//session.execute("use " + CassandraKEYSPACE);
		installedFunctions = (List<Integer>) Collections.synchronizedList(new ArrayList<Integer>());
	}
	
	private void initHttp() throws IOException
	{
		server = HttpServer.create(new InetSocketAddress(8000), 0);
		server.createContext("/getPreference", new RequestHandler(session, installedFunctions, config));
		server.createContext("/code/", new HttpCodeDownloadHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
	}

	public void close()
	{
		server.stop(0);
		session.closeAsync();
		//cluster.closeAsync();
		
		
	}
}

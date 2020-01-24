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
package pathstore.system;

import java.io.IOException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pathstore.client.PathStoreCluster;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCache;
import pathstore.common.QueryCacheEntry;
import pathstore.common.Role;
import pathstore.common.Timer;
import pathstore.exception.PathMigrateAlreadyGoneException;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;
import pathstore.util.SchemaInfo.Table;

import org.apache.commons.cli.*;

public class PathStoreServerImpl implements PathStoreServer {
	private final Logger logger = LoggerFactory.getLogger(PathStoreServerImpl.class);

	public PathStoreServerImpl() {}


	@Override //child calls this (maybe client or child node)
	public String addUserCommandEntry(String user, String keyspace, String table, byte[] clauses, int limit)
			throws RemoteException, PathMigrateAlreadyGoneException {

		//System.out.println("In addUserCommandEntry " + user + ":"+ keyspace + ":" + table + " " + clauses);

		try {
			QueryCache.getInstance().updateDeviceCommandCache(user, keyspace, table, clauses, limit);
		} catch (Exception e) {
			if(e instanceof PathMigrateAlreadyGoneException)
				throw new RemoteException("PathMigrateAlreadyGoneException");
			else
				throw new RemoteException(e.getMessage());
		}

		return "server says hello! in user command entry";
	}

	public String addQueryEntry(String keyspace, String table,
			byte[]clauses, int limit) throws RemoteException {
		logger.info("In addQueryEntry " + keyspace + ":" + table + " " + clauses);

		long d = System.nanoTime();
		try {
			QueryCache.getInstance().updateCache(keyspace, table, clauses, limit);
//			System.out.println("^^^^^^^^^^^^^^^^ time to reply took: " + Timer.getTime(d));
			
		} catch (ClassNotFoundException | IOException e) {
			throw new RemoteException(e.getMessage());
		}

		return "server says hello!";
	}

	@Override
	public UUID createQueryDelta(String keyspace, String table,
			byte[] clauses, UUID parentTimestamp, int nodeID, int limit) throws RemoteException {
		logger.info("In createQueryDelta " + keyspace + ":" + table + " " + clauses + " pts:" +
				parentTimestamp.timestamp() + " " + nodeID);
		
		try {
			return QueryCache.getInstance().createDelta(keyspace, table, clauses, parentTimestamp, nodeID, limit);
		} catch (ClassNotFoundException | IOException e) {
			throw new RemoteException(e.getMessage());
		}
	}


	private static void parseCommandLineArguments(String args[]) {
		Options options = new Options();

		//options.addOption( "a", "all", false, "do not hide entries starting with ." );


		options.addOption( Option.builder("r").longOpt( "role" )
				.desc( "[CLIENT|SERVER|ROOTSERVER]" )
				.hasArg()
				.argName("ROLE")
				.build() );

		options.addOption( Option.builder().longOpt( "rmiport" )
				.desc( "NUMBER" )
				.hasArg()
				.argName("PORT")
				.build() );

		options.addOption( Option.builder().longOpt( "rmiportparent" )
				.desc( "NUMBER" )
				.hasArg()
				.argName("PORT")
				.build() );

		options.addOption( Option.builder().longOpt( "cassandraport" )
				.desc( "NUMBER" )
				.hasArg()
				.argName("PORT")
				.build() );

		options.addOption( Option.builder().longOpt( "cassandraportparent" )
				.desc( "NUMBER" )
				.hasArg()
				.argName("PORT")
				.build() );

		options.addOption( Option.builder("n").longOpt( "nodeid" )
				.desc( "Number" )
				.hasArg()
				.argName("nodeid")
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

		if (cmd.hasOption("role")) {
			switch(cmd.getOptionValue("role")){ 
			case "SERVER":
				PathStoreProperties.getInstance().role = Role.SERVER;
				break;
			case "ROOTSERVER":
				PathStoreProperties.getInstance().role = Role.ROOTSERVER;
				break;
			case "CLIENT":
				PathStoreProperties.getInstance().role = Role.CLIENT;
				break;
			}
		}

		if (cmd.hasOption("rmiport")) 
			PathStoreProperties.getInstance().RMIRegistryPort = Integer.parseInt(cmd.getOptionValue("rmiport"));


		if (cmd.hasOption("rmiportparent")) 
			PathStoreProperties.getInstance().RMIRegistryParentPort = Integer.parseInt(cmd.getOptionValue("rmiportparent"));


		if (cmd.hasOption("cassandraport")) 
			PathStoreProperties.getInstance().CassandraPort = Integer.parseInt(cmd.getOptionValue("cassandraport"));

		if (cmd.hasOption("cassandraportparent")) 
			PathStoreProperties.getInstance().CassandraParentPort = Integer.parseInt(cmd.getOptionValue("cassandraportparent"));


		if (cmd.hasOption("nodeid")) 
			PathStoreProperties.getInstance().NodeID = Integer.parseInt(cmd.getOptionValue("nodeid"));

	}

	

	public static void main(String args[]) {
		try {

			parseCommandLineArguments(args);

			PathStoreServerImpl obj = new PathStoreServerImpl();
			PathStoreServer stub = (PathStoreServer) UnicastRemoteObject.exportObject(obj, 0);
			
			// Bind the remote object's stub in the registry
			
			System.setProperty("java.rmi.server.hostname",PathStoreProperties.getInstance().RMIRegistryIP);

			Registry registry = LocateRegistry.createRegistry(PathStoreProperties.getInstance().RMIRegistryPort);
			
			//Registry registry = LocateRegistry.createRegistry(PathStoreProperties.getInstance().RMIRegistryPort);
			//Registry registry = LocateRegistry.getRegistry(PathStoreProperties.getInstance().RMIRegistryIP, PathStoreProperties.getInstance().RMIRegistryPort);

			try {
				registry.bind("PathStoreServer", stub);
			}
			catch(Exception ex) {
				registry.rebind("PathStoreServer", stub);
			}

			System.err.println("PathStoreServer ready");


			if (PathStoreProperties.getInstance().role != Role.ROOTSERVER) {
				PathStorePullServer server = new PathStorePullServer();
				server.start();
				//   server.join();
			}

			if (PathStoreProperties.getInstance().role != Role.ROOTSERVER) {
				PathStorePushServer server = new PathStorePushServer();
				server.start();
				server.join();
			}

			
			

		} catch (Exception e) {
			System.err.println("PathStoreServer exception: " + e.toString());
			e.printStackTrace();
		}
		
		// Commented 2 lines handle session consistency.
		
		//PathStoreMigrateImpl.getInstance();
		//PathStoreAuthenticateImpl.getInstance();
	}

}
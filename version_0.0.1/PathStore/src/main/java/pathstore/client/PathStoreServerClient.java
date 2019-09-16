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
package pathstore.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCacheEntry;
import pathstore.common.Role;
import pathstore.exception.PathStoreRemoteException;

public class PathStoreServerClient {

	static private PathStoreServerClient instance=null;
	
	private PathStoreServer stub;
	
	static public PathStoreServerClient getInstance()  {
		if (PathStoreServerClient.instance == null) 
			PathStoreServerClient.instance = new PathStoreServerClient();
		return PathStoreServerClient.instance;
	}
	
	public PathStoreServerClient()  {
		try {
			Registry registry=null;
			 
			if (PathStoreProperties.getInstance().role == Role.SERVER)
				registry = LocateRegistry.getRegistry(PathStoreProperties.getInstance().RMIRegistryParentIP,PathStoreProperties.getInstance().RMIRegistryParentPort);
			else
				registry = LocateRegistry.getRegistry(PathStoreProperties.getInstance().RMIRegistryIP,PathStoreProperties.getInstance().RMIRegistryPort);
			
			
		    stub = (PathStoreServer) registry.lookup("PathStoreServer");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new PathStoreRemoteException();
		}
	}
	
	public void addQueryEntry(QueryCacheEntry entry) {
		try {
			String result = stub.addQueryEntry(entry.getKeyspace(), entry.getTable(), entry.getClausesSerialized());
			result = "";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new PathStoreRemoteException();
		}
	}

	public UUID cretateQueryDelta(QueryCacheEntry entry) {
		try {
			return stub.createQueryDelta(entry.getKeyspace(),entry.getTable(), 
										 entry.getClausesSerialized(),
										 entry.getParentTimeStamp(),
										 PathStoreProperties.getInstance().NodeID);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new PathStoreRemoteException();
		}
	}
	
}



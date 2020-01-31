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

import pathstore.common.PathStoreProperties;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;

/**
 * TODO: Copy of {@link pathstore.client.PathStoreCluster}
 */
public class PathStorePriviledgedCluster {

		static PathStorePriviledgedCluster pathStoreCluster=null;
		
		private Cluster cluster=null;
		private Session session=null; 
		
		public static PathStorePriviledgedCluster getInstance() {
			if (PathStorePriviledgedCluster.pathStoreCluster == null) 
				PathStorePriviledgedCluster.pathStoreCluster = new PathStorePriviledgedCluster();
			return PathStorePriviledgedCluster.pathStoreCluster;
		}
		
		public PathStorePriviledgedCluster() {
			System.out.println("Connecting to:" + 
					PathStoreProperties.getInstance().CassandraIP + " " +
					PathStoreProperties.getInstance().CassandraPort);
			
			
			this.cluster = new Cluster.Builder()
		            .addContactPoints(PathStoreProperties.getInstance().CassandraIP)
					.withPort(PathStoreProperties.getInstance().CassandraPort)
		            .withSocketOptions(new SocketOptions().setTcpNoDelay(true).setReadTimeoutMillis(15000000))
		            .withQueryOptions(
		                    new QueryOptions()
		                        .setRefreshNodeIntervalMillis(0)
		                        .setRefreshNodeListIntervalMillis(0)
		                        .setRefreshSchemaIntervalMillis(0)
		                ).build();
			session = this.cluster.connect();
		}
		
		//Hossein
		public Metadata getMetadata()
		{
			return cluster.getMetadata();
		}
		
		//Hossein: why not put this in the constructor?
		public Session connect() {
			//return this.cluster.connect();
			return session;
		}

}

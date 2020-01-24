package pathstore.system;

import pathstore.common.PathStoreProperties;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;

public class PathStoreParentCluster {

		static PathStoreParentCluster pathStoreCluster=null;
		
		private Cluster cluster=null;
		private Session session=null;
		
		public static PathStoreParentCluster getInstance() {
			if (PathStoreParentCluster.pathStoreCluster == null) 
				PathStoreParentCluster.pathStoreCluster = new PathStoreParentCluster();
			return PathStoreParentCluster.pathStoreCluster;
		}
		
		public PathStoreParentCluster() {
			
//			System.out.println("Connecting to Parent:" + 
//					PathStoreProperties.getInstance().CassandraParentIP + " " +
//					PathStoreProperties.getInstance().CassandraParentPort);
			
			
			this.cluster = new Cluster.Builder()
					.addContactPoints(PathStoreProperties.getInstance().CassandraParentIP)
					.withPort(PathStoreProperties.getInstance().CassandraParentPort)
		            .withSocketOptions(new SocketOptions().setTcpNoDelay(true).setReadTimeoutMillis(15000000))
		            .withQueryOptions(
		                    new QueryOptions()
		                        .setRefreshNodeIntervalMillis(0)
		                        .setRefreshNodeListIntervalMillis(0)
		                        .setRefreshSchemaIntervalMillis(0)
		                ).build();
			this.session = cluster.connect();
		}
		
		public Session connect() {
			//return this.cluster.connect();
			return session;
		}
}

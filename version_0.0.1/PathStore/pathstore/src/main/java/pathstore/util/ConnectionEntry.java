package pathstore.util;

import com.datastax.driver.core.Session;

import pathstore.common.PathStoreMigrate;

public class ConnectionEntry
{
	public int parentId;

	public PathStoreMigrate stub;
	public Session session;

	public String RmiIP;
	public int PortRmi;

	public String PathStoreIP;
	public int PathStorePort;



	public ConnectionEntry(int parentId, PathStoreMigrate stub, Session session, String rmiIP, int portRmi, String pathStoreIP,
			int pathStorePort) {
		super();
		this.parentId = parentId;
		this.stub = stub;
		this.session = session;
		RmiIP = rmiIP;
		PortRmi = portRmi;
		PathStoreIP = pathStoreIP;
		PathStorePort = pathStorePort;
	}
}
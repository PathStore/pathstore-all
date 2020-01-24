package pathstore.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.datastax.driver.core.querybuilder.Clause;


public interface PathStoreAuthenticate extends Remote {

	boolean authenticateSession(String sid, String username, String password) throws RemoteException;
	String getUser(String sid) throws RemoteException;
	boolean isAuthenticated(String sid) throws RemoteException;

	
}

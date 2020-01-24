package pathstore.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.datastax.driver.core.querybuilder.Clause;


public interface PathStoreMigrate extends Remote {
	String migrate(String SID, String previousEdge) throws RemoteException;
	ArrayList<CommandEntryReply> sendData(String sid, String edgeId, boolean neighbors,
//	HashMap<String, HashMap<String, HashMap<Object,UUID>>> rowsOnDest,
	HashMap<String, HashMap<Object, UUID>> differenceList) throws RemoteException;
	
	String fetchFromParent(ArrayList<CommandEntryReply> queriesToExecute, String previousEdge) throws RemoteException;
	
}

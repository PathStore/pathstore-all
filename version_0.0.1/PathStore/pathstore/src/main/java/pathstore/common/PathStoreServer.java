package pathstore.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import com.datastax.driver.core.querybuilder.Clause;

import pathstore.exception.PathMigrateAlreadyGoneException;


public interface PathStoreServer extends Remote {
	String addQueryEntry(String keyspace, String table, byte[] clauses, int limit) throws RemoteException;
	
	String addUserCommandEntry(String user, String keyspace, String table, byte[] clauses, int limit) throws RemoteException, PathMigrateAlreadyGoneException;

	UUID createQueryDelta(String keyspace, String table, byte[] clauses, UUID parentTimestamp, int nodeID, int limit) throws RemoteException;
	
}

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
package pathstore.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.datastax.driver.core.querybuilder.Clause;


/**
 * TODO: Not used currently most likely WIP
 */
public interface PathStoreMigrate extends Remote {
	String migrate(String SID, String previousEdge) throws RemoteException;
	ArrayList<CommandEntryReply> sendData(String sid, String edgeId, boolean neighbors,
//	HashMap<String, HashMap<String, HashMap<Object,UUID>>> rowsOnDest,
	HashMap<String, HashMap<Object, UUID>> differenceList) throws RemoteException;
	
	String fetchFromParent(ArrayList<CommandEntryReply> queriesToExecute, String previousEdge) throws RemoteException;
	
}

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
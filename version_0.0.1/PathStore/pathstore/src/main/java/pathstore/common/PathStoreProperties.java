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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class PathStoreProperties {
	static private PathStoreProperties instance=null;
	
	public int NodeID=1;
	
	public int ParentID=-1;
	
	public String RMIRegistryIP=null;
	public int RMIRegistryPort=1099;
	public String RMIRegistryParentIP=null;
	public int RMIRegistryParentPort=1100;
	
	public Role role=null;
	public String CassandraPath = null;
	
	public String CassandraIP=null;
	public int CassandraPort=0;
	
	public String CassandraParentIP=null;
	public int CassandraParentPort=0;

	public int MaxBatchSize=4096*10; // 
	
	public int PullSleep=1000;   // sleep period in milliseconds
	public int PushSleep=1000;   // sleep period in milliseconds
	
	
	
	static public PathStoreProperties getInstance() {
		if (PathStoreProperties.instance == null) 
			PathStoreProperties.instance = new PathStoreProperties();
		return PathStoreProperties.instance;
	}
	
	public PathStoreProperties() {
		try {
			Properties props = new Properties();
			FileInputStream in = new FileInputStream(Constants.PROPERTIESFILE);
			props.load(in);
	
			this.RMIRegistryIP = props.getProperty("RMIRegistryIP");
			this.RMIRegistryPort = Integer.parseInt(props.getProperty("RMIRegistryPort"));

			this.RMIRegistryParentIP = props.getProperty("RMIRegistryParentIP");
			
			String tmpp = props.getProperty("ParentID");
			if(tmpp!=null)
				this.ParentID = Integer.parseInt(tmpp);

			if (this.RMIRegistryParentIP != null)
				this.RMIRegistryParentPort = Integer.parseInt(props.getProperty("RMIRegistryParentPort"));
			
			switch(props.getProperty("Role")) {
			case "ROOTSERVER": 
				this.role = Role.ROOTSERVER;
				break;
			case "SERVER":
				this.role = Role.SERVER;
				break;
			default:
				this.role = Role.CLIENT;	
			}
			
			this.CassandraPath = props.getProperty("CassandraPath");
			
			this.CassandraIP = props.getProperty("CassandraIP");
			
			String temp = props.getProperty("CassandraPort");
			this.CassandraPort = temp != null? Integer.parseInt(temp) : 9042;
			
			temp = props.getProperty("CassandraParentIP");
			this.CassandraParentIP = temp !=null ? props.getProperty("CassandraParentIP"): "127.0.0.1";

			temp = props.getProperty("CassandraParentPort");
			this.CassandraParentPort = temp != null? Integer.parseInt(temp) : 9062;
			
			temp = props.getProperty("MaxBatchSize");
			this.MaxBatchSize = temp != null? Integer.parseInt(temp) : MaxBatchSize;
			
			temp = props.getProperty("PullSleep");
			this.PullSleep = temp != null? Integer.parseInt(temp) : 1000;
			
			temp = props.getProperty("PushSleep");
			this.PushSleep = temp != null? Integer.parseInt(temp) : 1000;
			
			temp = props.getProperty("NodeID");
			this.NodeID = temp != null? Integer.parseInt(temp) : 1;
			
			
			in.close();
		}
		catch(Exception ex) {
			// TODO log exception 
		}
	}

}

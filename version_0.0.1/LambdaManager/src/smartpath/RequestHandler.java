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
package smartpath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.net.InetAddresses;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pathstore.util.ApplicationSchema;

public class RequestHandler implements HttpHandler {
	Session session;
	List<Integer> installedFunctions;
	Configuration config;

	public RequestHandler(Session session, List<Integer> installedFunctions, Configuration config) {
		this.session = session;
		this.installedFunctions=installedFunctions;
		this.config=config;
	}

	@Override //Tested
	public void handle(HttpExchange httpReq) throws IOException {

		long d = System.nanoTime();
		Map <String,String>parms = queryToMap(httpReq.getRequestURI().getQuery());
		String funcIP = parms.get("funcId");
		String domain = parms.get("domain");
		System.out.println("handling request...");
		
		String response = "ok";
		if(funcIP!=null)
		{

			InetAddress.getByName(funcIP);
			int funcid = Util.getFuncIDFromIP(InetAddress.getByName(funcIP));
			System.out.println("funcId= " + funcid);

			FuncEntry funcEntry = new FuncEntry(session, funcid);
			

			if(Util.locationComparison(funcEntry.deploy_strategy,LambdaManager.LambdaManagerLOCATION))
			{
				System.out.println("function should be running on this node ...");
				//TODO this should be done using a thread pool
				AppEntry appEntry = new AppEntry(session, funcEntry.appId);
				ArrayList<FuncEntry> fentries = appEntry.getFuncEntries(session);
				RequestProcessor rp = new RequestProcessor(installedFunctions, config,  appEntry,fentries);
				System.out.println( "totaltime: " + (System.nanoTime()-d)/1000000.0);

				rp.start();
			}
			else
			{
				response = "fail - function should not be running here";
			}
		}
		else if(domain!=null)
		{
			String[] tokens = domain.split(Pattern.quote("."));
			String root_domain = tokens[tokens.length-3].toLowerCase();
			System.out.println("domain requestsed ...");
			
			AppEntry appEntry = new AppEntry(session, root_domain);
			
			ArrayList<FuncEntry> fentries = appEntry.getFuncEntries(session);

			for(FuncEntry funcEntry: fentries)
			{
				if(funcEntry.deploy_strategy.equals(LambdaManager.LambdaManagerLOCATION))
				{	
					//TODO this should be done using  a thread pool
					System.out.println("new request: " + funcEntry.funcId);
					RequestProcessor rp = new RequestProcessor(installedFunctions, config, appEntry, fentries);
					System.out.println( "totaltime: " + (System.nanoTime()-d)/1000000.0);
					rp.start();
					break;
				}
			}

		}


		httpReq.sendResponseHeaders(200, response.length());
		OutputStream os = httpReq.getResponseBody();
		os.write(response.getBytes());
		os.close();

	}

	public static Map<String, String> queryToMap(String query){
		Map<String, String> result = new HashMap<String, String>();
		for (String param : query.split("&")) {
			String pair[] = param.split("=");
			if (pair.length>1) {
				result.put(pair[0], pair[1]);
			}else{
				result.put(pair[0], "");
			}
		}
		return result;
	}


	int IpToInteger(byte[] bytes, int len) {
		int val = 0;
		for (int i = 0; i < 3; i++) {
			val <<= 8;
			val |= bytes[i] & 0xff;
		}
		return val;
	}


	byte[] unpack(int bytes) {
		return new byte[] {
				(byte)((bytes >>> 24) & 0xff),
				(byte)((bytes >>> 16) & 0xff),
				(byte)((bytes >>>  8) & 0xff),
				(byte)((bytes       ) & 0xff)
		};
	}
}


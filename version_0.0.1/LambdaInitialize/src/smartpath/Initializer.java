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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pathstore.client.PathStoreCluster;
import pathstore.util.ApplicationSchema;

public class Initializer {

	public static String CassandraKEYSPACE = "pathstore_applications";
	public static String CassandraIP = "localhost";
	public static String RmiIP = "localhost";
	public static int RmiPort = 1099;
	public static int CassandraPort = 9052;
	public static String CODE_DIR ="/tmp/code/";
	public static String DNSSERVERIP = "10.70.20.154";
	public static String InitializerIP = "10.70.20.154";
	public static OkHttpClient client;
	public Configuration config;


	AtomicInteger nextAppId; 
	AtomicInteger nextFuncId; 

	Session session;
	//Cluster cluster;
	PathStoreCluster cluster;

	public Initializer()
	{
		client = new OkHttpClient();
		initConfiguration();
		initServer();
		ApplicationSchema.setParameters(CassandraIP, CassandraPort, RmiIP, RmiPort, "ROOTSERVER");
		initDB();
		initDNS();
		nextAppId = new AtomicInteger(0);
		nextFuncId = new AtomicInteger(1);
		
	}

	private void initDNS() {
		// TODO Auto-generated method stub
		String s = "{\"name\":\"lambdascale.com.\", \"kind\": \"Native\", \"masters\": [], \"nameservers\": [\"ns1.lambdascale.com.\", \"ns2.lambdascale.com.\"]}";

		RequestBody body = RequestBody.create( MediaType.parse("text/plain"),s);
		Request request = new Request.Builder()
				.url("http://"+DNSSERVERIP+":8081/api/v1/servers/localhost/zones")
				.addHeader("X-API-Key", "changeme")
				.post(body).build();

		Response response;
		try {
			response = client.newCall(request).execute();
			response.close();
			System.out.println(s+ " " +request);
			System.out.println(response);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	private void initDB()
	{
//		cluster = Cluster.builder() 
//				.addContactPoint(CassandraIP) 
//				.withPort(9042).build();
        cluster = PathStoreCluster.getInstance();

		session = cluster.connect();
		//session.execute("use " + CassandraKEYSPACE);
	}

	private void initConfiguration() {
		try {
			config = new PropertiesConfiguration("config.properties");

		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		CassandraIP = config.getString("Cassandra.ip");
		CassandraPort = Integer.parseInt(config.getString("Cassandra.port"));
		RmiIP = config.getString("Rmi.ip");
		RmiPort = Integer.parseInt(config.getString("Rmi.port"));
		
		CassandraKEYSPACE = config.getString("Cassandra.keyspace");
		CODE_DIR = config.getString("Initializer.code_location");
		DNSSERVERIP= config.getString("Dns.ip");
		InitializerIP = config.getString("Initializer.ip");
		System.out.println("INTIALIZER IP: "+ InitializerIP);
		
		File dest = new File(CODE_DIR);
		if(!dest.exists())
			dest.mkdir();
	}

	public void initServer()
	{
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(8000), 0);
			server.createContext("/fileupload", new MyHandler());
			server.createContext("/", new MyHandler2());
			server.setExecutor(null); // creates a default executor
			server.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public class MyHandler implements HttpHandler {

		@Override
		public void handle(final HttpExchange t) throws IOException {
			for(Entry<String, List<String>> header : t.getRequestHeaders().entrySet()) {
				System.out.println(header.getKey() + ": " + header.getValue().get(0));
			}

			DiskFileItemFactory d = new DiskFileItemFactory();      

			try {
				ServletFileUpload up = new ServletFileUpload(d);
				List<FileItem> result = up.parseRequest(new RequestContext() {

					@Override
					public String getCharacterEncoding() {
						return "UTF-8";
					}

					@Override
					public int getContentLength() {
						return 0; //tested to work with 0 as return
					}

					@Override
					public String getContentType() {
						return t.getRequestHeaders().getFirst("Content-type");
					}

					@Override
					public InputStream getInputStream() throws IOException {
						return t.getRequestBody();
					}

				});
				//t.getResponseHeaders().add("Content-type", "text/html");
				t.sendResponseHeaders(200, 0);
				OutputStream os = t.getResponseBody();

				String owner="";
				String appName="";
				ByteBuffer code = null;

				for(FileItem fi : result) {
					System.out.println(fi.getContentType());
					if(!fi.isFormField())
					{
						os.write(fi.getName().getBytes());
						os.write("\r\n".getBytes());
						System.out.println("File-Item: " + fi.getFieldName() + " = " + fi.getName());
						code = ByteBuffer.wrap(fi.get());
					}
					else
					{
						System.out.println(fi.getFieldName() + fi.getString() );
						if(fi.getFieldName().equals("owner"))
						{
							owner = fi.getString();
						}
						else if(fi.getFieldName().equals("appname"))
						{
							appName=fi.getString().toLowerCase();
						}
					}
				}
				os.close();
				int appId = nextAppId.incrementAndGet();
				writeCodeToFile(appId, code);
				String schema = Util.readFileContents(CODE_DIR+appId+"/WEB-INF/schema.cql");
				String augment = initializeCassandraForApp(appId, appName, schema);
				System.out.println("augmented: " + augment);
				ArrayList<Integer> funcs = createFuncsEntry(appId, appName);
				createAppsEntry(appId, owner,appName,code, funcs, schema,augment);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private String initializeCassandraForApp(int appId, String appName, String schema) {
			
			try {
				return ApplicationSchema.importApplicationUsingSchemaString(appId, appName, appName, schema);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
		

		public ArrayList<Integer> createFuncsEntry(int appId, String appName)
		{

			ArrayList<Integer> funcs = new ArrayList<>();
			try {

				File fXmlFile = new File(CODE_DIR + appId + "/WEB-INF/web.xml");
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(fXmlFile);

				//optional, but recommended
				//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
				doc.getDocumentElement().normalize();

				System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

				NodeList nList = doc.getElementsByTagName("servlet-mapping");

				System.out.println("----------------------------");

				for (int temp = 0; temp < nList.getLength(); temp++) {

					Node nNode = nList.item(temp);

					System.out.println("\nCurrent Element :" + nNode.getNodeName());

					if (nNode.getNodeType() == Node.ELEMENT_NODE) {

						Element eElement = (Element) nNode;
						String path = eElement.getElementsByTagName("url-pattern").item(0).getTextContent();
						String preference = eElement.getElementsByTagName("loc-pref").item(0).getTextContent();
						String subDomain = eElement.getElementsByTagName("sub-domain").item(0).getTextContent().toLowerCase();
						int id = nextFuncId.incrementAndGet();
						Statement stmt = QueryBuilder.insertInto(CassandraKEYSPACE,"funcs").value("funcid", id)
								.value("appid", appId)
								.value("deploy_strategy", preference)
								.value("path", path)
								.value("sub_domain", subDomain);
						session.execute(stmt);
						funcs.add(id);
						writeToDNS(id, preference, subDomain, appName);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return funcs;
		}


	}

	public void writeToDNS(int funcId, String pref, String subDomain, String appName) {
		// TODO Auto-generated method stub
		//String DNSSERVER = "127.0.0.1";
		try {
			String ip = Util.encodeFuncIDtoIP(funcId, pref).getHostAddress();
			//PDNS Version 3
//			String s="{\"rrsets\": "
//					+ "[ {\"name\": \""+subDomain+"."+appName+".lambdascale.com\""
//					+ ", \"type\": \"A\", \"changetype\": \"REPLACE\""
//					+ ", \"records\": [ {\"content\": \""+ip+"\""
//					+ ", \"disabled\": false, \"name\": \""+subDomain+"."+appName+".lambdascale.com\""
//					+ ", \"ttl\": 86400, \"type\": \"A\", \"priority\": 0 } ] } ] }";
			
			
			String s = "{\"rrsets\": "
					+ "[ {\"name\": \""+subDomain+"."+appName+".lambdascale.com.\""
					+", \"type\": \"A\", \"ttl\": 86400, \"changetype\": \"REPLACE\","
					+" \"records\": [ {\"content\": \""+ip+"\""
					+", \"disabled\": false } ] } ] }";
			
			System.out.println("s: " + s );

			RequestBody body = RequestBody.create( MediaType.parse("text/plain"),s);

			//PDNS VERSION 3
//			Request request = new Request.Builder()
//					.url("http://"+DNSSERVERIP+":8081/servers/localhost/zones/lambdascale.com")
//					.addHeader("X-API-Key", "changeme")
//					.patch(body).build();
			
			Request request = new Request.Builder()
					.url("http://"+DNSSERVERIP+":8081/api/v1/servers/localhost/zones/lambdascale.com")
					.addHeader("X-API-Key", "changeme")
					.patch(body).build();


			Response response;
			try {
				response = client.newCall(request).execute();
				System.out.println(response);
				response.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	String pathToSubDomain(String str)
	{
		List<String> elephantList = new ArrayList<String>(Arrays.asList(str.split("/")));
		//Collections.reverse(elephantList);
		//elephantList.removeAll(Collections.singleton(null));

		StringBuilder sb = new StringBuilder();
		for (int i=elephantList.size()-1;i>-1;i--) {
			String a = elephantList.get(i);
			if (a.equals(""))
			{
				elephantList.remove(i);
			}
			else
			{
				sb.append(a);
				sb.append('.');
			}
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	int createAppsEntry(int id, String owner, String appName, ByteBuffer code, ArrayList<Integer> funcs, String schema, String augment) {
		// TODO Auto-generated method stub
		Statement stmt = QueryBuilder.insertInto(CassandraKEYSPACE,"apps").value("appid", id)
				.value("owner", owner)
				.value("root_domain", appName)
				.value("code", code)
				.value("funcs",funcs)
				.value("app_schema",schema)
				.value("app_name", appName)
				.value("app_schema_augmented", augment)
				.value("schema_name", appName);
		session.execute(stmt);
		return id;
	}

	public void writeCodeToFile(int appId, ByteBuffer code)
	{
		File dir = new File(CODE_DIR+appId);
		if (!dir.exists()){
			dir.mkdir();
		}
		FileChannel out;
		try {
			out = new FileOutputStream(CODE_DIR + appId + "/code.tar.gz").getChannel();
			out.write(code);
			code.position(0);
			Util.uncompressTarGZ(new File(CODE_DIR + appId + "/code.tar.gz"), new File(CODE_DIR + appId));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}



	public class MyHandler2 implements HttpHandler {
		@Override
		public void handle(final HttpExchange t) throws IOException {

			t.getResponseHeaders().add("Content-type", "text/html");
			t.sendResponseHeaders(200, 0);
			OutputStream os = t.getResponseBody();
			os.write(("<html><form action=\"http://"+InitializerIP+":8000/fileupload\" \n enctype=\"multipart/form-data\" method=\"post\">"
					+ "\n <p>"
					+ "Application Name:<br>"
					+ "\n <input type=\"text\" name=\"appname\" size=\"30\"></p>"
					+ "\n <p>"
					+ "\n Application Owner:<br>"
					+ "\n <input type=\"text\" name=\"owner\" size=\"30\"></p>"
					+ "\n <p>Please specify a file, or a set of files:<br>"
					+ "\n <input type=\"file\" name=\"datafile\" size=\"40\">	</p>"
					+ "\n <div>"
					+ "\n <input type=\"submit\" value=\"Deploy Application\">	"
					+ "\n </div></form></html>").getBytes());
			os.close();

		}
	}



	public static void main(String[] args) throws Exception {
		Initializer dep = new Initializer();


	}

}


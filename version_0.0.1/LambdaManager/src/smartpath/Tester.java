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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.processing.Filer;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pathstore.client.PathStoreCluster;

public class Tester implements HttpHandler  {

	public static void main(String[] args)  throws IOException, ArchiveException {
		// TODO Auto-generated method stub
		//				Session session;
		//		        PathStoreCluster cluster = PathStoreCluster.getInstance();
		//				session = cluster.connect();
		////				Select stmt= QueryBuilder.select().from("pathstore_applications","funcs");
		////				stmt.where(QueryBuilder.eq("funcid", 2));
		//				
		//				Select stmt = QueryBuilder.select().all().from("pathstore_applications","funcs");
		//				stmt.where(QueryBuilder.eq("appid", 1));
		//				//stmt.allowFiltering();
		//				
		//				Row fileRow = session.execute(stmt).one();
		//				System.out.println(fileRow.getInt("appId"));
		//				session.execute(stmt);


		//session.execute("CREATE KEYSPACE pathstore_demo WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1 }  AND durable_writes = false");

		//				Statement stmt = QueryBuilder.select().from("pathstore_applications","apps").where(QueryBuilder.eq("appid", 1));
		//				session.execute(stmt);
		//				Row fileRow = session.execute(stmt).one();
		//				List<Integer> funcs = fileRow.getList("funcs", Integer.class);
		//				
		//				System.out.println(funcs);
		//				System.out.println(fileRow.getString("owner"));
		//				List<Integer> l = new ArrayList<Integer>();
		//				l.add(3002);
		//				l.add(3003);
		//				l.add(3004);
		//				//Statement stmt = QueryBuilder.update("apps").with(QueryBuilder.set("funcs", l)).where(QueryBuilder.eq("appid", 1001));
		////				Statement stmt = QueryBuilder.select("funcs").from("apps").where(QueryBuilder.eq("appid", 1001));
		////				session.execute(stmt);
		////				Row fileRow = session.execute(stmt).one();
		////				List<Integer>response = fileRow.getList("funcs", Integer.class);
		//				Statement stmt = QueryBuilder.select("funcs").from("apps").where(QueryBuilder.eq("appid", 1001));
		//				session.execute(stmt);
		//				Row fileRow = session.execute(stmt).one();
		//				List<Integer> funcs = fileRow.getList("funcs", Integer.class);
		//				System.out.println("funcs: " + funcs);
		//				String start = "job \"edge-sample\" {"
		//						+ " datacenters = [\"dc1\"]"
		//						+ "  group \"deployment\" {"
		//						+ "task \"sc\" {"
		//						+ "  driver = \"java\""
		//						+ "  config {"
		//						+ "    jar_path    = \"local/ServletContainer.jar\""
		//						+ "    args = [\"${NOMAD_PORT_http}\", \"local/sample.war\"]  }"
		//						+ " artifact {source=\"http://"+"10.0.0.1"+":8000/code/"+1001+"/WEB-INF/ServletContainer.jar\""
		//						+ " }"
		//						+ " artifact { source=\"http://"+"10.0.0.1"+":8000/code/"+1001+"/WEB-INF/code.war\""
		//						+ " }"
		//						+ " constraint {"
		//						+ " attribute = \"${meta.usage}\""
		//						+ " operator  = \"=\""
		//						+ " value     = \"deploy\""
		//						+ " }";
		//
		//				StringBuilder sb = new StringBuilder(start);
		//				for(int i=0;i<funcs.size();i++)
		//				{
		//					int fid = funcs.get(i);
		//					System.out.println("&&& " + i);
		//					stmt = QueryBuilder.select("deploy_strategy").from("funcs").where(QueryBuilder.eq("funcid", fid));
		//					session.execute(stmt);
		//					fileRow = session.execute(stmt).one();
		//					String answer = fileRow.getString(0);
		//
		//					if(answer.equals(LambdaManager.LOCATION))
		//					{
		//						String middle= "  service {"
		//								+ "port = \"http\"tags = [\""+LambdaManager.LOCATION+":"+Util.encodeFuncIDtoIP(fid, LambdaManager.LOCATION).getHostAddress()+"\"]"
		//								+ "check {type = \"http\""
		//								+ "path = \"/\""
		//								+ "interval = \"5s\""
		//								+ "timeout = \"2s\""
		//								+ "}}";
		//						System.out.println("********** middle: " + middle);
		//						sb.append(middle);
		//					}
		//
		//				}
		//
		//				String end= "resources {"
		//						+ "cpu = 500 "
		//						+ "memory = 400 "
		//						+ "network {"
		//						+ "mbits = 5 "
		//						+ "port http{}}}}}}";
		//				sb.append(end);
		//				System.out.println(sb.toString());

		//		
		//		HttpServer server;
		//		server = HttpServer.create(new InetSocketAddress(8000), 0);
		//		server.createContext("/test", new Tester());
		//		server.setExecutor(null); // creates a default executor
		//		server.start();

		//		Path path = Paths.get("/tmp/test.tar.gz");
		//		byte[] data = Files.readAllBytes(path);
		//		ByteBuffer buffer =ByteBuffer.wrap(data);
		//		System.out.println(bytesToHex(data));
		//		Statement stmt = QueryBuilder
		//				.select("appid")
		//				.from("lambdascale","funcs")
		//				.where(QueryBuilder.eq("funcId", 3002));
		//		Row fileRow = session.execute(stmt).one();
		//		int appId = fileRow.getInt(0);
		//		System.out.println("app id is: " + appId);

		// Statement query = QueryBuilder.update("apps").with(QueryBuilder.set("code", buffer)).where(QueryBuilder.eq("appid", 1001));

		// session.execute( query);


		//		Statement stmt = QueryBuilder
		//				.select("deploy_strategy")
		//				.from("lambdascale","funcs")
		//				.where(QueryBuilder.eq("funcid", 3002));
		//		Row fileRow = session.execute(stmt).one();
		//		String response = fileRow.getString(0);
		//		if(!response.equals("edge"))
		//		{
		//			//packet should now be routed to the core
		//			System.out.println("not called edge");
		//		}
		//		else
		//			System.out.println("called edge");

		//String getResponse = doGetRequest("http://www.vogella.com");
		//System.out.println(getResponse);

		//		long d = System.nanoTime();
		//		uncompressTarGZ(new File("/tmp/1001/code.tar.gz"), new File("/tmp/1001"));
		//		System.out.println((System.nanoTime()-d)/1000000);
		//		
		//	


		//		JsonObject value = Json.createObjectBuilder().add("job", "edge-sample")
		//				.add("datacenters", Json.createArrayBuilder().add("dc1"))
		//				.add("group", Json.createObjectBuilder()
		//						.add("streetAddress", "21 2nd Street")
		//						.add("city", "New York")
		//						.add("state", "NY")
		//						.add("postalCode", "10021"))
		//				.add("phoneNumber", Json.createArrayBuilder()
		//						.add(Json.createObjectBuilder()
		//								.add("type", "home")
		//								.add("number", "212 555-1234"))
		//						.add(Json.createObjectBuilder()
		//								.add("type", "fax")
		//								.add("number", "646 555-4567")))
		//				.build();

		//System.out.println(value.toString());
		//		try {
		//
		//			File fXmlFile = new File("/tmp/input.xml");
		//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		//			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		//			Document doc = dBuilder.parse(fXmlFile);
		//
		//			//optional, but recommended
		//			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		//			doc.getDocumentElement().normalize();
		//
		//			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
		//
		//			NodeList nList = doc.getElementsByTagName("servlet");
		//
		//			System.out.println("----------------------------");
		//
		//			for (int temp = 0; temp < nList.getLength(); temp++) {
		//
		//				Node nNode = nList.item(temp);
		//
		//				System.out.println("\nCurrent Element :" + nNode.getNodeName());
		//
		//				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
		//
		//					Element eElement = (Element) nNode;
		//
		//					System.out.println("preference: " + eElement.getElementsByTagName("preference").item(0).getTextContent());
		////					System.out.println("Nick Name : " + eElement.getElementsByTagName("nickname").item(0).getTextContent());
		////					System.out.println("Salary : " + eElement.getElementsByTagName("salary").item(0).getTextContent());
		//
		//				}
		//			}
		//		} catch (Exception e) {
		//			e.printStackTrace();
		//		}



		//		  try {
		//				PropertiesConfiguration config = new PropertiesConfiguration("config.properties");
		//				System.out.println(config.getString("Cassandra.ip"));
		//			} catch (ConfigurationException e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}


		System.out.println(Util.encodeFuncIDtoIP(3, "core"));
		System.out.println(Util.getFuncIDFromIP(InetAddress.getByName("192.168.241.2")));


	}


	static String doGetRequest(String url) throws IOException {
		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder()
				.url(url)
				.build();

		Response response = client.newCall(request).execute();
		return response.body().string();


		//
		//		 HttpUrl url = chain.request().httpUrl()
		//	                .newBuilder()
		//	                .addQueryParameter("api_key", mApiKey)
		//	                .build();

		//        OkHttpClient client = new OkHttpClient();
		//		HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.github.help").newBuilder();
		//		urlBuilder.addQueryParameter("v", "1.0");
		//		urlBuilder.addQueryParameter("user", "vogella");
		//		String url = urlBuilder.build().toString();
		//
		//		Request request = new Request.Builder()
		//		                     .url(url)
		//		                     .build();
	}



	public static void uncompressTarGZ(File tarFile, File dest) throws IOException {
		if(!dest.exists())
			dest.mkdir();
		TarArchiveInputStream tarIn = null;

		tarIn = new TarArchiveInputStream(
				new GzipCompressorInputStream(
						new BufferedInputStream(
								new FileInputStream(
										tarFile
										)
								)
						)
				);

		TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
		// tarIn is a TarArchiveInputStream
		while (tarEntry != null) {// create a file with the same name as the tarEntry
			File destPath = new File(dest, tarEntry.getName());
			//System.out.println("working: " + destPath.getCanonicalPath());
			if (tarEntry.isDirectory()) {
				destPath.mkdirs();
			} else {
				destPath.createNewFile();
				//byte [] btoRead = new byte[(int)tarEntry.getSize()];
				byte [] btoRead = new byte[1024];
				//FileInputStream fin 
				//  = new FileInputStream(destPath.getCanonicalPath());
				BufferedOutputStream bout = 
						new BufferedOutputStream(new FileOutputStream(destPath));
				int len = 0;

				while((len = tarIn.read(btoRead)) != -1)
				{
					bout.write(btoRead,0,len);
				}

				bout.close();
				btoRead = null;

			}
			tarEntry = tarIn.getNextTarEntry();
		}
		tarIn.close();
	} 





	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}


	@Override
	public void handle(HttpExchange t) throws IOException {
		String root = "/tmp";
		URI uri = t.getRequestURI();
		System.out.println(root + uri.getPath());
		File file = new File(root + uri.getPath()).getCanonicalFile();
		if (!file.getPath().startsWith(root)) {
			// Suspected path traversal attack: reject with 403 error.
			String response = "403 (Forbidden)\n";
			t.sendResponseHeaders(403, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} else if (!file.isFile()) {
			// Object does not exist or is not a file: reject with 404 error.
			String response = "404 (Not Found)\n";
			t.sendResponseHeaders(404, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} else {
			// Object exists and is a file: accept with response code 200.
			t.sendResponseHeaders(200, 0);
			OutputStream os = t.getResponseBody();
			FileInputStream fs = new FileInputStream(file);
			final byte[] buffer = new byte[0x10000];
			int count = 0;
			while ((count = fs.read(buffer)) >= 0) {
				os.write(buffer,0,count);
			}
			fs.close();
			os.close();
		}
	}
}

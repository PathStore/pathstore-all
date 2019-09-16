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

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.sun.net.httpserver.Headers;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Tester {
	public static OkHttpClient client;

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
				Session session;
				Cluster cluster;
				cluster = Cluster.builder() 
						.addContactPoint("10.70.20.154") 
						.withPort(9052).build();
				session = cluster.connect();
				String schema = cluster.getMetadata().getKeyspace("pathstore_demo").exportAsString();
			    System.out.println("schema is: " + schema);	
		//		session.execute("use lambdascale");

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

		//		String str ="/tmp/test/";
		//		List<String> elephantList = new ArrayList<String>(Arrays.asList(str.split("/")));
		//		//Collections.reverse(elephantList);
		//		//elephantList.removeAll(Collections.singleton(null));
		//
		//		StringBuilder sb = new StringBuilder();
		//		for (int i=elephantList.size()-1;i>-1;i--) {
		//			String a = elephantList.get(i);
		//			if (a.equals(""))
		//			{
		//				elephantList.remove(i);
		//			}
		//			else
		//			{
		//				sb.append(a);
		//				sb.append('.');
		//			}
		//		}
		//		sb.setLength(sb.length() - 1);
		//
		//
		//		System.out.println("result" + elephantList + "   "  +sb.toString());

//		int ipAddress = 1234453453;
//		byte[] bytes = BigInteger.valueOf(ipAddress).toByteArray();
//		//		for(int i=0; i<16;i++)
//		//		{
//		//			//System.out.println(((bytes[0] & ~(1 << i))+""));
//		//(b | (1 << 6));
//		//			bytes[1] = (byte) (bytes[1] & ~(1 << i));
//		//		}
//		bytes[0]=0;
//		bytes[1]=0;
//		//bytes[1] = (byte) (bytes[1] | (1 << 6));
//		InetAddress address = InetAddress.getByAddress(bytes);
//		System.out.println(address);
//		System.out.println(byteAToInt(address.getAddress()));
//
//		InetAddress aa = IPcreator(1, "core");
////		//System.out.println(aa.toString().substring(1));
//		System.out.println(aa.getHostAddress());
////		System.out.println(IPdisIntegrate(aa));
//		//client = new OkHttpClient();
//		//writeToDNS(22, "edge", "salam");
//		byte[] b = {0,0};
//		//b[1]=(byte) 192;
//		b[1] = (byte) (b[1] | (1 << 0));
//		
//		System.out.println(b[1]);
//		System.out.println(new BigInteger(b).intValue());

	}

	static public void writeToDNS(int funcId, String pref, String subDomain) {
		// TODO Auto-generated method stub
		String DNSSERVER = "127.0.0.1";
		try {
			String ip = IPcreator(funcId, pref).getHostName();
			String s="{\"rrsets\": "
					+ "[ {\"name\": \""+subDomain+".lambdascale.com\""
					+ ", \"type\": \"A\", \"changetype\": \"REPLACE\""
					+ ", \"records\": [ {\"content\": \""+ip+"\""
					+ ", \"disabled\": false, \"name\": \""+subDomain+".lambdascale.com\""
					+ ", \"ttl\": 86400, \"type\": \"A\", \"priority\": 0 } ] } ] }";

			RequestBody body = RequestBody.create( MediaType.parse("text/plain"),s);
			Request request = new Request.Builder()
					.url("http://"+DNSSERVER+":8081/servers/localhost/zones/lambdascale.com")
					.addHeader("X-API-Key", "changeme")
					.patch(body).build();

			Response response;
			try {
				response = client.newCall(request).execute();
				System.out.println(response);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	static InetAddress IPcreator(int funcId, String preference) throws UnknownHostException
	{
		//		byte[] bytes={0,0,0,0};
		//		byte[] bytess = BigInteger.valueOf(funcId).toByteArray();
		//		for(int i=0;i<bytess.length;i++)
		//		{
		//			int off = 4-bytess.length;
		//			bytes[i+off]=bytess[i];
		//		}
		byte[] bytes = intToByteA(funcId);

		switch(preference)
		{
		case "edge":
			bytes[0]=10;
			bytes[1] = (byte) (bytes[1] | (1 << 6));
			break;

		case "core":
			bytes[0]=10;
			bytes[1] = (byte) (bytes[1] | (1 << 7));
			break;

		case "cloud":
			bytes[0]=100;
			bytes[1]=111;
			break;
		}		
		InetAddress toReturn =InetAddress.getByAddress(bytes); 
		return toReturn;
	}

	static int IPdisIntegrate(InetAddress funcId)
	{

		byte[] bytes = funcId.getAddress();
		bytes[0]=0;
		bytes[1] = (byte) (bytes[1] & ~(1 << 6));
		bytes[1] = (byte) (bytes[1] & ~(1 << 7));
		bytes[1] = (byte) (bytes[1] & ~(1 << 8));

		return byteAToInt(bytes);
	}

	static int byteAToInt(byte[] bytes) {
		int val = 0;
		for (int i = 0; i < bytes.length; i++) {
			val <<= 8;
			val |= bytes[i] & 0xff;
		}
		return val;
	}
	
	static byte[] intToByteA(int bytes) {
		return new byte[] {
				(byte)((bytes >>> 24) & 0xff),
				(byte)((bytes >>> 16) & 0xff),
				(byte)((bytes >>>  8) & 0xff),
				(byte)((bytes       ) & 0xff)
		};
	}


	int IpToInteger(byte[] bytes, int len) {
		int val = 0;
		for (int i = 0; i < 3; i++) {
			val <<= 8;
			val |= bytes[i] & 0xff;
		}
		return val;
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
}

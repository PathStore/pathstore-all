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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.configuration.Configuration;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pathstore.util.ApplicationSchema;

public class RequestProcessor extends Thread {

	public static String CODE_DIR ="/tmp/code/";
	public static String NomadUrl = "nomad";
	public static OkHttpClient client;
	public static String DEFAULTIP="192.168.224.1";
	public static String checkInitialStatus = "";
	public static long checkInterval = 50000000;

	//Session session;
	List<Integer> installedFunctions;
	Configuration config;
	AppEntry appEntry;
	ArrayList<FuncEntry> funcEntries;

	public RequestProcessor(List<Integer> installedFunctions, Configuration config, AppEntry appEntry, ArrayList<FuncEntry> fentries)
	{
		//this.session = session;
		this.installedFunctions=installedFunctions;
		client = new OkHttpClient();
		this.config=config;
		this.appEntry=appEntry;
		this.funcEntries=fentries;

		NomadUrl = config.getString("Nomad.url");
		CODE_DIR = config.getString("LambdaManager.code_location");
		DEFAULTIP = config.getString("DNS.DefaultIP");
		checkInitialStatus = config.getString("Nomad.checkInitialStatus", checkInitialStatus);
		checkInterval = config.getLong("Nomad.checkInterval", checkInterval);
		File dest = new File(CODE_DIR);
		if(!dest.exists())
			dest.mkdir();
	}

	public void run()
	{
		//SALEHE: start nomad here pass code to it

		int appid = getAndUntarCode();
		long d = System.nanoTime();
		try {
			Runnable schemawriter =  () -> {
					   ApplicationSchema.getInstance().createAugmentedDB(appEntry.appId);
				  };
			schemawriter.run();
			System.out.println( "totaltime2: " + (System.nanoTime()-d)/1000000.0);

			requestNomad(appid);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		for(FuncEntry f:funcEntries)
			installedFunctions.add(f.funcId);
	}




	public void writeBuffer(ByteBuffer buffer, OutputStream stream) {
		WritableByteChannel channel = Channels.newChannel(stream);

		try {
			channel.write(buffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getAndUntarCode()
	{


		try {
			File dir = new File(CODE_DIR+appEntry.appId);
			System.out.println("creating directory: " +CODE_DIR+appEntry.appId );
			if (!dir.exists()){
				dir.mkdir();
			}
			FileChannel out = new FileOutputStream(CODE_DIR + appEntry.appId + "/code.tar.gz").getChannel();
			out.write(appEntry.code);
			Util.uncompressTarGZ(new File(CODE_DIR + appEntry.appId + "/code.tar.gz"), new File(CODE_DIR + appEntry.appId));
			return appEntry.appId;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return -1;
	}



	public void requestNomad(int appid) throws UnknownHostException
	{

		System.out.println("Sending Request to Nomad appid: " + appid);
		String domain = appEntry.root_domain;
		domain+=".lambdascale.com";
		String check_path = UUID.randomUUID().toString();

		StringBuilder sb = new StringBuilder();
		StringBuilder subDomains = new StringBuilder();

		//DEfAULT DOMAIN
		sb.append(
				"                            {\r\n" +
						"                                \"Id\": \"\",\r\n" +
						"                                \"Name\": \"lambdaapp-" + appid + "-deployment-sc\",\r\n" +
						"                                \"Tags\": [\r\n" +
						"                                    \"lambdaapp:" + domain + "\"\r\n" +
						"                                ],\r\n" +
						"                                \"PortLabel\": \"http\",\r\n" +
						"                                \"Checks\": [\r\n" +
						"                                    {\r\n" +
						"                                        \"Id\": \"\",\r\n" +
						"                                        \"Name\": \"service: \\\"lambdaapp-" + appid + "-deployment-sc\\\" check\",\r\n" +
						"                                        \"Type\": \"http\",\r\n" +
						"                                        \"Command\": \"\",\r\n" +
						"                                        \"Args\": null,\r\n" +
						"                                        \"Path\": \"/" + check_path + "\",\r\n" +
						"                                        \"Protocol\": \"\",\r\n" +
						"                                        \"PortLabel\": \"\",\r\n" +
						"                                        \"Interval\": " + checkInterval + ",\r\n" +
						"                                        \"Timeout\": 2000000000,\r\n" +
						"                                        \"InitialStatus\": \"" + checkInitialStatus + "\"\r\n" +
						"                                    }\r\n" +
						"                                ]\r\n" +
				"                            }\r\n");



		for (FuncEntry fEntry: funcEntries) {
			//int fid = funcs.get(i);

			int fid = fEntry.funcId;
			String deployStrategy = fEntry.deploy_strategy;


			if(Util.locationComparison(deployStrategy,LambdaManager.LambdaManagerLOCATION))
			{
				subDomains.append(fEntry.sub_domain);
				subDomains.append('|');
				subDomains.append(fEntry.path);
				subDomains.append('|');

				if (sb.length() > 0)
					sb.append(",\n");
				sb.append(
						"                            {\r\n" +
								"                                \"Id\": \"\",\r\n" +
								"                                \"Name\": \"lambdafunc-" + appid + "-" + fid+"-deployment-sc\",\r\n" +
								"                                \"Tags\": [\r\n" +
								"                                    \"lambdafunc:" + Util.encodeFuncIDtoIP(fid, LambdaManager.LambdaManagerLOCATION).getHostAddress() + "\"\r\n" +
								"                                ],\r\n" +
								"                                \"PortLabel\": \"http\",\r\n" +
								"                                \"Checks\": [\r\n" +
								"                                    {\r\n" +
								"                                        \"Id\": \"\",\r\n" +
								"                                        \"Name\": \"service: \\\"lambdafunc-" + appid + "-" + fid+ "-deployment-sc\\\" check\",\r\n" +
								"                                        \"Type\": \"http\",\r\n" +
								"                                        \"Command\": \"\",\r\n" +
								"                                        \"Args\": null,\r\n" +
								"                                        \"Path\": \"/" + check_path + "\",\r\n" +
								"                                        \"Protocol\": \"\",\r\n" +
								"                                        \"PortLabel\": \"\",\r\n" +
								"                                        \"Interval\": " + checkInterval + ",\r\n" +
								"                                        \"Timeout\": 2000000000,\r\n" +
								"                                        \"InitialStatus\": \"" + checkInitialStatus + "\"\r\n" +
								"                                    }\r\n" +
								"                                ]\r\n" +
						"                            }\r\n");
			}
		}


		//get rid of last |
		subDomains.setLength(subDomains.length() - 1);


		String nomadJob = 
				"{\r\n" +
						"    \"Job\": {\r\n" +
						"        \"Region\": \"global\",\r\n" +
						"        \"ID\": \"lambda-" + appid + "\",\r\n" +
						"        \"ParentID\": \"\",\r\n" +
						"        \"Name\": \"lambda-" + appid + "\",\r\n" +
						"        \"Type\": \"service\",\r\n" +
						"        \"Priority\": 50,\r\n" +
						"        \"AllAtOnce\": false,\r\n" +
						"        \"Datacenters\": [\r\n" +
						"            \"dc1\"\r\n" +
						"        ],\r\n" +
						"        \"Constraints\": null,\r\n" +
						"        \"TaskGroups\": [\r\n" +
						"            {\r\n" +
						"                \"Name\": \"deployment\",\r\n" +
						"                \"Count\": 1,\r\n" +
						"                \"Constraints\": null,\r\n" +
						"                \"Tasks\": [\r\n" +
						"                    {\r\n" +
						"                        \"Name\": \"sc\",\r\n" +
						"                        \"Driver\": \"raw_exec\",\r\n" +
						"                        \"User\": \"\",\r\n" +
						"                        \"Config\": {\r\n" +
						"                            \"args\": [\r\n" +
						"                                \"run\",\r\n" +
						"                                \"--net=host\",\r\n" +
						"                                \"--volume=execs,kind=host,source=${NOMAD_TASK_DIR}\",\r\n" +
						"                                \"--mount=volume=execs,target=/var/execs\",\r\n" +
						"                                \"quay.io/pires/docker-jre:8u112_1\",\r\n" +
						"                                \"--exec=java\",\r\n" +
						"                                \"--\",\r\n" +
						"                                \"-jar\",\r\n" +
						"                                \"/var/execs/ServletContainer.jar\",\r\n" +
						"                                \"${NOMAD_PORT_http}\",\r\n" +
						"                                \"/var/execs/application.war\",\r\n" +
						"                                \"" + domain + "\",\r\n" +
						"                                \"" + subDomains + "\",\r\n" +
						"                                \"" + check_path + "\"\r\n" +
						"                            ],\r\n" +
						"                            \"command\": \"/usr/bin/rkt\"\r\n" +
						"                        },\r\n" +
						"                        \"Constraints\": [\r\n" +
						"                            {\r\n" +
						"                                \"LTarget\": \"${meta.usage}\",\r\n" +
						"                                \"RTarget\": \"deploy\",\r\n" +
						"                                \"Operand\": \"=\"\r\n" +
						"                            }\r\n" +
						"                        ],\r\n" +
						"                        \"Env\": null,\r\n" +
						"                        \"Services\": [\r\n" +
						sb.toString()+
						"                        ],\r\n" +
						"                        \"Resources\": {\r\n" +
						"                            \"CPU\": 500,\r\n" +
						"                            \"MemoryMB\": 400,\r\n" +
						"                            \"DiskMB\": 0,\r\n" +
						"                            \"IOPS\": 0,\r\n" +
						"                            \"Networks\": [\r\n" +
						"                                {\r\n" +
						"                                    \"Public\": false,\r\n" +
						"                                    \"CIDR\": \"\",\r\n" +
						"                                    \"ReservedPorts\": null,\r\n" +
						"                                    \"DynamicPorts\": [\r\n" +
						"                                        {\r\n" +
						"                                            \"Label\": \"http\",\r\n" +
						"                                            \"Value\": 0\r\n" +
						"                                        }\r\n" +
						"                                    ],\r\n" +
						"                                    \"IP\": \"\",\r\n" +
						"                                    \"MBits\": 5\r\n" +
						"                                }\r\n" +
						"                            ]\r\n" +
						"                        },\r\n" +
						"                        \"Meta\": null,\r\n" +
						"                        \"KillTimeout\": 5000000000,\r\n" +
						"                        \"LogConfig\": {\r\n" +
						"                            \"MaxFiles\": 10,\r\n" +
						"                            \"MaxFileSizeMB\": 10\r\n" +
						"                        },\r\n" +
						"                        \"Artifacts\": [\r\n" +
						"                            {\r\n" +
						"                                \"GetterSource\": \"http://" + LambdaManager.LambdaManagerIP + ":8000/code/ServletContainer.jar\",\r\n" +
						"                                \"GetterOptions\": null,\r\n" +
						"                                \"RelativeDest\": \"local/\"\r\n" +
						"                            },\r\n" +
						"                            {\r\n" +
						"                                \"GetterSource\": \"http://" + LambdaManager.LambdaManagerIP + ":8000/code/" + appid + "/WEB-INF/application.war\",\r\n" +
						"                                \"GetterOptions\": null,\r\n" +
						"                                \"RelativeDest\": \"local/\"\r\n" +
						"                            }\r\n" +
						"                        ],\r\n" +
						"                        \"Vault\": null,\r\n" +
						"                        \"Templates\": null,\r\n" +
						"                        \"DispatchPayload\": null\r\n" +
						"                    }\r\n" +
						"                ],\r\n" +
						"                \"RestartPolicy\": {\r\n" +
						"                    \"Interval\": 60000000000,\r\n" +
						"                    \"Attempts\": 2,\r\n" +
						"                    \"Delay\": 15000000000,\r\n" +
						"                    \"Mode\": \"delay\"\r\n" +
						"                },\r\n" +
						"                \"EphemeralDisk\": {\r\n" +
						"                    \"Sticky\": false,\r\n" +
						"                    \"Migrate\": false,\r\n" +
						"                    \"SizeMB\": 300\r\n" +
						"                },\r\n" +
						"                \"Meta\": null\r\n" +
						"            }\r\n" +
						"        ],\r\n" +
						"        \"Update\": {\r\n" +
						"            \"Stagger\": 0,\r\n" +
						"            \"MaxParallel\": 0\r\n" +
						"        },\r\n" +
						"        \"Periodic\": null,\r\n" +
						"        \"ParameterizedJob\": null,\r\n" +
						"        \"Payload\": null,\r\n" +
						"        \"Meta\": null,\r\n" +
						"        \"VaultToken\": \"\",\r\n" +
						"        \"Status\": \"\",\r\n" +
						"        \"StatusDescription\": \"\",\r\n" +
						"        \"CreateIndex\": 0,\r\n" +
						"        \"ModifyIndex\": 0,\r\n" +
						"        \"JobModifyIndex\": 0\r\n" +
						"    }\r\n" +
						"}\r\n";


		System.out.println(nomadJob);
		RequestBody body = RequestBody.create( MediaType.parse("application/json"),nomadJob);
		Request request = new Request.Builder().url(NomadUrl).post(body).build();

		Response response;
		try {
			response = client.newCall(request).execute();
			response.body().string();
			response.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}







	//    ByteBuffer5 fileByteBuffer = ByteBuffer.wrap( readFileToByteArray( filename ) );
	//    Statement insertFile = QueryBuilder.insertInto( "files" ).value( "filename", filename ).value( "file", fileByteBuffer );
	//    session.execute( insertFile );

}

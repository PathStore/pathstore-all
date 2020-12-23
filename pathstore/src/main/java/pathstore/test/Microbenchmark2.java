///**********
//*
//* Copyright 2019 Eyal de Lara, Seyed Hossein Mortazavi, Mohammad Salehe
//*
//* Licensed under the Apache License, Version 2.0 (the "License");
//* you may not use this file except in compliance with the License.
//* You may obtain a copy of the License at
//*
//*    http://www.apache.org/licenses/LICENSE-2.0
//*
//* Unless required by applicable law or agreed to in writing, software
//* distributed under the License is distributed on an "AS IS" BASIS,
//* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//* See the License for the specific language governing permissions and
//* limitations under the License.
//*
//***********/
//package pathstore.test;
//
//import com.datastax.driver.core.ResultSet;
//import com.datastax.driver.core.Row;
//import com.datastax.driver.core.Session;
//import com.datastax.driver.core.querybuilder.Insert;
//import com.datastax.driver.core.querybuilder.QueryBuilder;
//import com.datastax.driver.core.querybuilder.Select;
//
//import pathstore.client.PathStoreCluster;
//import pathstore.common.PathStoreProperties;
//
//public class Microbenchmark2 {
//
//
//	static public void main(String args[]) throws InterruptedException {
//		//parseCommandLineArguments(args);
//
//
//
//		PathStoreProperties p1 = new PathStoreProperties();
//		p1.CassandraIP="10.70.20.156";
//		p1.GRPCIP =p1.CassandraIP;
//		PathStoreCluster cluster = new PathStoreCluster(p1);
//		Session session = cluster.connect();
//
//		PathStoreProperties p2 = new PathStoreProperties();
//		p2.CassandraIP="10.70.20.154";
//		p2.GRPCIP =p2.CassandraIP;
//
//		PathStoreCluster cluster2 = new PathStoreCluster(p2);
//		Session session2 = cluster2.connect();
//
//		//		Cluster cluster2 = Cluster.builder().addContactPoint("10.70.20.154").withPort(9052).build();
//		//		Session session2 = cluster2.connect();
//
//		int lvl=5;
//
//		int count =1024;
//		StringBuilder sb = new StringBuilder(count);
//		for( int i=0; i<count/2; i++ ) {
//			sb.append("*");
//		}
//
//		for(int i=0;i<lvl;i++)
//		{
//			String tmp = i+"";
//			sb.append(sb.toString());
//			String output = sb.toString();
//			System.out.println("size is : " + output.length());
//			Insert insert = QueryBuilder.insertInto("pathstore_demo", "users");
//			insert.value("name", tmp);
//			insert.value("sport", sb.toString());
//			session2.execute(insert);
//		}
//
//		Thread.sleep(3000);
//
//		//
//		long d= System.nanoTime();
//
//		for(int i=0;i<lvl;i++)
//		{
//
//			Select select = QueryBuilder.select("sport").from("pathstore_demo", "users");
//			//select.allowFiltering();
//			String tmp = i+"";
//			select.where(QueryBuilder.eq("name", tmp));
//			try {
//				while(true) {
//					ResultSet results = session.execute(select);
//					Row r = results.one();
//					if(r !=null)
//					{
//						//Row row = results.one();
//						//if(row.getString("spo").equals(i))
//						//{
//							System.out.println((i*4096)+ "time took: " + (System.nanoTime()-d)/1000000.0);
//							d= System.nanoTime();
//							break;
//						//}
//					}
//				}
//			}
//			catch(Exception ex) {
//				ex.printStackTrace();
//			}
//		}
//		//		session.close();
//
//		System.exit(0);
//	}
//}

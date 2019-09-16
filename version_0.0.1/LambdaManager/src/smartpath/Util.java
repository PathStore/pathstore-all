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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class Util {
	public static InetAddress encodeFuncIDtoIP(int funcId, String preference) throws UnknownHostException
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
			bytes[0]=(byte) 192;
			bytes[1]=(byte) 168;
			bytes[2] =(byte) (bytes[2] | (1 << 7));
			bytes[2] =(byte) (bytes[2] | (1 << 6));
			bytes[2] =(byte) (bytes[2] | (1 << 5));
			bytes[2] =(byte) (bytes[2] | (1 << 4));
			//bytes[2] =(byte) (bytes[2] | (1 << 3));
			bytes[2] =(byte) (bytes[2] | (1 << 0));
			break;

		case "core":
			//			bytes[0]=10;
			//			bytes[1] = (byte) (bytes[1] | (1 << 7));

			bytes[0]=(byte) 192;
			bytes[1]=(byte) 168;
			bytes[2] =(byte) (bytes[2] | (1 << 7));
			bytes[2] =(byte) (bytes[2] | (1 << 6));
			bytes[2] =(byte) (bytes[2] | (1 << 5));
			bytes[2] =(byte) (bytes[2] | (1 << 4));
			//bytes[2] =(byte) (bytes[2] | (1 << 3));
			bytes[2] =(byte) (bytes[2] | (1 << 1));
			break;

		case "country":
			//			bytes[0]=10;
			//			bytes[1] = (byte) (bytes[1] | (1 << 7));

			bytes[0]=(byte) 192;
			bytes[1]=(byte) 168;
			bytes[2] =(byte) (bytes[2] | (1 << 7));
			bytes[2] =(byte) (bytes[2] | (1 << 6));
			bytes[2] =(byte) (bytes[2] | (1 << 5));
			bytes[2] =(byte) (bytes[2] | (1 << 4));
			//bytes[2] =(byte) (bytes[2] | (1 << 3));

			bytes[2] =(byte) (bytes[2] | (1 << 1));
			bytes[2] =(byte) (bytes[2] | (1 << 0));
			break;

		case "cloud":
			//bytes[0]=100;
			//bytes[1]=111;
			//			bytes[0]=10;
			//			bytes[1] = (byte) (bytes[1] | (1 << 7));

			bytes[0]=(byte) 192;
			bytes[1]=(byte) 168;
			bytes[2] =(byte) (bytes[2] | (1 << 7));
			bytes[2] =(byte) (bytes[2] | (1 << 6));
			bytes[2] =(byte) (bytes[2] | (1 << 5));
			bytes[2] =(byte) (bytes[2] | (1 << 4));
			//bytes[2] =(byte) (bytes[2] | (1 << 3));
			break;
		}		
		InetAddress toReturn =InetAddress.getByAddress(bytes); 
		return toReturn;
	}

	public static int getFuncIDFromIP(InetAddress funcId)
	{

		byte[] bytes = funcId.getAddress();
		//		bytes[0]=0;
		//		bytes[1] = (byte) (bytes[1] & ~(1 << 6));
		//		bytes[1] = (byte) (bytes[1] & ~(1 << 7));
		//		bytes[1] = (byte) (bytes[1] & ~(1 << 8));

		bytes[0]=0;
		bytes[1]=0;
		bytes[2]=0;

		return byteAToInt(bytes);
	}

	public static int byteAToInt(byte[] bytes) {
		int val = 0;
		for (int i = 0; i < bytes.length; i++) {
			val <<= 8;
			val |= bytes[i] & 0xff;
		}
		return val;
	}

	public static byte[] intToByteA(int bytes) {
		return new byte[] {
				(byte)((bytes >>> 24) & 0xff),
				(byte)((bytes >>> 16) & 0xff),
				(byte)((bytes >>>  8) & 0xff),
				(byte)((bytes       ) & 0xff)
		};
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
				String parent = destPath.getParent();
				File parentFolder = new File(parent);
				if(!parentFolder.exists())
					parentFolder.mkdirs();
				
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

	public static boolean locationComparison(String funcLoc, String myLoc)
	{
		if(myLoc.equals("edge") && !funcLoc.equals("edge"))
			return false;
			
		else if(myLoc.equals("core") && funcLoc.equals("cloud"))
			return false;
		return true;
	}


}

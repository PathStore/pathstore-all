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

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class BandwidthCalculator {

	
	public static long getTotalBytes(String stringPath)
	{

		long total=0;

		try {

			ProcessBuilder pb = new ProcessBuilder(stringPath);
			//			pb.redirectOutput(Redirect.INHERIT);
			//			pb.redirectError(Redirect.INHERIT);
			Process process = pb.start();

			//			String[] command = { "/home/hossein/workspaceCloud/mobisys-stats.sh"};         
			//			Process process = Runtime.getRuntime().exec(command);                    
			BufferedReader reader = new BufferedReader(new InputStreamReader(        
					process.getInputStream()));                                          
			String s;                                                                
			while ((s = reader.readLine()) != null) {
				System.out.println(s);
				if(s.equals("INPUT") || s.equals("OUTPUT"))
					continue;
				long p = Long.parseLong(s);
				total+=p;


			}
			process.waitFor();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("tamume " + total);
		return total;
	}
	
}

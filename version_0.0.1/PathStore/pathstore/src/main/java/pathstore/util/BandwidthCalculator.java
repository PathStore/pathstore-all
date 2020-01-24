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

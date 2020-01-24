package pathstore.test;

import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

import pathstore.client.PathStoreCluster;
import pathstore.common.PathStoreProperties;

public class MicrobenchmarkTab3 {


	static double[] data;
	static int size;
	static public void main(String args[]) throws InterruptedException {
		//parseCommandLineArguments(args);

		
		Select select = QueryBuilder.select().from("pathstore_demo", "users");
		
//		Session sessionReader= cluster.connect();
		
		
//		for(int i=0; i<1;i++)
//		{
//			//select.where(QueryBuilder.eq("name", "Test"+i));
//			try{
//				System.out.println("new select:" + select);
//				//sessionReader.execute(select);
//			}
//			catch(Exception e)
//			{
//				e.printStackTrace();
//			}
//			
//		}
		
		PathStoreCluster clusterR = PathStoreCluster.getInstance();
		Session sessionR = clusterR.connect();
		select = QueryBuilder.select().from("pathstore_demo", "users");
		sessionR.execute(select);

		//writer
		PathStoreProperties p1 = new PathStoreProperties();
		p1.CassandraIP="10.70.20.1";
		p1.RMIRegistryIP=p1.CassandraIP;
		PathStoreCluster clusterW = new PathStoreCluster(p1);
		Session sessionWriter = clusterW.connect();

		
//		Cluster clusterReaderNode;
//		clusterReaderNode = Cluster.builder()
//				.addContactPoint("10.70.20.154") 
//				.withPort(9052).build();
//		Session sessionReaderNode = clusterReaderNode.connect();
//		sessionReaderNode.execute(select);

		//		Cluster clusterWriter2;
		//		clusterWriter2 = Cluster.builder() 
		//				.addContactPoint("10.70.20.156") 
		//				.withPort(9052).build();
		//		Session sessionWriter2 = clusterWriter2.connect();

		//int lvl=11;
		int count =1024;
		StringBuilder sb = new StringBuilder(count);
		for( int i=0; i<count; i++ ) {
			sb.append("*"); 
		}

		long sum =0;

		String key = "aa";
		for(int i=0; i<size;i++)
		{
			Insert insert = QueryBuilder.insertInto("pathstore_demo", "users");
			insert.value("name", key+i);
			insert.value("sport", sb.toString());
			System.out.println("Running insert!");
			long d = System.nanoTime();
			sessionWriter.execute(insert);
			
		}
	}
	
	
	
    static double getMean()
    {
        double sum = 0.0;
        for(double a : data)
            sum += a;
        return sum/size;
    }

    static double getVariance()
    {
        double mean = getMean();
        double temp = 0;
        for(double a :data)
            temp += (a-mean)*(a-mean);
        return temp/(size-1);
    }

    static double getStdDev()
    {
        return Math.sqrt(getVariance());
    }
}



// write: cloud ... read: core
//75.526354  125.086484  202.668974  241.862039  205.490135  157.669043  241.644127  255.096712  167.530987  136.617835  247.554834  161.28774  169.777995  151.446092  176.648588  199.0835  228.880438  210.589239  194.417842  224.501913  
//mean : 188.66904355000003
//standard deviation: 36.51866123410059

//W: core R: cloud
//84.927894  70.763903  66.472648  72.528379  69.979371  65.080185  84.296892  63.1303  65.694484  53.42324  66.544923  61.536935  68.648635  68.46224  73.320478  70.197238  63.695595  67.135239  60.280267  59.491323  
//mean : 64.78050844999999
//standard deviation: 5.485203809488419


//W: Edge R: cloud
//126.027813  83.079309  71.315876  74.384768  82.902329  81.30894  88.829489  70.356215  80.464523  88.888627  86.001543  79.745995  80.821252  81.726588  79.765705  87.453532  82.990594  82.457112  79.211827  89.871165  
//mean : 83.88016010000001
//standard deviation: 8.245086126949099


//W: cloud R: edge
//334.360679  213.031281  367.74081  199.06334  248.06695  299.14036  261.673744  266.414151  260.979988  263.961524  269.547878  299.182108  287.358308  275.574244  280.21395  259.538766  292.405458  274.694186  263.167511  341.611161  
//mean : 271.88631984999995
//standard deviation: 39.3072787070955


//W: Edge R: core
//11.0  17.0  14.0  10.0  16.0  21.0  10.0  10.0  9.0  11.0  15.0  9.0  12.0  16.0  8.0  8.0  13.0  12.0  16.0  
// mean : 10.6
//standard deviation: 2.1151900801420165


//W: Core R: Edge
//27.0  25.0001  35.0002  45.0003  50.0004  22.0005  34.0006  45.0007  38.0008  48.0009  53.0002  30.0003  33.0004  45.0005  24.0006  31.0007  50.0008  49.0009  62.001  34.0011  
//mean : 37.00055
//standard deviation: 9.140374183099674

//W: Edge R: Edge In: Core
//52.0  44.0  56.0002  58.0002  55.0004  51.0005  43.0006  33.0007  68.0008  60.0009  71.0002  55.0003  62.0004  53.0005  70.0006  77.0007  53.0008  54.0008  70.001  54.0011  
//mean : 56.950535
//standard deviation: 10.639974950369949


//W: Edge R: Edge In: Core
//249.0  400.0001  307.0002  489.0002  235.0004  530.0005  303.0006  380.0007  544.0008  373.0009  532.0002  287.0003  483.0004  477.0005  558.0006  335.0007  577.0008  580.0009  596.001  218.0011  
//mean : 392.65054499999997
//standard deviation: 107.3018347389771


//W: Edge R: Core (other)
//210.0  228.0  255.0002  200.0003  242.0003  258.0005  272.0006  257.0007  264.0008  290.0004  252.0002  269.0003  299.0004  211.0005  322.0006  294.0003  235.0008  316.0002  251.001  349.0011  
//mean : 263.70046
//standard deviation: 39.136980171382135


//W: Core R: Edge (other)
//203.0  436.0001  426.0002  229.0003  224.0004  459.0005  256.0006  269.0007  305.0008  468.0009  229.0002  289.0003  247.0004  253.0005  260.0006  289.0002  244.0008  261.0009  265.001  301.0011  
//mean : 295.6505250000001
//standard deviation: 82.2162054559541

//W: Core R: Core (other)
//247.0  227.0001  296.0002  209.0003  208.0002  284.0005  312.0006  266.0007  286.0008  280.0009  243.0  285.0002  253.0004  302.0005  190.0006  226.0007  316.0008  196.0009  274.001  318.0011  
//mean : 260.90052499999996
//standard deviation: 40.917088633233284

//W: Edge R: Edge 
//251.0  664.0001  383.0002  430.0003  445.0004  418.0005  413.0006  254.0007  268.0008  256.0009  272.0002  282.0003  484.0004  405.0005  509.0006  268.0007  267.0008  325.0009  274.001  264.0011  
//mean : 356.60055
//standard deviation: 112.80822357151328
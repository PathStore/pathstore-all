package pathstore.common;

public class Timer {

	public static double getTime(long g)
	{
		return (double) ((System.nanoTime()-g)/1000000.0);
	}
}

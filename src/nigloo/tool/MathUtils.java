package nigloo.tool;

public class MathUtils
{
	private MathUtils()
	{
		throw new UnsupportedOperationException();
	}
	
	public static int clamp(int value, int min, int max)
	{
		if (min > max)
			throw new IllegalArgumentException("min (" + min + ") > max (" + max + ")");
		
		return value < min ? min : value > max ? max : value;
	}
	
	public static long clamp(long value, long min, long max)
	{
		if (min > max)
			throw new IllegalArgumentException("min (" + min + ") > max (" + max + ")");
		
		return value < min ? min : value > max ? max : value;
	}
	
	public static float clamp(float value, float min, float max)
	{
		if (min > max)
			throw new IllegalArgumentException("min (" + min + ") > max (" + max + ")");
		
		return value < min ? min : value > max ? max : value;
	}
	
	public static double clamp(double value, double min, double max)
	{
		if (min > max)
			throw new IllegalArgumentException("min (" + min + ") > max (" + max + ")");
		
		return value < min ? min : value > max ? max : value;
	}
}

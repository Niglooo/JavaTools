package nigloo.tool;

import java.io.Closeable;
import java.io.IOException;

public class Utils {

	public Utils() {throw new UnsupportedOperationException();}

	
	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {}
		}
	}
	
	public static boolean isBlank(String s) {
		if (s == null)
			return true;
		
		for (int i = 0 ; i < s.length() ; i++)
			if (!Character.isWhitespace(s.charAt(i)))
				return false;
		
		return true;
	}
	
	public static boolean isNotBlank(String s) {
		return !isBlank(s);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object o)
	{
		return (T) o;
	}
}

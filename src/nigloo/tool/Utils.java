package nigloo.tool;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;

public class Utils
{
	public Utils()
	{
		throw new UnsupportedOperationException();
	}
	
	public static final Comparator<CharSequence> NATURAL_ORDER = new NaturalOrderComparator();
	
	private static class NaturalOrderComparator implements Comparator<CharSequence>
	{
		@Override
		public int compare(CharSequence seq1, CharSequence seq2)
		{
			int len1 = seq1.length();
			int len2 = seq2.length();
			int idx1 = 0;
			int idx2 = 0;
			
			while (true)
			{
				if (idx1 >= len1 && idx2 >= len2)
					return 0;
				else if (idx1 >= len1)
					return -1;
				else if (idx2 >= len2)
					return 1;
				
				char c1 = seq1.charAt(idx1);
				char c2 = seq2.charAt(idx2);
				
				if (!Character.isDigit(c1) || !Character.isDigit(c2))
				{
					int res = compareIgnoreCase(c1, c2);
					if (res != 0)
						return res;
				}
				else
				{
					int digitEnd1 = idx1 + 1;
					while (digitEnd1 < len1 && Character.isDigit(seq1.charAt(digitEnd1)))
						digitEnd1++;
					long number1 = Long.parseLong(seq1, idx1, digitEnd1, 10);
					idx1 = digitEnd1 - 1;
					
					int digitEnd2 = idx2 + 1;
					while (digitEnd2 < len2 && Character.isDigit(seq2.charAt(digitEnd2)))
						digitEnd2++;
					long number2 = Long.parseLong(seq2, idx2, digitEnd2, 10);
					idx2 = digitEnd2 - 1;
					
					if (number1 != number2)
						return (number1 < number2) ? -1 : 1;
				}
				
				idx1++;
				idx2++;
			}
		}
	}
	
	public static int compareIgnoreCase(char c1, char c2)
	{
		if (c1 == c2)
			return 0;
		
		c1 = Character.toUpperCase(c1);
		c2 = Character.toUpperCase(c2);
		if (c1 == c2)
			return 0;
		
		c1 = Character.toLowerCase(c1);
		c2 = Character.toLowerCase(c2);
		if (c1 == c2)
			return 0;
		
		return c1 - c2;
	}
	
	public static void closeQuietly(Closeable closeable)
	{
		if (closeable != null)
		{
			try
			{
				closeable.close();
			}
			catch (IOException e)
			{
			}
		}
	}
	
	public static boolean isBlank(String s)
	{
		if (s == null)
			return true;
		
		for (int i = 0 ; i < s.length() ; i++)
			if (!Character.isWhitespace(s.charAt(i)))
				return false;
			
		return true;
	}
	
	public static boolean isNotBlank(String s)
	{
		return !isBlank(s);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object o)
	{
		return (T) o;
	}
}

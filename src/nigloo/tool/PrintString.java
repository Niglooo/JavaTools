package nigloo.tool;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

public class PrintString extends PrintWriter
{
	private final CharArrayWriter caw;
	
	public PrintString()
	{
		this(new CharArrayWriter());
	}
	
	private PrintString(CharArrayWriter caw)
	{
		super(caw);
		this.caw = caw;
	}
	
	public void clear()
	{
		flush();
		caw.reset();
	}
	
	public boolean isEmpty()
	{
		flush();
		return caw.size() == 0;
	}
	
	@Override
	public String toString()
	{
		flush();
		return caw.toString();
	}
}

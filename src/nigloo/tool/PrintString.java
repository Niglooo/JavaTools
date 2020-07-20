package nigloo.tool;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class PrintString extends PrintStream {

	private final ByteArrayOutputStream baos;
	
	
	public PrintString() {
		this(new ByteArrayOutputStream());
	}
	
	private PrintString(ByteArrayOutputStream baos) {
		super(baos);
		this.baos = baos;
	}

	
	public void clear() {
		flush();
		baos.reset();
	}
	
	public boolean isEmpty() {
		flush();
		return baos.size() == 0;
	}
	
	@Override
	public String toString() {
		flush();
		return baos.toString();
	}
}

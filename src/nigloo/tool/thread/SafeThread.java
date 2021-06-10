package nigloo.tool.thread;

public class SafeThread extends Thread
{
	private volatile boolean suspended = false;
	private volatile boolean stop = false;
	private final Object mutex = new Object();
	
	public SafeThread()
	{
		super();
	}
	
	public SafeThread(Runnable target)
	{
		super();
	}
	
	public SafeThread(ThreadGroup group, Runnable target)
	{
		super(group, target);
	}
	
	public SafeThread(String name)
	{
		super(name);
	}
	
	public SafeThread(ThreadGroup group, String name)
	{
		super(group, name);
	}
	
	public SafeThread(Runnable target, String name)
	{
		super(target, name);
	}
	
	public SafeThread(ThreadGroup group, Runnable target, String name)
	{
		super(group, target, name);
	}
	
	public SafeThread(ThreadGroup group, Runnable target, String name, long stackSize)
	{
		super(group, target, name, stackSize);
	}
	
	public SafeThread(ThreadGroup group, Runnable target, String name, long stackSize, boolean inheritThreadLocals)
	{
		super(group, target, name, stackSize, inheritThreadLocals);
	}
	
	public final void safeSuspend() {
		suspended = true;
	}
	
	public final void safeResume() {
		suspended = false;
		synchronized (mutex) {
			mutex.notify();
		}
	}
	
	public final void safeStop() {
		stop = true;
		interrupt();
		synchronized (mutex) {
			mutex.notify();
		}
	}
	
	
	
	
	
	final protected void checkThreadState() throws ThreadStopException {
		
		assert this == Thread.currentThread();
		
		while (suspended) {
			if (stop)
				throw new ThreadStopException();
			
			synchronized (mutex) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {}
			}
		}
			
		if (stop)
			throw new ThreadStopException();
	}
}

package nigloo.tool.thread;

public class SafeThread extends Thread
{
	private volatile boolean suspended = false;
	private volatile boolean stop = false;
	private final Object canResume = new Object();
	
	private long sleepUntil = 0;
	
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
	
	public final void safeSuspend()
	{
		suspended = true;
	}
	
	public final void safeResume()
	{
		suspended = false;
		synchronized (canResume)
		{
			canResume.notify();
		}
	}
	
	public final void safeStop()
	{
		stop = true;
		interrupt();
		synchronized (canResume)
		{
			canResume.notify();
		}
	}
	
	/***************************************************************************
	 * * Called only from this thread * *
	 **************************************************************************/
	
	protected final void checkThreadState() throws ThreadStopException
	{
		if (this != Thread.currentThread())
			throw new IllegalStateException("checkThreadState can only be called from \"this\" Thread");
		
		while (suspended)
		{
			if (stop)
				throw new ThreadStopException();
			
			synchronized (canResume)
			{
				try
				{
					canResume.wait();
				}
				catch (InterruptedException e)
				{
				}
			}
		}
		
		if (stop)
			throw new ThreadStopException();
	}
	
	protected final void uninterruptedSleep(long millis) throws ThreadStopException
	{
		if (this != Thread.currentThread())
			throw new IllegalStateException("uninterruptedSleep can only be called from \"this\" Thread");
		
		if (millis < 0)
			throw new IllegalArgumentException("millis must be positive. Got " + millis);
		
		long now = System.currentTimeMillis();
		sleepUntil = now + millis;
		
		while (sleepUntil > now)
		{
			try
			{
				Thread.sleep(sleepUntil - now);
			}
			catch (InterruptedException e)
			{
			}
			
			checkThreadState();
			
			now = System.currentTimeMillis();
		}
	}
}

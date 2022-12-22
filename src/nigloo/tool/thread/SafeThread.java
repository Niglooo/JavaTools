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
		super(target);
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
	
	public static void checkThreadState() throws ThreadStopException
	{
		SafeThread currentThread = getCurrentSafeThread();
		
		while (currentThread.suspended)
		{
			if (currentThread.stop)
				throw new ThreadStopException();
			
			synchronized (currentThread.canResume)
			{
				try
				{
					currentThread.canResume.wait();
				}
				catch (InterruptedException e)
				{
				}
			}
		}
		
		if (currentThread.stop)
			throw new ThreadStopException();
	}
	
	public static void uninterruptedSleep(long millis) throws ThreadStopException
	{
		SafeThread currentThread = getCurrentSafeThread();
		
		if (millis < 0)
			throw new IllegalArgumentException("millis must be positive. Got " + millis);
		
		long now = System.currentTimeMillis();
		currentThread.sleepUntil = now + millis;
		
		while (currentThread.sleepUntil > now)
		{
			try
			{
				Thread.sleep(currentThread.sleepUntil - now);
			}
			catch (InterruptedException e)
			{
			}
			
			checkThreadState();
			
			now = System.currentTimeMillis();
		}
		
		checkThreadState();
	}
	
	private static SafeThread getCurrentSafeThread() throws ThreadStopException
	{
		if (Thread.currentThread() instanceof SafeThread currentThread)
			return currentThread;
		else
			throw new IllegalStateException("The current thread is not a " + SafeThread.class.getSimpleName());
	}
}

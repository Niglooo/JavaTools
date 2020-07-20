package nigloo.tool.thread;

public class SafeThread extends Thread
{
	private volatile boolean suspended = false;
	private volatile boolean stop = false;
	private final Object mutex = new Object();
	
	
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

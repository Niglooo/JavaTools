package nigloo.tool;

/**
 * It's a metronome, it's a timer, it's a MetronomeTimer. It allows you to
 * control the speed a loop (useful in a thread)
 */
public final class MetronomeTimer
{
	private final long interval;
	private long lastTick;
	
	public MetronomeTimer(long interval)
	{
		this.interval = interval;
		this.lastTick = System.currentTimeMillis();
	}
	
	public synchronized void waitNextTick() throws InterruptedException
	{
		long timeToSleep;
		while ((timeToSleep = lastTick + interval - System.currentTimeMillis()) > 0)
			Thread.sleep(timeToSleep);
		
		lastTick = System.currentTimeMillis();
	}
	
	public synchronized void reset()
	{
		lastTick = System.currentTimeMillis();
	}
}

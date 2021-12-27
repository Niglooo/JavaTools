package nigloo.tool;

public class StopWatch
{
	public enum Precision
	{
		NANO
		{
			@Override
			long fromNano(long nano)
			{
				return nano;
			}
		},
		MILLI
		{
			@Override
			long fromNano(long nano)
			{
				return nano / 1_000_000l;
			}
		};
		
		abstract long fromNano(long nano);
	}
	
	private Precision precision = Precision.MILLI;
	
	public StopWatch()
	{
	}
	
	public StopWatch(Precision precision)
	{
		this.precision = precision;
	}
	
	private enum State
	{
		UNSTARTED, STARTED_NO_SLPIT, STARTED_WITH_SPLIT, STOPPED
	}
	
	private State state = State.UNSTARTED;
	
	private boolean isRunning()
	{
		return state != State.UNSTARTED && state != State.STOPPED;
	}
	
	private long start;
	private long split;
	private long stop;
	
	public void start()
	{
		if (isRunning())
			throw new IllegalStateException("already started");
		
		state = State.STARTED_NO_SLPIT;
		start = System.nanoTime();
	}
	
	public long split()
	{
		if (!isRunning())
			throw new IllegalStateException("not running");
		
		long oldSplit = (state == State.STARTED_NO_SLPIT) ? start : split;
		split = System.nanoTime();
		state = State.STARTED_WITH_SPLIT;
		return precision.fromNano(split - oldSplit);
	}
	
	public long stop()
	{
		if (!isRunning())
			throw new IllegalStateException("not running");
		
		stop = System.nanoTime();
		state = State.STOPPED;
		return precision.fromNano(stop - start);
	}
	
	public long splitAndStop()
	{
		if (!isRunning())
			throw new IllegalStateException("not running");
		
		long time = split();
		state = State.STOPPED;
		return time;
	}
	
	public long time()
	{
		long stopTime = switch (state)
		{
			case UNSTARTED -> throw new IllegalStateException("not started");
			case STARTED_NO_SLPIT -> System.nanoTime();
			case STARTED_WITH_SPLIT -> split;
			case STOPPED -> stop;
		};
		
		return precision.fromNano(stopTime - start);
	}
}

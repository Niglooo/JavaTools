package nigloo.tool;

public class Reference<T>
{
	private T target;
	
	public Reference() {
		this.target = null;
	}

	public Reference(T target) {
		this.target = target;
	}

	public T get() {
		return target;
	}

	public void set(T target) {
		this.target = target;
	}
	
	public void clear() {
		target = null;
	}
}

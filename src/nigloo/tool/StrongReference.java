package nigloo.tool;

public class StrongReference<T>
{
	private T value;
	
	public StrongReference() {
		this.value = null;
	}

	public StrongReference(T target) {
		this.value = target;
	}

	public T get() {
		return value;
	}

	public void set(T target) {
		this.value = target;
	}
	
	public void clear() {
		value = null;
	}
	
	@Override
	public String toString()
	{
		return "StrongReference [value=" + value + "]";
	}
}

package nigloo.tool.gson;

import java.lang.reflect.Type;
import java.util.Objects;

import com.google.gson.InstanceCreator;

public class ExistingInstanceCreator<T> implements InstanceCreator<T>
{
	private final T instance;
	
	public ExistingInstanceCreator(T instance)
	{
		this.instance = Objects.requireNonNull(instance, "instance");
	}
	
	@Override
	public T createInstance(Type type)
	{
		return instance;
	}
}

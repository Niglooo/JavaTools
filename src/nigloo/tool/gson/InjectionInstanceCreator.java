package nigloo.tool.gson;

import java.lang.reflect.Type;

import com.google.gson.InstanceCreator;

import nigloo.tool.injection.Injector;

public class InjectionInstanceCreator implements InstanceCreator<Object>
{
	@Override
	public Object createInstance(Type type)
	{
		return Injector.getInstance((Class<?>) type);
	}
}

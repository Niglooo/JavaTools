package nigloo.tool.injection.impl;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import nigloo.tool.injection.InjectionContext;


public class FreeInjectionContext implements InjectionContext
{
	private static final Logger LOGGER = Logger.getLogger(FreeInjectionContext.class.getName());
	
	@Override
	public Object getInstance(Field field)
	{
		LOGGER.finer("Injecting free instance of "+field.getType()+" in "+field);
		return getInstance(field.getType());
	}
	
	@Override
	public <T> T getInstance(Class<T> clazz)
	{
		return instanciate(clazz);
	}
}

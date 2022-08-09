package nigloo.tool.injection.impl;

import java.lang.annotation.AnnotationFormatError;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nigloo.tool.injection.InjectionContext;
import nigloo.tool.injection.InjectionException;
import nigloo.tool.injection.annotation.Singleton;


public class SingletonInjectionContext implements InjectionContext {

	
	private Set<Class<?>> singletonClasses = new HashSet<>();
	private Map<Class<?>, Class<?>> singletonImplementionClasses = new HashMap<>();
	private Map<Class<?>, Object> singletonInstances = new HashMap<>();
	
	/**
	 * Force clazz to be considered a singleton
	 * 
	 * @param <T>
	 * @param clazz
	 * @param instance
	 */
	synchronized public <T> void setSingletonInstance(Class<T> clazz, T instance) {
		
		singletonClasses.add(clazz);
		addSingletonInstance(clazz, instance, false);
	}
	
	synchronized public <T> void replaceSingletonInstance(Class<T> clazz, T instance) {
		
		if (!isSingleton(clazz))
			throw new InjectionException(clazz.getName()+" is not a Singleton");
		
		addSingletonInstance(clazz, instance, true);
	}
	
	@Override
	synchronized public boolean declareInstance(Object instance) {
		Class<?> clazz = instance.getClass();
		
		if (isSingleton(clazz)) {
			addSingletonInstance(clazz, instance, false);
			return true;
		}
		
		return false;
	}

	@Override
	synchronized public <T> T getInstance(Class<T> clazz)
	{
		if (!isSingleton(clazz))
			return null;
		
		T instance = getSingletonInstance(clazz);
		if (instance != null)
			return instance;
		
		Singleton singletonAnnotation = clazz.getAnnotation(Singleton.class);
		if (singletonAnnotation != null && singletonAnnotation.implementation() != void.class)
		{
			if (clazz.isAssignableFrom(singletonAnnotation.implementation()))
				singletonImplementionClasses.put(clazz, singletonAnnotation.implementation());
			else
				throw new AnnotationFormatError("Invalid value for attribute implementation of @Singleton of class "
				        + clazz.getCanonicalName() + ". The value MUST be a class which extends/implements "
				        + clazz.getCanonicalName());
		}
		
		@SuppressWarnings("unchecked")
		Class<? extends T> implClass = (Class<? extends T>) singletonImplementionClasses.getOrDefault(clazz, clazz);
		instance = instanciate(implClass);
		
		/*
		 * We test again if there an instance registered because the constructor may
		 * have called declareInstanceInjectAndPostConstruct and so the singleton
		 * declared itself
		 */
		T existingInstance = getSingletonInstance(clazz);
		
		if (existingInstance == null)
			addSingletonInstance(clazz, instance, false);
		else if (instance != existingInstance)
			throw new AssertionError("Instance of " + clazz.getName() + " already exist (should NOT happen)");
		
		return instance;
	}
	
	
	private boolean isSingleton(Class<?> clazz) {
		return clazz.isAnnotationPresent(Singleton.class) || singletonClasses.contains(clazz);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getSingletonInstance(Class<T> clazz) {
		
		return (T) singletonInstances.get(clazz);
	}
	
	
	private void addSingletonInstance(Class<?> clazz, Object instance, boolean replaceInstance) {

		if (!replaceInstance)
		{
			Object existingInstance = singletonInstances.get(clazz);
			if (existingInstance != null && existingInstance != instance)
				throw new InjectionException("An instance of "+clazz+" already exists");
		}
		
		singletonInstances.put(clazz, instance);
	}
}

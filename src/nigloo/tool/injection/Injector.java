package nigloo.tool.injection;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nigloo.tool.collection.WeakIdentityHashSet;
import nigloo.tool.injection.annotation.Inject;
import nigloo.tool.injection.annotation.PostConstruct;
import nigloo.tool.injection.impl.FreeInjectionContext;


public class Injector
{
	private Injector() {throw new UnsupportedOperationException();}
	
	/**
	 * When set to {@code true} <br/>
	 * - {@link #init(Object)} is a no-op<br/>
	 * - {@link #getInstance(Class)} always return {@code null}
	 */
	private static boolean DISABLE_INJECTION = false;
	
	public static void DISABLE() {
		DISABLE_INJECTION = true;
	}

	/**
	 * Set of already constructed instances.
	 * Needed to avoid double construction due to circular dependencies
	 */
	private static WeakIdentityHashSet<Object> initializedInstances = new WeakIdentityHashSet<>();
	
	private static List<InjectionContext> contexts = new ArrayList<>();
	static {
		contexts.add(new FreeInjectionContext());
	}
	
	// the FreeInjectionContext must always stay last since it's always willing to create an instance
	synchronized public static void addContext(InjectionContext context) {
		contexts.add(contexts.size()-1, context);
	}
	
	synchronized public static boolean removeContext(InjectionContext context) {
		return contexts.remove(context);
	}
	
	/**
	 * Inject, post-construct and register instance if singleton
	 * 
	 * @param instance
	 */
	synchronized public static void init(Object instance) {
		
		if (DISABLE_INJECTION)
			return;
		
		if (initializedInstances.contains(instance))
			return;
		
		for (InjectionContext context : contexts) {
			if (context.declareInstance(instance))
				break;
		}
		
		initializeInstance(instance);
	}
	
	/**
	 * Retrieve an instance of clazz
	 * 
	 * @param clazz
	 * @return
	 */
	synchronized public static <T> T getInstance(Class<T> clazz) {
		
		if (DISABLE_INJECTION)
			return null;
		
		T instance = null;
		for (InjectionContext context : contexts)
			if ((instance = context.getInstance(clazz)) != null)
				break;
		
		if (instance == null)
			throw new AssertionError("Cannot get an instance of "+clazz+" (should NOT happen)");
		
		initializeInstance(instance);
		
		return instance;
	}
	
	private static void initializeInstance(Object instance) {
		if (initializedInstances.contains(instance))
			return;
		
		initializedInstances.add(instance);
		inject(instance);
		postConstruct(instance);
	}
	
	
	private static void inject(Object object) {
		
		Class<?> clazz = object.getClass();
		
		for (Field field : getAllFields(clazz)) {
			try {
				if (!field.isAnnotationPresent(Inject.class))
					continue;
				
				field.setAccessible(true);
				
				if (field.get(object) != null)
					continue;
				
				Object instance = null;
				for (InjectionContext context : contexts)
					if ((instance = context.getInstance(field)) != null)
						break;
				
				if (instance == null)
					throw new AssertionError("Cannot get an instance for field "+field+" (should NOT happen)");
				
				initializeInstance(instance);
				
				field.set(object, instance);
			
			} catch (SecurityException |
					IllegalArgumentException |
					IllegalAccessException |
					InaccessibleObjectException e) {
				throw new InjectionException("Cannot set "+field, e);
			}
		}
	}
	
	
	private static void postConstruct(Object object)
	{
		Class<?> clazz = object.getClass();
		
		for (Method method : getAllMethods(clazz)) {
			
			if (!method.isAnnotationPresent(PostConstruct.class))
				continue;
			
			try {
				method.setAccessible(true);
				
				if (method.getParameterCount() > 0)
					throw new InjectionException("@PostConstruct method cannot have parameters: "+method);
				
				method.invoke(object);
				
				return;
			}
			catch (SecurityException |
					IllegalAccessException |
					IllegalArgumentException |
					InvocationTargetException |
					InaccessibleObjectException e) {
				throw new InjectionException("Cannot call "+method, e);
			}
		}
	}
	
	private static List<Field> getAllFields(Class<?> clazz) {
		
		List<Field> allFields = new ArrayList<>();
		
		Class<?> current = clazz;
		while(current.getSuperclass()!=null) {
			
			allFields.addAll(Arrays.asList(current.getDeclaredFields()));
			
			current = current.getSuperclass();
		}
		
		// Superclass first (for injection order)
		Collections.reverse(allFields);
		
		return allFields;
	}
	
	private static List<Method> getAllMethods(Class<?> clazz) {
		
		List<Method> allMethods = new ArrayList<>();
		
		Class<?> current = clazz;
		while(current.getSuperclass()!=null) {
			
			allMethods.addAll(Arrays.asList(current.getDeclaredMethods()));
			
			current = current.getSuperclass();
		}
		
		return allMethods;
	}
}

package nigloo.tool.injection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

public interface InjectionContext
{
	/**
	 * Retrieve an instance of T or {@code null} if this context cannot provide such an instance.
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	<T> T getInstance(Class<T> clazz);
	
	/**
	 * Retrieve an instance to be injected in field or null if this context cannot provide such an instance.
	 * 
	 * @param field
	 * @return
	 * 
	 * @implNote Default implementation return {@code getInstance(field.getType())}
	 */
	default Object getInstance(Field field) {
		return getInstance(field.getType());
	};
	
	/**
	 * Declare an instance that has not been produced by an InjectionContext and that this InjectionContext
	 * might want to handle. If that's the case, return {@code true}, otherwise {@code false}.
	 * 
	 * @param instance
	 * @return
	 * 
	 * @implNote Default implementation just return {@code false}
	 */
	default boolean declareInstance(Object instance) {
		return false;
	}
	
	default <T> T instanciate(Class<T> clazz) {
		Logger.getLogger(getClass().getName()).finest("Instanciating "+clazz.getName());
		try {
			//TODO Call other constructor if available (inject parameters)
			//if several constructors, error
			return clazz.getConstructor().newInstance();
		}
		catch (InstantiationException |
				IllegalAccessException |
				IllegalArgumentException |
				InvocationTargetException |
				NoSuchMethodException |
				SecurityException e)
		{
			throw new InjectionException("Cannot instanciate "+clazz.getName(), e);
		}
	}
}

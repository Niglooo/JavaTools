package nigloo.tool.gson;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class RecordsTypeAdapterFactory implements TypeAdapterFactory
{
	@Override
	@SuppressWarnings("unchecked")
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
	{
		if (!type.getRawType().isRecord())
			return null;
		
		return new RecordTypeAdapter<T>(gson, (Class<T>) type.getRawType(), gson.getDelegateAdapter(this, type));
	}
	
	private static class RecordTypeAdapter<T> extends TypeAdapter<T>
	{
		private final Gson gson;
		private final TypeAdapter<T> delegate;
		private final Field[] fields;
		private final Method[] accessors;
		private final Constructor<T> constructor;
		

		public RecordTypeAdapter(Gson gson, Class<T> clazz, TypeAdapter<T> delegate)
		{
			super();
			this.gson = gson;
			this.delegate = delegate;
			try
			{
				RecordComponent[] components = clazz.getRecordComponents();
				fields = new Field[components.length];
				accessors = new Method[components.length];
				Class<?>[] types = new Class<?>[components.length];
				for (int i = 0 ; i < components.length ; i++)
				{
					fields[i] = clazz.getDeclaredField(components[i].getName());
					accessors[i] = components[i].getAccessor();
					types[i] = components[i].getType();
				}
				
				this.constructor = clazz.getConstructor(types);
				this.constructor.setAccessible(true);
			}
			catch (Throwable e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void write(JsonWriter out, T value) throws IOException
		{
			if (value == null)
			{
				out.nullValue();
				return;
			}
			
			delegate.write(out, value);
		}
		
		@Override
		public T read(JsonReader in) throws IOException
		{
			if (in.peek() == JsonToken.NULL)
			{
				in.nextNull();
				return null;
			}
			
			Object[] values = new Object[fields.length];
			
			in.beginObject();
			
			while (in.hasNext())
			{
				String name = in.nextName();
				int i;
				for (i = 0 ; i < fields.length ; i++)
				{
					if (gson.fieldNamingStrategy().translateName(fields[i]).equals(name))
					{
						values[i] = gson.fromJson(in, fields[i].getGenericType());
						break;
					}
				}
				if (i == fields.length)
					in.skipValue();
			}
			
			in.endObject();
			
			try
			{
				return constructor.newInstance(values);
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
			       InvocationTargetException e)
			{
				throw new JsonParseException("Cannot instanciate " + constructor.getDeclaringClass() + " using "
				        + constructor, e);
			}
		}
	}
}

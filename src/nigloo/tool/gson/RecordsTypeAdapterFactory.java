package nigloo.tool.gson;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
				Class<?>[] types = new Class<?>[components.length];
				for (int i = 0 ; i < components.length ; i++)
				{
					fields[i] = clazz.getDeclaredField(components[i].getName());
					types[i] = components[i].getType();
				}
				
				this.constructor = clazz.getDeclaredConstructor(types);
				this.constructor.setAccessible(true);
			}
			catch (NoSuchFieldException | NoSuchMethodException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void write(JsonWriter out, T value) throws IOException
		{
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

			loopJsonField:
			while (in.hasNext())
			{
				String name = in.nextName();
				for (int i = 0 ; i < fields.length ; i++)
				{
					if (gson.fieldNamingStrategy().translateName(fields[i]).equals(name))
					{
						values[i] = gson.fromJson(in, fields[i].getGenericType());
						continue loopJsonField;
					}
				}
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
				throw new JsonParseException("Cannot instantiate " + constructor.getDeclaringClass() + " using "
				        + constructor, e);
			}
		}
	}
}

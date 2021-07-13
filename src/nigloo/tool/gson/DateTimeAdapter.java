package nigloo.tool.gson;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DateTimeAdapter implements JsonSerializer<TemporalAccessor>, JsonDeserializer<TemporalAccessor>
{
	private final DateTimeFormatter formatter;
	
	public DateTimeAdapter()
	{
		this.formatter = DateTimeFormatter.ISO_DATE_TIME;
	}
	
	public DateTimeAdapter(DateTimeFormatter formatter)
	{
		this.formatter = formatter;
	}
	
	@Override
	public JsonElement serialize(TemporalAccessor src, Type typeOfSrc, JsonSerializationContext context)
	{
		return new JsonPrimitive(formatter.format(src));
	}
	
	@Override
	public TemporalAccessor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
	        throws JsonParseException
	{
		return formatter.parse(json.getAsString(), query(typeOfT));
	}
	
	private TemporalQuery<TemporalAccessor> query(Type typeOfT)
	{
		return ta ->
		{
			try
			{
				Method from = ((Class<?>) typeOfT).getMethod("from", TemporalAccessor.class);
				return (TemporalAccessor) from.invoke(null, ta);
			}
			catch (Exception e)
			{
				return ta;
			}
		};
	}
}

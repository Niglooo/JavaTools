package nigloo.tool.gson;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class DateTimeTypeAdapter extends TypeAdapter<TemporalAccessor>
{
	private final DateTimeFormatter formatter;
	
	public DateTimeTypeAdapter()
	{
		this.formatter = DateTimeFormatter.ISO_DATE_TIME;
	}
	
	public DateTimeTypeAdapter(DateTimeFormatter formatter)
	{
		this.formatter = formatter;
	}
	
	@Override
	public void write(JsonWriter out, TemporalAccessor value) throws IOException
	{
		if (value == null)
		{
			out.nullValue();
			return;
		}
		
		out.value(formatter.format(value));
	}
	
	@Override
	public TemporalAccessor read(JsonReader in) throws IOException
	{
		if (in.peek() == JsonToken.NULL)
		{
			in.nextNull();
			return null;
		}
		
		return formatter.parse(in.nextString());
	}
	
}

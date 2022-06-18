package nigloo.tool.gson;

import java.io.IOException;
import java.util.regex.Pattern;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * A nullSafe {@link TypeAdapter} mapping {@link Pattern} as String.
 * 
 * @implNote Use {@link Pattern#pattern()} and {@link Pattern#compile(String)}
 *           for mapping
 * 
 */
public class PatternTypeAdapter extends TypeAdapter<Pattern>
{
	
	@Override
	public void write(JsonWriter out, Pattern value) throws IOException
	{
		if (value == null)
			out.nullValue();
		else
			out.value(value.pattern());
	}
	
	@Override
	public Pattern read(JsonReader in) throws IOException
	{
		if (in.peek() == JsonToken.NULL)
		{
			in.nextNull();
			return null;
		}
		else
			return Pattern.compile(in.nextString());
	}
}

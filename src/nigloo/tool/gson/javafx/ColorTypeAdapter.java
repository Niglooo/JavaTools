package nigloo.tool.gson.javafx;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import javafx.scene.paint.Color;
import nigloo.tool.javafx.FXUtils;

public class ColorTypeAdapter extends TypeAdapter<Color>
{
	@Override
	public void write(JsonWriter out, Color color) throws IOException
	{
		out.value(FXUtils.toRGBA(color));
	}

	@Override
	public Color read(JsonReader in) throws IOException
	{
		return Color.web(in.nextString());
	}
}

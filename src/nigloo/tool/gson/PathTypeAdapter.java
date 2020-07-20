package nigloo.tool.gson;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class PathTypeAdapter extends TypeAdapter<Path> {

	@Override
	public void write(JsonWriter out, Path value) throws IOException {
		if (value == null)
			out.nullValue();
		else
			out.value(value.toString());
	}

	@Override
	public Path read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		else
			return Paths.get(in.nextString());
	}
}

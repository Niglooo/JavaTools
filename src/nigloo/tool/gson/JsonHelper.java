package nigloo.tool.gson;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.charset.StandardCharsets;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import nigloo.tool.Utils;

public class JsonHelper
{
	private JsonHelper()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Ex: field1[0].1[field2].name
	 * 
	 * @param element
	 * @param path
	 * @return
	 * @throws NumberFormatException
	 * @throws IndexOutOfBoundsException
	 * @throws IllegalArgumentException
	 */
	public static String followPath(JsonElement element, String path)
	        throws NumberFormatException,
	        IndexOutOfBoundsException,
	        IllegalArgumentException,
	        ClassCastException
	{
		return followPath(element, path, String.class);
	}
	
	/**
	 * Ex: field1[0].1[field2].name
	 * 
	 * @param element
	 * @param path
	 * @param resultType
	 * @return
	 * @throws NumberFormatException
	 * @throws IndexOutOfBoundsException
	 * @throws IllegalArgumentException
	 * @throws ClassCastException
	 */
	public static <T> T followPath(JsonElement element, String path, Class<T> resultType)
	        throws NumberFormatException,
	        IndexOutOfBoundsException,
	        IllegalArgumentException,
	        ClassCastException
	{
		if (element == null)
			return null;
		
		if (path == null || path.isEmpty())
		{
			if (resultType.isAssignableFrom(element.getClass()))
				return Utils.cast(element);
			
			if (element.isJsonNull())
				return null;
			
			try
			{
				// Final classes
				if (resultType == Long.class || resultType == long.class)
					return Utils.cast(element.getAsLong());
				if (resultType == Integer.class || resultType == int.class)
					return Utils.cast(element.getAsInt());
				if (resultType == Short.class || resultType == short.class)
					return Utils.cast(element.getAsShort());
				if (resultType == Byte.class || resultType == byte.class)
					return Utils.cast(element.getAsByte());
				if (resultType == Boolean.class || resultType == boolean.class)
					return Utils.cast(element.getAsBoolean());
				if (resultType == Float.class || resultType == float.class)
					return Utils.cast(element.getAsFloat());
				if (resultType == Double.class || resultType == double.class)
					return Utils.cast(element.getAsDouble());
				if (resultType == Character.class || resultType == char.class)
				{
					String str = element.getAsString();
					if (str.length() != 1)
						throw new UnsupportedOperationException();
					return Utils.cast(str.charAt(0));
				}
				if (resultType.isAssignableFrom(String.class))
					return Utils.cast(element.getAsString());
				
				// Can be subclassed => use isAssignableFrom
				if (resultType.isAssignableFrom(Number.class))
					return Utils.cast(element.getAsNumber());
				if (resultType.isAssignableFrom(BigDecimal.class))
					return Utils.cast(element.getAsBigDecimal());
				if (resultType.isAssignableFrom(BigInteger.class))
					return Utils.cast(element.getAsBigInteger());
				
				throw new UnsupportedOperationException();
			}
			catch (ClassCastException | IllegalStateException | UnsupportedOperationException e)
			{
				throw new ClassCastException("Cannot convert " + element + " into " + resultType.getSimpleName());
			}
		}
		
		path = path.replace('[', '.').replace("]", "");
		
		int posDot = path.indexOf('.');
		String field = (posDot == -1) ? path : path.substring(0, posDot);
		String subPath = (posDot == -1) ? "" : path.substring(posDot + 1);
		JsonElement subElement;
		
		if (element.isJsonNull())
			return null;
		else if (element.isJsonObject())
			subElement = element.getAsJsonObject().get(field);
		else if (element.isJsonArray())
			subElement = element.getAsJsonArray().get(Integer.parseInt(field));
		else
			throw new IllegalArgumentException("Cannot access [" + field + "] of " + element);
		
		return followPath(subElement, subPath, resultType);
	}
	
	/**
	 * Equivalent to prettyPrint(json, System.out)
	 * 
	 * @param json
	 */
	public static void prettyPrint(JsonElement json)
	{
		prettyPrint(json, System.out);
	}
	
	public static void prettyPrint(JsonElement json, Appendable out)
	{
		new GsonBuilder().setPrettyPrinting().create().toJson(json, out);
	}
	
	public static BodyHandler<JsonElement> httpBodyHandler()
	{
		return new BodyHandler<JsonElement>()
		{
			@Override
			public BodySubscriber<JsonElement> apply(ResponseInfo responseInfo)
			{
				return BodySubscribers.mapping(BodySubscribers.ofInputStream(), in ->
				{
					return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				});
			}
		};
	}
}

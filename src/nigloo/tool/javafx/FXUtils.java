package nigloo.tool.javafx;

import java.util.Objects;
import java.util.function.Function;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;

public class FXUtils
{
	private FXUtils()
	{
		throw new UnsupportedOperationException();
	}
	
	public static <T> ObjectBinding<T> toObject(BooleanProperty booleanProperty, Function<Boolean, ? extends T> mapping)
	{
		Objects.requireNonNull(booleanProperty, "booleanProperty");
		Objects.requireNonNull(mapping, "mapping");
		
		return new ObjectBinding<T>()
		{
			{
				bind(booleanProperty);
			}
			
			@Override
			protected T computeValue()
			{
				return mapping.apply(booleanProperty.getValue());
			}
		};
	}
}

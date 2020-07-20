package nigloo.tool.javafx.component;

import javafx.beans.property.IntegerProperty;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

public class EditableIntegerSpinner extends Spinner<Integer> {
	
	private SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory;
	private int previousValidValue;

	public EditableIntegerSpinner() {
		
		previousValidValue = 0;
		
		valueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);
		this.setValueFactory(valueFactory);
		
		valueFactory.valueProperty().addListener((obs, oldValue, newValue) -> {previousValidValue = newValue;});
		
		StringConverter<Integer> converter = new StringConverter<Integer>() {
			@Override
			public Integer fromString(String s) {
				try {
					previousValidValue = Integer.valueOf(s);
				}
				catch (NumberFormatException e) {}
				
				return previousValidValue;
			}
			@Override
			public String toString(Integer i) {
				return i.toString();
			}
		};
		
		valueFactory.setConverter(converter);
		TextFormatter<Integer> formatter = new TextFormatter<Integer>(converter, valueFactory.getValue());
		this.getEditor().setTextFormatter(formatter);
		valueFactory.valueProperty().bindBidirectional(formatter.valueProperty());
		
		this.setEditable(true);
	}
	
	public IntegerProperty minProperty() {
		return valueFactory.minProperty();
	}
	
	public void setMin(int value) {
		valueFactory.setMin(value);
	}
	
	public int getMin() {
		return valueFactory.getMin();
	}
	
	public IntegerProperty maxProperty() {
		return valueFactory.maxProperty();
	}
	
	public void setMax(int value) {
		valueFactory.setMax(value);
	}
	
	public int getMax() {
		return valueFactory.getMax();
	}
	
	public void setValue(int value) {
		valueFactory.setValue(value);
	}
}

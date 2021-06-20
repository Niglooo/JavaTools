package nigloo.tool.javafx.component.dialog;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import nigloo.tool.javafx.FXUtils;

public class ExceptionDialog extends AlertWithIcon
{
	public ExceptionDialog(Throwable exception, String errorMessage)
	{
		super(AlertType.ERROR);
		
		this.setTitle("Exception Dialog");
		this.setHeaderText(errorMessage);
		this.setContentText(exception.getMessage());
		
		// Create expandable Exception.
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		pw.flush();
		String exceptionText = sw.toString();
		
		Label label = new Label("The exception stacktrace was:");
		
		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setPrefWidth(FXUtils.computeTextWidth(textArea.getFont(), textArea.getText(), 0.0D) + 50);
		
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);
		
		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);
		
		// Set expandable Exception into the dialog pane.
		this.getDialogPane().setExpandableContent(expContent);
	}
}

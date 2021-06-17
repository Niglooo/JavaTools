package nigloo.tool.javafx.component.dialog;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class AlertWithIcon extends Alert
{
	
	public AlertWithIcon(AlertType alertType)
	{
		this(alertType, (ButtonType[]) null);
	}
	
	public AlertWithIcon(AlertType alertType, ButtonType... buttons)
	{
		super(alertType, null, buttons);
		Scene scene = getDialogPane().getScene();
		getDialogPane().applyCss();
		if (getGraphic() != null)
			((Stage) scene.getWindow()).getIcons().add(((ImageView) getGraphic()).getImage());
	}
	
	public void setDefaultButton(ButtonType defaultButtonType)
	{
		for (ButtonType buttonType : getButtonTypes())
		{
			boolean isDefault = buttonType.equals(defaultButtonType);
			Button button = (Button) getDialogPane().lookupButton(buttonType);
			
			button.setDefaultButton(isDefault);
			if (isDefault)
				button.requestFocus();
		}
	}
}

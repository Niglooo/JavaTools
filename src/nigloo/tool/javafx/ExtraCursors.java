package nigloo.tool.javafx;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;

public class ExtraCursors
{
	public static final Cursor SCROLL_MIDDLE = load("scroll_middle.png", 16, 16);
	
	public static final Cursor SCROLL_UP = load("scroll_up.png", 16, 16);
	
	public static final Cursor SCROLL_DOWN = load("scroll_down.png", 16, 16);
	
	private static Cursor load(String filename, double hotspotX, double hotspotY)
	{
		return new ImageCursor(new Image(ExtraCursors.class.getResourceAsStream("internal/" + filename)),
		                       hotspotX,
		                       hotspotY);
	}
	
	private ExtraCursors()
	{
		throw new UnsupportedOperationException();
	}
}

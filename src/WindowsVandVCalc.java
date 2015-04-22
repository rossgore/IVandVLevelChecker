import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
public class WindowsVandVCalc{
    // brings up main window when run
    public static void main(String [] args)
    {
		try {
		    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e)
		{
				e.printStackTrace();
		}
		StatDebugGUI gui = new StatDebugGUI("windows");
    }
}
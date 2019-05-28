package simccs.gui;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 *
 * @author yaw
 */

public class SimCCSMain extends JFrame implements ActionListener
{
    private static List<Gui> windows = null;

    private static void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        Gui simccs = new Gui();
        Stage stage = new Stage();
        Scene scene = simccs.buildGUI(stage);
        fxPanel.setScene(scene);
    }
    public static void main(String args[])
    {
        showSimCCS();
    }

    public static void showSimCCS() {
       /* if(windows == null){
            windows = new ArrayList<>();
            Locale.setDefault(Locale.ENGLISH);
            Gui frame = new Gui();
            Scene scene = frame.buildGUI();
        }*/

        final JFXPanel fxPanel = new JFXPanel();
        JFrame frame = new JFrame("SimCCS Editor");
        frame.add(fxPanel);
        frame.setSize(1060, 660);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Platform.runLater(() -> initFX(fxPanel));
    }
    public void actionPerformed(java.awt.event.ActionEvent evt)
    {

        if (evt.getID() == Event.WINDOW_DESTROY)
            System.exit(0);
        if (evt.getSource() instanceof MenuItem)
        {
            String menuLabel = ((MenuItem)evt.getSource()).getLabel();

            if(menuLabel.equals("Close"))
            {
                setVisible(false);
            }
        }
    }
}

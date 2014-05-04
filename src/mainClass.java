import java.awt.BorderLayout;

import javax.swing.JFrame;

import GUI.DelaunayAp;

/**
 * 
 * @author Kevin van As
 *
 */
public class mainClass {
	
    private static String windowTitle = "Voronoi/Delaunay Window";

    /**
     * Main program (used when run as application instead of applet).
     */
    public static void main (String[] args) {
        DelaunayAp applet = new DelaunayAp();    // Create applet
        JFrame dWindow = new JFrame();           // Create window
        applet.init();                           // Applet initialization
        dWindow.setTitle(windowTitle);           // Set window title
        dWindow.setLayout(new BorderLayout());   // Specify layout manager
        dWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                                 // Specify closing behavior
        dWindow.add(applet, "Center");           // Place applet into window
        dWindow.setSize(700, 500);               // Set window size
        dWindow.setVisible(true);                // Show the window
    }

}

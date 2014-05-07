package GUI;

/*
 * Copyright (c) 2005, 2007 by L. Paul Chew.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.*;

import triangulation.*;
import triangulation.delaunay.algorithms.Algorithms;
import triangulation.delaunay.algorithms.DelaunayAlgorithm;
import triangulation.delaunay.refineAlgorithms.DelaunayRefineAlgorithm;
import triangulation.delaunay.refineAlgorithms.RefineAlgorithms;

/**
 * The Delaunay applet.
 *
 * Creates and displays a Delaunay Triangulation (DT) or a Voronoi Diagram
 * (VoD). Has a main program so it is an application as well as an applet.
 *
 * @author Paul Chew
 *
 * Created July 2005. Derived from an earlier, messier version.
 *
 * Modified December 2007. Updated some of the Triangulation methods. Added the
 * "Colorful" checkbox. Reorganized the interface between DelaunayAp and
 * DelaunayPanel. Added code to find a Voronoi cell.
 *
 */
@SuppressWarnings("serial")
public class DelaunayAp extends javax.swing.JApplet
        implements Runnable, ActionListener, MouseListener, MouseMotionListener {

    private boolean debug = false;             // Used for debugging
    private Component currentSwitch = null;    // Entry-switch that mouse is in

    private JRadioButton voronoiButton = new JRadioButton("Voronoi Diagram");
    private JRadioButton delaunayButton =
                                    new JRadioButton("Delaunay Triangulation");
    private JComboBox<Algorithms> algorithmSelector = new JComboBox<Algorithms>(Algorithms.values());
    private JButton clearButton = new JButton("Clear");
    private JCheckBox colorfulBox = new JCheckBox("More Colorful");
    private DelaunayPanel delaunayPanel = new DelaunayPanel(this);
    private JLabel circleSwitch = new JLabel("Show Empty Circles");
    private JLabel delaunaySwitch = new JLabel("Show Delaunay Edges");
    private JLabel voronoiSwitch = new JLabel("Show Voronoi Edges");
    
    private JButton loadPointsButton = new JButton("Points",IO.createImageIcon(getClass().getResource("/images/Open16.gif")));
    private JButton savePointsButton = new JButton("Points",IO.createImageIcon(getClass().getResource("/images/Save16.gif")));
    private JButton loadBGButton = new JButton("BG",IO.createImageIcon(getClass().getResource("/images/Open16.gif")));
    private JFileChooser fc = new JFileChooser();

    private JComboBox<RefineAlgorithms> refinementSelector = new JComboBox<RefineAlgorithms>(RefineAlgorithms.values());
    private JButton runRefineAlgorButton = new JButton("Refine");
    
    private JComboBox<MouseModes> mousemodeSelector = new JComboBox<MouseModes>(MouseModes.values());

    /**
     * Initialize the applet.
     * As recommended, the actual use of Swing components takes place in the
     * event-dispatching thread.
     */
    public void init () {
    	try {SwingUtilities.invokeAndWait(this);}
        catch (Exception e) {System.err.println("Initialization failure");}
    }

    /**
     * Set up the applet's GUI.
     * As recommended, the init method executes this in the event-dispatching
     * thread.
     */
    public void run () {
        setLayout(new BorderLayout());
        
        JPanel buttonNorthPanel = new JPanel();
        buttonNorthPanel.setLayout(new BorderLayout());
        this.add(buttonNorthPanel, "North");
        
        // Add the button controls: viewSelection
        ButtonGroup group = new ButtonGroup();
        group.add(delaunayButton);
        group.add(voronoiButton);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(delaunayButton);
        buttonPanel.add(voronoiButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(new JLabel("          "));      // Spacing
        buttonPanel.add(colorfulBox);
        buttonPanel.add(mousemodeSelector);
        buttonPanel.add(new JLabel("          "));      // Spacing
        buttonPanel.add(loadPointsButton);
        buttonPanel.add(savePointsButton);
        buttonPanel.add(loadBGButton);
        buttonNorthPanel.add(buttonPanel, "North");

        // Add the button controls: algorithmSelection
        buttonPanel = new JPanel();
        buttonPanel.add(algorithmSelector);
        buttonPanel.add(new JLabel("          "));      // Spacing
        buttonPanel.add(refinementSelector);
        buttonPanel.add(runRefineAlgorButton);
        buttonNorthPanel.add(buttonPanel, "Center");

        // Add the mouse-entry switches
        JPanel switchPanel = new JPanel();
        switchPanel.add(circleSwitch);
        switchPanel.add(new Label("     "));            // Spacing
        switchPanel.add(delaunaySwitch);
        switchPanel.add(new Label("     "));            // Spacing
        switchPanel.add(voronoiSwitch);
        this.add(switchPanel, "South");

        // Build the delaunay panel
        delaunayPanel.setBackground(Color.gray);
        this.add(delaunayPanel, "Center");

        // Register the listeners
        voronoiButton.addActionListener(this);
        delaunayButton.addActionListener(this);
        clearButton.addActionListener(this);
        loadPointsButton.addActionListener(this);
        savePointsButton.addActionListener(this);
        loadBGButton.addActionListener(this);
        colorfulBox.addActionListener(this);
        
        algorithmSelector.addActionListener(this);
        mousemodeSelector.addActionListener(this);
        runRefineAlgorButton.addActionListener(this);
        
        delaunayPanel.addMouseListener(this);
        delaunayPanel.addMouseMotionListener(this);
        circleSwitch.addMouseListener(this);
        delaunaySwitch.addMouseListener(this);
        voronoiSwitch.addMouseListener(this);

        // Initialize the radio buttons
        delaunayButton.doClick();
    }

    /**
     * A button has been pressed; redraw the picture.
     */
    public void actionPerformed(ActionEvent e) {
        if (debug)
            System.out.println(((AbstractButton)e.getSource()).getText());
        if (e.getSource() == clearButton) delaunayPanel.clear();
        if (e.getSource() == algorithmSelector){
        	delaunayPanel.changeAlgorithm(this.getActiveAlgorithm());
        }
        if(e.getSource() == mousemodeSelector){
            MouseModes mousemode = (MouseModes)mousemodeSelector.getSelectedItem();
            mousemode.onModeChange(delaunayPanel);        	
        }
        if(e.getSource() == loadBGButton){
        	int returnVal = fc.showOpenDialog(this);
        	fc.setBackground(Color.green);
        	fc.setDialogTitle("Load a background file (png, jpg, ...).");
        
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fc.getSelectedFile();
	            delaunayPanel.loadBackground(file);
	            //This is where a real application would open the file.
	            System.out.println("Opening: " + file.getName() + ".");
	        } else {
	            System.out.println("LoadBG command cancelled by user.");
	        }        	
        }
        if(e.getSource() == loadPointsButton){
        	int returnVal = fc.showOpenDialog(this);
        	fc.setBackground(Color.blue);
        	fc.setDialogTitle("Load a points file.");
        
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fc.getSelectedFile();
	            delaunayPanel.loadPoints(file);
	            //This is where a real application would open the file.
	            System.out.println("Opening: " + file.getName() + ".");
	        } else {
	            System.out.println("Open command cancelled by user.");
	        }
        }
        if(e.getSource() == savePointsButton){
        	int returnVal = fc.showSaveDialog(this);
        	fc.setBackground(Color.red);
        	fc.setDialogTitle("Save to a points file.");
        
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fc.getSelectedFile();
	            delaunayPanel.savePoints(file);
	            //This is where a real application would open the file.
	            System.out.println("Saving: " + file.getName() + ".");
	        } else {
	            System.out.println("Save command cancelled by user.");
	        }
        }
        if(e.getSource() == runRefineAlgorButton){
        	delaunayPanel.refine(((RefineAlgorithms)refinementSelector.getSelectedItem()).createAlgorithm());
        }
        delaunayPanel.repaint();
    }

    /**
     * If entering a mouse-entry switch then redraw the picture.
     */
    public void mouseEntered(MouseEvent e) {
        currentSwitch = e.getComponent();
        if (currentSwitch instanceof JLabel) delaunayPanel.repaint();
        else currentSwitch = null;
    }

    /**
     * If exiting a mouse-entry switch then redraw the picture.
     */
    public void mouseExited(MouseEvent e) {
        currentSwitch = null;
        if (e.getComponent() instanceof JLabel) delaunayPanel.repaint();
    }

    /**
     * If mouse has been pressed inside the delaunayPanel then add a new site.
     */
    public void mousePressed(MouseEvent e) {
        if (e.getSource() != delaunayPanel) return;
        if (debug ) {
        	Pnt point = new Pnt(e.getX(),e.getY());
        	System.out.println("Click " + point);
        }
        MouseModes mousemode = (MouseModes)mousemodeSelector.getSelectedItem();
        if( e.getButton() == MouseEvent.BUTTON1){
        	mousemode.onLeftClick(e.getX(), e.getY(), delaunayPanel);
        }else if ( e.getButton() == MouseEvent.BUTTON3){
        	mousemode.onRightClick(e.getX(), e.getY(), delaunayPanel);        	
        }
        delaunayPanel.repaint();
    }
    
	@Override
	public void mouseMoved(MouseEvent e) {
        if (e.getSource() != delaunayPanel) return;
        MouseModes mousemode = (MouseModes)mousemodeSelector.getSelectedItem();
        mousemode.onMouseMove(e.getX(), e.getY(), delaunayPanel);		
	}

    /**
     * Not used, but needed for MouseListener and MouseMotionListener.
     */
    public void mouseReleased(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}
	public void mouseDragged(MouseEvent e) {}


    /**
     * @return true iff the "colorful" box is selected
     */
    public boolean isColorful() {
        return colorfulBox.isSelected();
    }

    /**
     * @return true iff doing Voronoi diagram.
     */
    public boolean isVoronoi() {
        return voronoiButton.isSelected();
    }

    /**
     * @return true iff within circle switch
     */
    public boolean showingCircles() {
        return currentSwitch == circleSwitch;
    }

    /**
     * @return true iff within delaunay switch
     */
    public boolean showingDelaunay() {
        return currentSwitch == delaunaySwitch;
    }

    /**
     * @return true iff within voronoi switch
     */
    public boolean showingVoronoi() {
        return currentSwitch == voronoiSwitch;
    }
    
    public DelaunayAlgorithm getActiveAlgorithm(){
    	Algorithms selectedAlg = (Algorithms)algorithmSelector.getSelectedItem();
    	return selectedAlg.createAlgorithm();
    }

}

/**
 * Graphics Panel for DelaunayAp.
 */
@SuppressWarnings("serial")
class DelaunayPanel extends JPanel {

    public static Color voronoiColor = Color.magenta;
    public static Color delaunayColor = Color.green;
    public static int pointRadius = 3;
    
    private ImageIcon backgroundImage = null;	
    
    private DelaunayAp controller;              // Controller for DT
    private Triangulation dt;                   // Delaunay triangulation
    private Map<Object, Color> colorTable;      // Remembers colors for display
    private Triangle initialTriangle;           // Initial triangle
    private static int initialSize = 10000;     // Size of initial triangle
    private Graphics g;                         // Stored graphics context
    private Random random = new Random();       // Source of random numbers
    
    private Pnt lastPnt = null;					// Pnt which was last added. Required for making the boundary.
    private Pnt boundaryPointer = null;			// Pnt used to draw a pointer line while in boundary-builder mode.
    private boolean hooked = false;				// true if we are hooked to some point
    public static double hookRadius = 5;		// Radius within which a boundary will hook to an already-existing point.

    /**
     * Create and initialize the DT.
     */
    public DelaunayPanel (DelaunayAp controller) {
        this.controller = controller;
        initialTriangle = new Triangle(
                new Pnt(-initialSize, -initialSize),
                new Pnt( initialSize, -initialSize),
                new Pnt(           0,  initialSize));
        dt = new Triangulation(initialTriangle,controller.getActiveAlgorithm());
        colorTable = new HashMap<Object, Color>();
    }

	public void savePoints(File file) {
    	IO.savePoints(file, dt.obtainBoundary(), dt.obtainAllPoints());
		
	}

	public void loadPoints(File file) {
		IO.loadPoints(file, dt);
	}
	
	public void loadBackground(File file){
		backgroundImage = IO.createImageIcon(file.getAbsolutePath());
	}

	/**
     * Add a new site to the DT.
     * @param point the site to be added.
     */
    public void addSite(Pnt point) {
        if(!dt.delaunayPlace(point)) {
        	JOptionPane.showMessageDialog(this, 
        			"You should first create a boundary and then puts your points in that boundary", 
        			"Point out of boundary", 
        			JOptionPane.ERROR_MESSAGE);
        	// TODO nicer exit
        	//throw new RuntimeException("that doesn't work anymore");
        }
    }

    /**
     * Add a new boundary to the DT.
     * @author Kevin van As
     * @param point the site to be added.
     */
    public void addBoundarySite() {
    	if(lastPnt == null){
    		this.addSite(boundaryPointer); //Single point does not form a boundary.
    	}else{//Add the point to the triangulation:
            boolean success = dt.delaunayPlaceBoundary(boundaryPointer,lastPnt); //2 points -> part of a boundary
            if(!success) boundaryPointer = null;
    	}
    	lastPnt = boundaryPointer;
    }

	/**
	 * Executes the refinement algorithm, provided that a valid algorithm was selected.
	 * 
	 * @author Kevin van As
	 * @param selectedItem
	 */
    public void refine(DelaunayRefineAlgorithm alg) {
		//if(alg!=null)alg.refine(dt,20d/180*Math.PI,200); 
		//TODO: Currently a fixed criterion of 20 degrees and area of 200.
		dt.refine(dt, alg, 20d/180*Math.PI,200);
	}
    
    /**
     * Re-initialize the DT.
     */
    public void clear() {
        dt = new Triangulation(initialTriangle,controller.getActiveAlgorithm());
        lastPnt = null;
        boundaryPointer = null;
    }

    /**
     * Get the color for the spcified item; generate a new color if necessary.
     * @param item we want the color for this item
     * @return item's color
     */
    private Color getColor (Object item) {
        if (colorTable.containsKey(item)) return colorTable.get(item);
        Color color = new Color(Color.HSBtoRGB(random.nextFloat(), 1.0f, 1.0f));
        colorTable.put(item, color);
        return color;
    }

    /* Basic Drawing Methods */

    /**
     * Draw a point.
     * @param point the Pnt to draw
     */
    public void draw (Pnt point) {
        int r = pointRadius;
        int x = (int) point.coord(0);
        int y = (int) point.coord(1);
        g.fillOval(x-r, y-r, r+r, r+r);
    }

    /**
     * Draw a circle.
     * @param center the center of the circle
     * @param radius the circle's radius
     * @param fillColor null implies no fill
     */
    public void draw (Pnt center, double radius, Color fillColor) {
        int x = (int) center.coord(0);
        int y = (int) center.coord(1);
        int r = (int) radius;
        if (fillColor != null) {
            Color temp = g.getColor();
            g.setColor(fillColor);
            g.fillOval(x-r, y-r, r+r, r+r);
            g.setColor(temp);
        }
        g.drawOval(x-r, y-r, r+r, r+r);
    }

    /**
     * Draw a polygon.
     * @param polygon an array of polygon vertices
     * @param fillColor null implies no fill
     */
    public void draw (Pnt[] polygon, Color fillColor) {
        int[] x = new int[polygon.length];
        int[] y = new int[polygon.length];
        for (int i = 0; i < polygon.length; i++) {
            x[i] = (int) polygon[i].coord(0);
            y[i] = (int) polygon[i].coord(1);
        }
        if (fillColor != null) {
            Color temp = g.getColor();
            g.setColor(fillColor);
            g.fillPolygon(x, y, polygon.length);
            g.setColor(temp);
        }
        g.drawPolygon(x, y, polygon.length);
    }

    /**
     * Draw a line.
     * @author Kevin van As
     * @param p: array of size 2 containing two Pnt instances
     */
    public void drawLine(Pnt[] p) {
    	if(p.length != 2) throw new IllegalArgumentException("Must have exactly 2 points to draw a line.");
        g.drawLine((int)p[0].coord(0), (int)p[0].coord(1), (int)p[1].coord(0), (int)p[1].coord(1));
    }

    /**
     * Draw a line.
     * @author Kevin van As
     * @param p1: Point 1
     * @param p2: Point 2
     */
    public void drawLine(Pnt p1, Pnt p2) {
        g.drawLine((int)p1.coord(0), (int)p1.coord(1), (int)p2.coord(0), (int)p2.coord(1));
    }

    /* Higher Level Drawing Methods */

    /**
     * Handles painting entire contents of DelaunayPanel.
     * Called automatically; requested via call to repaint().
     * @param g the Graphics context
     */
    public void paintComponent (Graphics g) {
        super.paintComponent(g);
        this.g = g;
        Color temp;
        
        // Flood the drawing area with a "background" color
        temp = g.getColor();
        Color bgColor = null;
        if (!controller.isVoronoi()) bgColor = (delaunayColor);
        else if (dt.contains(initialTriangle)) bgColor = (this.getBackground());
        else g.setColor(voronoiColor);
        g.setColor(bgColor);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        g.setColor(temp);
        if(backgroundImage != null)
        	g.drawImage(backgroundImage.getImage(), 0, 0, this.getWidth(), this.getHeight(), bgColor, null);
        
        // If no colors then we can clear the color table
        if (!controller.isColorful()) colorTable.clear();

        // Draw the appropriate picture
        if (controller.isVoronoi())
            drawAllVoronoi(controller.isColorful(), true);
        else drawAllDelaunay(controller.isColorful());

        // Draw any extra info due to the mouse-entry switches
        temp = g.getColor();
        g.setColor(Color.white);
        if (controller.showingCircles()) drawAllCircles();
        if (controller.showingDelaunay()) drawAllDelaunay(false);
        if (controller.showingVoronoi()) drawAllVoronoi(false, false);
        g.setColor(temp);
        
        // Draw boundaryBuilder pointer
        if(boundaryPointer != null){
	        draw(boundaryPointer);
	        temp = g.getColor();
	        g.setColor(Color.white);
	        draw(boundaryPointer,hookRadius,null);
	        if(lastPnt != null)
		        drawLine(lastPnt,boundaryPointer);	  
	        g.setColor(temp);
        }
    }

    /**
     * Draw all the Delaunay triangles.
     * @param withFill true iff drawing Delaunay triangles with fill colors
     */
    public void drawAllDelaunay (boolean withFill) {
        for (Triangle triangle : dt) {
            Pnt[] vertices = triangle.toArray(new Pnt[0]);
            draw(vertices, withFill? getColor(triangle) : null);
            
            //Draw PSLG:

	        Color temp = g.getColor();
	        g.setColor(Color.white);
            for (Pnt vertex : triangle){
            	Set<Pnt> facet = triangle.facetOpposite(vertex);
            	if(dt.isPSLG(facet)){
            		drawLine(facet.toArray(new Pnt[0]));
            	}
            }
	        g.setColor(temp);
        }
    }

    /**
     * Draw all the Voronoi cells.
     * @param withFill true iff drawing Voronoi cells with fill colors
     * @param withSites true iff drawing the site for each Voronoi cell
     */
    public void drawAllVoronoi (boolean withFill, boolean withSites) {
        // Keep track of sites done; no drawing for initial triangles sites
        HashSet<Pnt> done = new HashSet<Pnt>(initialTriangle);
        for (Triangle triangle : dt)
            for (Pnt site: triangle) {
//            	Triangle.moreInfo = true;
//            	System.out.println("(DelaunayPanel: drawAllVoroni) " + triangle.toString() + "\t\t" + site.toString());
//            	Triangle.moreInfo = false;
            	
                if (done.contains(site)) continue;
                done.add(site);
                List<Triangle> list = dt.surroundingTriangles(site, triangle);
                Pnt[] vertices = new Pnt[list.size()];
                int i = 0;
                for (Triangle tri: list)
                    vertices[i++] = tri.getCircumcenter();
                draw(vertices, withFill? getColor(site) : null);
                if (withSites) draw(site);
            }
    }

    /**
     * Draw all the empty circles (one for each triangle) of the DT.
     */
    public void drawAllCircles () {
        // Loop through all triangles of the DT
        for (Triangle triangle: dt) {
            // Skip circles involving the initial-triangle vertices
            if (triangle.containsAny(initialTriangle)) continue;
            Pnt c = triangle.getCircumcenter();
            double radius = c.subtract(triangle.get(0)).magnitude();
            draw(c, radius, null);
        }
    }
    
    
    /**
     * @author Kevin van As
     */
    public void changeAlgorithm(DelaunayAlgorithm alg){
    	dt.changeAlgorithm(alg);
    }
    
    /**
     * 
     * 
     * @author Kevin van As
     * @param pnt
     */
	public void changeBoundaryPointer(Pnt pnt) {
		boundaryPointer = pnt;
		Pnt hookPnt = boundaryPointer.nearestPointInRadius(dt.obtainAllPoints(), hookRadius); //TODO: Must make a set of all points...
		if(hookPnt != null){
			boundaryPointer = hookPnt;
			hooked = true;
		}else{
			hooked = false;
		}
		this.repaint();
	}
    /**
     * 
     * 
     * @author Kevin van As
     * @param pnt
     */
	public void resetLastPnt() {
		lastPnt = null;
	}

	public void removeSite() {
		if(hooked)
			dt.delaunayRemove(boundaryPointer);
	}

}

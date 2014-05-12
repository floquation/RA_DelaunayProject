package GUI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;

import javax.swing.ImageIcon;

import triangulation.Pnt;
import triangulation.Triangulation;

public class IO {
	
	private static final boolean debug = false;
	

	public static void loadPoints(File file, Triangulation trilation){
		long time = System.nanoTime();
		try{
			FileReader fr = new FileReader(file);
			Scanner s = new Scanner(fr);
			double x1;
			double y1;
			double x2;
			double y2;
			
			//First, load the boundary:
			String str = s.nextLine();
			if(debug)System.out.println(str);
			s.useDelimiter("[,;]");
			if(debug)System.out.println("(IO) Loading boundary.");
			try{
				while(s.hasNext()){
					x1=Double.parseDouble(s.next());
					y1=Double.parseDouble(s.next());
					x2=Double.parseDouble(s.next());
					y2=Double.parseDouble(s.next());
					if(debug)System.out.println("Loading points: (" + x1 + ", "+ y1 + ") & (" + x2 + ", " + y2 + ");");
					trilation.delaunayPlaceBoundary(new Pnt(x1,y1), new Pnt(x2,y2));
					s.nextLine();
				}
			}catch(NumberFormatException e){}
			
			//Second, load all points and add them (note: nothing will happen if the site was already in the triangulation, so we need not care about that):
			str = s.nextLine();
			if(debug)System.out.println(str);
			if(debug)System.out.println("(IO) Loading points.");
			while(s.hasNext()){
				x1=Double.parseDouble(s.next());
				y1=Double.parseDouble(s.next());
				if(debug)System.out.println("Loading points: (" + x1 + ", "+ y1 + ");");
				trilation.delaunayPlace(new Pnt(x1,y1));
				s.nextLine();
			}
			
			
			s.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(debug) System.out.println("(IO) Elapsed loading time was: " + (System.nanoTime()-time)/1000000000d + " s.");
	}

	public static void savePoints(File file, Set<Set<Pnt>> boundary, Set<Pnt> pointList){
		try {
			//Write boundary-point file:
			FileWriter fw = new FileWriter(file,false); //overwrite entire file
			BufferedWriter bw = new BufferedWriter(fw);
			
			//First, store the boundary:
			bw.write("[Boundary Facets];");
			bw.newLine();
			for(Set<Pnt> facet : boundary){
				for(Pnt vertex : facet){
					bw.write(vertex.coord(0) + "," + vertex.coord(1) + ";");		
				}
				bw.newLine();
			}

			bw.newLine();
			//Second, store all points (including those contained in the boundary):
			bw.write("[All Points];");
			bw.newLine();
			for(Pnt vertex : pointList){
				bw.write(vertex.coord(0) + "," + vertex.coord(1) + ";");	
				bw.newLine();
			}
			
			bw.close();
			fw.close();
			
			//Write xPoints, yPoints file:
			File file2 = new File(file.getPath()+"2");
			fw = new FileWriter(file2,false); //overwrite entire file
			bw = new BufferedWriter(fw);

			//Second, store all points (including those contained in the boundary):
			bw.write("[xPoints];");
			bw.newLine();
			for(Pnt vertex : pointList){
				bw.write(vertex.coord(0) + ",");	
			}
			bw.newLine();
			bw.newLine();
			bw.write("[yPoints];");
			bw.newLine();
			for(Pnt vertex : pointList){
				bw.write(vertex.coord(1) + ",");	
			}
			
			bw.close();
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	 
    /** Returns an ImageIcon, or null if the path was invalid. */
    public static ImageIcon createImageIcon(String path) {
        ImageIcon icon = new ImageIcon(path);
        if(icon.getIconHeight() != -1)
        	return icon;
        return null;
    }
    /** Returns an ImageIcon, or null if the path was invalid. */
    public static ImageIcon createImageIcon(java.net.URL imgURL) {
    	try{
    		return new ImageIcon(imgURL);
    	}catch(NullPointerException e){
        	return null;
        }
    }
    
//    public static void main(String[] args){
//    	String path = "src/images/Open16.gif";
//    	
//    	ImageIcon img = createImageIcon(path);
//    	System.out.println("Image loaded successfully? : " + (img.getIconHeight()!=-1) );
//    }
}

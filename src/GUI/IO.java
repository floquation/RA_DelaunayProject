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

	public static void loadPoints(File file, Triangulation trilation){
		try{
			FileReader fr = new FileReader(file);
			Scanner s = new Scanner(fr);
			double x1;
			double y1;
			double x2;
			double y2;
			
			//First, load the boundary:
			s.nextLine();
			s.useDelimiter("[,;]");
			while(s.hasNextInt()){
				x1=s.nextDouble();
				y1=s.nextDouble();
				x2=s.nextDouble();
				y2=s.nextDouble();
				trilation.delaunayPlaceBoundary(new Pnt(x1,y1), new Pnt(x2,y2));
				s.nextLine();
			}
			
			//Second, load all poDoubles and add them (note: nothing will happen if it a site was already in the triangulation, so we need not care about that):
			s.nextLine();
			s.nextLine();
			while(s.hasNextDouble()){
				x1=s.nextDouble();
				y1=s.nextDouble();
				trilation.delaunayPlace(new Pnt(x1,y1));
				s.nextLine();
			}
			
			
			s.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void savePoints(File file, Set<Set<Pnt>> boundary, Set<Pnt> pointList){
		try {
			FileWriter fw = new FileWriter(file,false); //overwrite entire file
			BufferedWriter bw = new BufferedWriter(fw);
			
			//First, store the boundary:
			bw.write("[Boundary Facets]");
			bw.newLine();
			for(Set<Pnt> facet : boundary){
				for(Pnt vertex : facet){
					bw.write(vertex.coord(0) + "," + vertex.coord(1) + ";");		
				}
				bw.newLine();
			}

			bw.newLine();
			//Second, store all points (including those contained in the boundary):
			bw.write("[All Points]");
			bw.newLine();
			for(Pnt vertex : pointList){
				bw.write(vertex.coord(0) + "," + vertex.coord(1) + ";");	
				bw.newLine();
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

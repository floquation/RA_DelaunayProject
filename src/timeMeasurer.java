import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import GUI.IO;

import triangulation.Pnt;
import triangulation.Triangle;
import triangulation.Triangulation;
import triangulation.delaunay.algorithms.BowyerWatson;
import triangulation.delaunay.algorithms.Lawson;


public class timeMeasurer {
	
	private static final String fileName = "output";
	private static final String fileExt = "pnt";
	private static final boolean savePoints = false;
	
	public static void main(String[] args){
		//Init triangulation
        Triangle tri0 =
                new Triangle(new Pnt(-10000,10000), new Pnt(10000,10000), new Pnt(0,-10000));
           
        double[] domain = new double[]{10,10,1000,500};
        int N = (int)Math.pow(2, 12);
        int repeat = 40;
        double[] resultTimes = new double[repeat];
        
        for(int i =0; i<repeat; i++){
        	//re-init the triangulation
            Triangulation trilation = new Triangulation(tri0,new BowyerWatson());
	        
	        //Generate a set of random points
	        Set<Pnt> points = generateRandomPoints(N,domain);
	           
	        //System.out.println("<<Starting measurement>>");
	
	    	long time = System.nanoTime();
	    	  
	    	trilation.delaunayPlace(points);   	
	    	
	    	
			resultTimes[i] = (double)((System.nanoTime()-time)/1000000000d);
			System.out.println("<<Execution Time>> = " + resultTimes[i]);
			
			if(savePoints)IO.savePoints(new File(fileName + i + fileExt), trilation.obtainBoundary(), trilation.obtainAllPoints());
        }
        
        System.out.println("<<Done!>>");
        System.out.println(Arrays.toString(resultTimes));
	}
	
	private static Set<Pnt> generateRandomPoints(int N, double[] domain){
		Set<Pnt> points = new HashSet<Pnt>();
		
		for(int i = 0; i<N; i++){
			points.add(new Pnt(domain[0]+Math.random()*domain[2],domain[1]+Math.random()*domain[3]));
		}
		
		return points;
	}
	
}

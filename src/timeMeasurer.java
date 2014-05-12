import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import GUI.IO;

import triangulation.Pnt;
import triangulation.Triangle;
import triangulation.Triangulation;
import triangulation.delaunay.algorithms.*;
import triangulation.delaunay.refineAlgorithms.Ruppert;


/**
 * 
 * @author Kevin van As
 *
 */
public class timeMeasurer {
	
	private static final String fileName = "output";
	private static final String fileExt = ".pnt";
	private static final boolean savePoints = false;
	
	/**
	 * <<Delaunay triangulation>>
	 * 0 = Lawson
	 * 1 = BowyerWatson
	 * 2 = Lawson with PSLG1
	 * 3 = BowyerWatson with PSLG1
	 * <<Refinement>>
	 * 4 = Ruppert + Lawson with PSLG1
	 */
	private static int whichAlg = 3;
	
	
	public static void main(String[] args){
		
//Configuration code\\
		
        double[] domain = new double[]{10,10,1000,500};
        int N = (int)Math.pow(2, 12);
        int repeat = 100;
        
//Measurement code\\
        
        double[] resultTimes = new double[repeat];        
        if(whichAlg == 0){
	        for(int i =0; i<repeat; i++){
	        	//re-init the triangulation
	            Triangulation trilation = initializeTriangulation(new Lawson());
		        
		        //Generate a set of random points
		        Set<Pnt> points = generateRandomPoints(N,domain);
		           
		        //System.out.println("<<Starting measurement>>");
		
		    	long time = System.nanoTime();
		    	  
		    	trilation.delaunayPlace(points);   	
		    	
		    	
				resultTimes[i] = (double)((System.nanoTime()-time)/1000000000d);
				System.out.println("<<Execution Time>> = " + resultTimes[i]);
				
				if(savePoints)IO.savePoints(new File(fileName + i + fileExt), trilation.obtainBoundary(), trilation.obtainAllPoints());
	        }
		}else if(whichAlg == 1){
	        for(int i =0; i<repeat; i++){
	        	//re-init the triangulation
	            Triangulation trilation = initializeTriangulation(new BowyerWatson());
		        
		        //Generate a set of random points
		        Set<Pnt> points = generateRandomPoints(N,domain);
		           
		        //System.out.println("<<Starting measurement>>");
		
		    	long time = System.nanoTime();
		    	  
		    	trilation.delaunayPlace(points);   	
		    	
		    	
				resultTimes[i] = (double)((System.nanoTime()-time)/1000000000d);
				System.out.println("<<Execution Time>> = " + resultTimes[i]);
				
				if(savePoints)IO.savePoints(new File(fileName + i + fileExt), trilation.obtainBoundary(), trilation.obtainAllPoints());
	        }
		}else if(whichAlg == 2){
	        for(int i =0; i<repeat; i++){
	        	//re-init the triangulation
	            Triangulation trilation = initializeTriangulation(new Lawson());
		        
		        //Generate a set of random points
		        Set<Pnt> points = generateRandomPoints(N,domain);
		           
		        //System.out.println("<<Starting measurement>>");
		
		    	long time = System.nanoTime();
		    	  
		    	trilation.delaunayPlace(points);   	
		    	
		    	
				resultTimes[i] = (double)((System.nanoTime()-time)/1000000000d);
				System.out.println("<<Execution Time>> = " + resultTimes[i]);
				
				if(savePoints)IO.savePoints(new File(fileName + i + fileExt), trilation.obtainBoundary(), trilation.obtainAllPoints());
	        }
		}else if(whichAlg == 3){
	        for(int i =0; i<repeat; i++){
	        	//re-init the triangulation
	            Triangulation trilation = initializeTriangulation(new BowyerWatson());
		        
		        //Generate a set of random points
		        Set<Pnt> points = generateRandomPoints(N,domain);
		           
		        //System.out.println("<<Starting measurement>>");
		
		    	long time = System.nanoTime();
		    	  
		    	trilation.delaunayPlace(points);   	
		    	
		    	
				resultTimes[i] = (double)((System.nanoTime()-time)/1000000000d);
				System.out.println("<<Execution Time>> = " + resultTimes[i]);
				
				if(savePoints)IO.savePoints(new File(fileName + i + fileExt), trilation.obtainBoundary(), trilation.obtainAllPoints());
	        }
		}else if(whichAlg == 4){
	        for(int i =0; i<repeat; i++){
	        	//re-init the triangulation
	            Triangulation trilation = initializeTriangulation(new Lawson());
		        
		        //Refine using Ruppert:
		           
		        //System.out.println("<<Starting measurement>>");
		
		    	long time = System.nanoTime();
		    	 
		    	//trilation.refine(trilation, new Ruppert(), 20d/180*Math.PI,200);
		    	
		    	
				resultTimes[i] = (double)((System.nanoTime()-time)/1000000000d);
				System.out.println("<<Execution Time>> = " + resultTimes[i]);
				
				if(savePoints)IO.savePoints(new File(fileName + i + fileExt), trilation.obtainBoundary(), trilation.obtainAllPoints());
	        }
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
	
	private static Triangulation initializeTriangulation(DelaunayAlgorithm alg){
		//Init triangulation
        Triangle tri0 =
                new Triangle(new Pnt(-10000,10000), new Pnt(10000,10000), new Pnt(0,-10000));
		Triangulation trilation = new Triangulation(tri0,alg);
		

		if(whichAlg == 2 || whichAlg == 3 || whichAlg == 4) IO.loadPoints(new File("PSLG1.pnt"),trilation);
		
		
		return trilation;
	}
	
}

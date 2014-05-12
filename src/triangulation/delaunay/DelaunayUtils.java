package triangulation.delaunay;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import triangulation.Pnt;
import triangulation.Triangle;
import triangulation.Triangulation;

public abstract class DelaunayUtils {
	
	/**
	 * Checks whether two lines (in the form of a facet) intersect.
	 * Facets are sets of exactly two Pnts.
	 * 
	 * @author Kevin van As
	 * @param facet1 is the facet to be intersected with the other facet
	 * @param facet2 is the facet to be intersected with the other facet
	 * @param includeCorners is true if the corner of a segment is a part of the segment
	 * @return true iff the facets intersect.
	 */
	public static boolean intersect(Set<Pnt> facet1, Set<Pnt> facet2, boolean includeCorners){
		if(facet1.size() != 2 || facet2.size() != 2) 
			throw new IllegalArgumentException("Facet should have size 2");
		
		// Store facet1 in vertices[0, 1]
		// Store facet2 in vertices[2, 3]
		Pnt[] vertices = new Pnt[4];
		int i = 0;
		for(Pnt point : facet1){
			vertices[i++] = point;
		}
		for(Pnt point : facet2){
			vertices[i++] = point;
		}
		
		Pnt BmA = vertices[1].subtract(vertices[0]);
		Pnt PmA = vertices[2].subtract(vertices[0]);
		Pnt PmQ = vertices[2].subtract(vertices[3]);
		
		double det = BmA.cross_z(PmQ);
		double mu = BmA.cross_z(PmA)/det;
		double lamb = PmA.cross_z(PmQ)/det;
		
		return (includeCorners && (0 <= mu && mu <= 1 && 0 <= lamb && lamb <= 1)) ||
				(!includeCorners && (0 < mu && mu < 1 && 0 < lamb && lamb < 1));
	}
	
    /**
     * Determine the cavity caused by site.
     * @param site the site causing the cavity
     * @param triangle the triangle containing site
     * @return set of all triangles that have site in their circumcircle
     */
	public static Set<Triangle> getCavity (Pnt site, Triangle triangle, Triangulation trilation) {
        Set<Triangle> encroached = new HashSet<Triangle>();
        Queue<Triangle> toBeChecked = new LinkedList<Triangle>();
        Set<Triangle> marked = new HashSet<Triangle>();
        
        //Find the cavity:
        toBeChecked.add(triangle);
        marked.add(triangle);
        while (!toBeChecked.isEmpty()) {
            triangle = toBeChecked.remove();
            if (site.vsCircumcircle(triangle.toArray(new Pnt[0])) == 1)
                continue; // Site outside triangle => triangle not in cavity
            encroached.add(triangle); // Triangle in cavity.
            // Check the neighbors
            for (Triangle neighbor: trilation.neighbors(triangle)){
                if (marked.contains(neighbor)) continue;
                marked.add(neighbor);
                toBeChecked.add(neighbor);
            }
        }
        
        //TODO: Does not work with PSLG yet.
//        Set<Triangle> toBeRemoved = new HashSet<Triangle>();
//        //Remove the PSLG from the cavity:
//        for(Triangle tr_cav: encroached){
//        	for(Pnt vertex : tr_cav){
//        		Set<Pnt> facet = tr_cav.facetOpposite(vertex);
//        		if(trilation.isPSLG(facet)){
//        			//We found a PSLG facet in the cavity. Check if its interior or exterior:
//        			if(encroached.contains(trilation.neighborOpposite(vertex, tr_cav))){
//        				//Interior, since the opposite neighbour is inside the cavity as well!
//        				//Remove the triangle from the cavity, because it may not be altered.
//        				toBeRemoved.add(tr_cav);
//        			}
//        		}
//        	}
//        }
//        encroached.removeAll(toBeRemoved);
        
        return encroached;
    }
	
	/**
	 * Returns true if the triangle is locally Delaunay w.r.t. the point "site".
	 * 
	 * @author Kevin van As
	 * @param site
	 * @param triangle
	 * @return
	 */
	public static boolean localDelaunay(Pnt site, Triangle triangle){
		//True if "site" is outside "triangle":
		return site.vsCircumcircle(triangle.toArray(new Pnt[0])) == 1;
	}
	
	/**
	 * Returns a list of bad triangles, which have a minimum angle smaller than minAngle and/or
	 * a surface area greater than maxArea.
	 * 
	 * @author Kevin van As
	 * @param minAngle
	 * @param maxArea
	 * @return a Queue<Triangle> with the bad triangles.
	 */
	public static Queue<Triangle> obtainBadTriangles(Triangulation trilation, double minAngle, double maxArea){
		Queue<Triangle> queue = new LinkedList<Triangle>();
		
		Iterator<Triangle> it_triangle = trilation.iterator();
		while(it_triangle.hasNext()){
			Triangle triangle = it_triangle.next();
			if(triangle.containsAny(trilation.obtainInitialTriangle())) continue; //TODO: Check if not an external triangle
			double angle = triangle.getMinAngle();
			double area = triangle.getSurfaceArea();
			//System.out.println("(Utils) area = " + area);
			if(angle < minAngle || area > maxArea) queue.add(triangle); 
		}
		System.out.println("(Utils) size = " + queue.size());
		return queue;
	}
	
	
	
	public static void main(String[] args){
		Pnt pnt1 = new Pnt(10,10);
		Pnt pnt2 = new Pnt(10,20);
		Pnt pnt3 = new Pnt(15,30);
		Pnt pnt4 = new Pnt(5,5);
		
		Set<Pnt> facet1 = new HashSet<Pnt>();
		facet1.add(pnt2);
		facet1.add(pnt1);
		Set<Pnt> facet2 = new HashSet<Pnt>();
		facet2.add(pnt4);
		facet2.add(pnt3);
		
		System.out.println("Intersects? " + intersect(facet1,facet2,true));
		
	}
	
}

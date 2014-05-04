package triangulation;

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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import triangulation.delaunay.algorithms.BowyerWatson;
import triangulation.delaunay.algorithms.DelaunayAlgorithm;

/**
 * A 2D Delaunay Triangulation (DT) with incremental site insertion.
 *
 * This is not the fastest way to build a DT, but it's a reasonable way to build
 * a DT incrementally and it makes a nice interactive display. There are several
 * O(n log n) methods, but they require that the sites are all known initially.
 *
 * A Triangulation is a Set of Triangles. A Triangulation is unmodifiable as a
 * Set; the only way to change it is to add sites (via delaunayPlace).
 *
 * @author Paul Chew
 *
 * Created July 2005. Derived from an earlier, messier version.
 *
 * Modified November 2007. Rewrote to use AbstractSet as parent class and to use
 * the Graph class internally. Tried to make the DT algorithm clearer by
 * explicitly creating a cavity.  Added code needed to find a Voronoi cell.
 *
 */
public class Triangulation extends AbstractSet<Triangle> {
	
	private final static boolean debug = true;

    private Triangle mostRecent = null;      	// Most recently "active" triangle
    private Triangle initialTriangle;			// Initial triangle
    private Graph<Triangle> triGraph;        	// Holds triangles for navigation
    private Set<Pnt> pointList;					// List of all points
    private Set<Set<Pnt>> boundary_PSLG;		// Holds the facets which form a boundary and thus may not be altered
    private DelaunayAlgorithm algorithm = null;	// The algorithm to use for the triangulation
    
    private Pnt[] OuterBound = new Pnt[]{new Pnt(0,0), new Pnt(1000,1000)}; //OuterBound. No point may be outside. //TODO: Do this neatly. Best method is a point-eating-virus, but Lawson's DelaunayRemover method must work first.

    /**
     * All sites must fall within the initial triangle.
     * @param triangle the initial triangle
     */
    public Triangulation (Triangle triangle, DelaunayAlgorithm algorithmIn) {
    	algorithm = algorithmIn;
        triGraph = new Graph<Triangle>();
        boundary_PSLG = new HashSet<Set<Pnt>>();
        pointList = new HashSet<Pnt>();
        addToGraph(triangle);
        initialTriangle = triangle;
    }

    /* The following two methods are required by AbstractSet */

    @Override
    public Iterator<Triangle> iterator () {
        return triGraph.nodeSet().iterator();
    }

    @Override
    public int size () {
        return triGraph.nodeSet().size();
    }

    @Override
    public String toString () {
        return "Triangulation with " + size() + " triangles";
    }

    /**
     * True iff triangle is a member of this triangulation.
     * This method isn't required by AbstractSet, but it improves efficiency.
     * @param triangle the object to check for membership
     */
    public boolean contains (Object triangle) {
        return triGraph.nodeSet().contains(triangle);
    }
    
    /**
     * @author Kevin van As
     * @param alg
     */
    public void changeAlgorithm(DelaunayAlgorithm alg){
    	algorithm = alg;
    }

    /**
     * Report neighbor opposite the given vertex of triangle.
     * @param site a vertex of triangle
     * @param triangle we want the neighbor of this triangle
     * @return the neighbor opposite site in triangle; null if none
     * @throws IllegalArgumentException if site is not in this triangle
     */
    public Triangle neighborOpposite (Pnt site, Triangle triangle) {
        if (!triangle.contains(site))
            throw new IllegalArgumentException("Bad vertex; not in triangle");
        for (Triangle neighbor: triGraph.neighbors(triangle)) {
            if (!neighbor.contains(site)) return neighbor;
        }
        return null;
    }

    /**
     * Return the set of triangles adjacent to triangle.
     * @param triangle the triangle to check
     * @return the neighbors of triangle
     */
    public Set<Triangle> neighbors(Triangle triangle) {
        return triGraph.neighbors(triangle);
    }

    /**
     * Locate the triangle with point inside it or on its boundary.
     * @param point the point to locate
     * @return the triangle that holds point; null if no such triangle
     */
    public Triangle locate (Pnt point) {
        Triangle triangle = mostRecent;
        if (!this.contains(triangle)) triangle = null; //Triangle was removed from the triangulation for-some-reason.

        // Try a directed walk (this works fine in 2D, but can fail in 3D)
        Set<Triangle> visited = new HashSet<Triangle>();
        while (triangle != null) {
            if (visited.contains(triangle)) { // This should never happen
                System.out.println("Warning: Caught in a locate loop");
                break;
            }
            visited.add(triangle);
            // Corner opposite point
            Pnt corner = point.isOutside(triangle.toArray(new Pnt[0]));
            if (corner == null) return triangle;
            triangle = this.neighborOpposite(corner, triangle);
        }
        // No luck; try brute force
        System.out.println("Warning: Checking all triangles for " + point);
        for (Triangle tri: this) {
            if (point.isOutside(tri.toArray(new Pnt[0])) == null) return tri;
        }
        // No such triangle
        System.out.println("Warning: No triangle holds " + point);
        return null;
    }
    
    /**
     * Add multiple sites at the same time, e.g. when loading from a file
     * 
     * @author Kevin van As
     * @param sites
     */
    public void delaunayPlace(Set<Pnt> sites){
		for(Pnt site : sites){
			delaunayPlace(site);
		}
    }
    
    /**
     * 
     * Adds a single point to the triangulation
     * 
     * @author Kevin van As
     * @param site
     */
    public void delaunayPlace (Pnt site) {
    	if(pointList.contains(site)) return;
    	if(site.coord(0) < OuterBound[0].coord(0) || site.coord(0) > OuterBound[1].coord(0) || 
    			site.coord(1) < OuterBound[0].coord(1) || site.coord(1) > OuterBound[1].coord(1))
    		return;
    	algorithm.delaunayPlace(site,this);
    	pointList.add(site);  
    	
    	if(debug)isGraphStillCorrect("delaunayPlace");
    }
    
    /**
     * Adds a boundary facet to the triangulation.
     * 
     * @author Kevin van As
     * @param site: the new point
     * @param old_site: the starting point (a.k.a. anchor)
     * @return success? true if the boundary was successfully added to the triangulation.
     */
	public boolean delaunayPlaceBoundary(Pnt site, Pnt old_site) {
    	if(site.coord(0) < OuterBound[0].coord(0) || site.coord(0) > OuterBound[1].coord(0) || 
    			site.coord(1) < OuterBound[0].coord(1) || site.coord(1) > OuterBound[1].coord(1))
    		return false;
		if(old_site.equals(site)){
			return true;
		}
		if(!pointList.contains(old_site)){
			delaunayPlace(old_site);
		}
		
		//Add the boundary facet and the new point:		
		Set<Pnt> facet = new HashSet<Pnt>();
		facet.add(site);
		facet.add(old_site);
		System.out.println(facet.toString());
		boundary_PSLG.add(facet);
    	boolean success = algorithm.delaunayPlaceBoundary(site,old_site,this);
    	if(!success){
    		//Fail!
    		boundary_PSLG.remove(facet);
    	}else{
    		//Success!
        	pointList.add(site);    		
    	}
    	if(debug)isGraphStillCorrect("delaunayPlaceBoundary");
    	
    	return success;
	}
	
	/**
	 * Given a PSLG segment, the segment is split in half by adding the midpoint of the segment.
	 * 
	 * @author Kevin van As
	 * @param segment
	 */
	public void splitBoundary(Set<Pnt> segmentAB) {
		Pnt[] segmentAB_ar = segmentAB.toArray(new Pnt[0]);			
		Pnt pntC = segmentAB_ar[0].midPoint(segmentAB_ar[1]);
		//Remove the old segment from the PSLG:
		boundary_PSLG.remove(segmentAB);
		//Add the new segments:
		Set<Pnt> segmentAC = new HashSet<Pnt>();
		segmentAC.add(segmentAB_ar[0]);
		segmentAC.add(pntC);
		System.out.println("(Triangulation) Adding the segment to PSLG: " + segmentAC.toString());
		boundary_PSLG.add(segmentAC);
		Set<Pnt> segmentCB = new HashSet<Pnt>();
		segmentCB.add(segmentAB_ar[1]);
		segmentCB.add(pntC);
		System.out.println("(Triangulation) Adding the segment to PSLG: " + segmentCB.toString());
		boundary_PSLG.add(segmentCB);
		
    	pointList.add(pntC);
		algorithm.splitBoundary(segmentAB, this);
		
    	if(debug)isGraphStillCorrect("splitBoundary");
	}
	
	/**
	 * Removes the point from the triangulation.
	 * 
	 * @author Kevin van As
	 * @param site to be removed
	 */
	public void delaunayRemove(Pnt site) {
    	pointList.remove(site);    	
		algorithm.delaunayRemove(site,this);
		
    	if(debug)isGraphStillCorrect("delaunayRemove");
	}
	
	/**
	 * @author Kevin van As
	 * @param facet: Facet to be checked.
	 * @return true if `facet' belongs to a boundary and thus may not be altered. false otherwise.
	 */
	public boolean isPSLG(Set<Pnt> facet){
		return boundary_PSLG.contains(facet);
	}
	
	/**
	 * Method returns all points in the triangulation.
	 * The set may not be modified.
	 * The set may be used for drawing purposes.
	 * 
	 * @author Kevin van As
	 * @return all points in an unmodifiable set
	 */
	public Set<Pnt> obtainAllPoints(){
		return Collections.unmodifiableSet(pointList);
	}
	
	/**
	 * @author Kevin van As
	 * @return A set containing the first 3 initial points.
	 */
	public Triangle obtainInitialTriangle() {
		return initialTriangle;
	}

	/**
	 * @author Kevin van As
	 * @return a set of facets forming the boundary, in an unmodifiable set
	 */
	public Set<Set<Pnt>> obtainBoundary() {
		return Collections.unmodifiableSet(boundary_PSLG);
	}

    /**
	 * Report triangles surrounding site in order (cw or ccw).
	 * @param site we want the surrounding triangles for this site
	 * @param triangle a "starting" triangle that has site as a vertex
	 * @return all triangles surrounding site in order (cw or ccw)
	 * @throws IllegalArgumentException if site is not in triangle
	 */
	public List<Triangle> surroundingTriangles (Pnt site, Triangle triangle) {
	    if (!triangle.contains(site))
	        throw new IllegalArgumentException("Site not in triangle");
	    List<Triangle> list = new ArrayList<Triangle>();
	    Triangle start = triangle;
	    Pnt guide = triangle.getVertexButNot(site);        // Affects cw or ccw
	    while (true) {
	        list.add(triangle);
	        Triangle previous = triangle;
	        triangle = this.neighborOpposite(guide, triangle); // Next triangle
	        guide = previous.getVertexButNot(site, guide);     // Update guide
	        if (triangle == start) break;
	    }
	    return list;
	}
	

    /**
     * @author Kevin van As
     * @param triangle
     */
    public void addToGraph(Triangle triangle){
        triGraph.add(triangle);
        mostRecent = triangle;    	
    }
    
    /**
     * @author Kevin van As
     * @param t1
     * @param t2
     */
    public void addLinkToGraph(Triangle t1, Triangle t2){
        triGraph.addLink(t1,t2);  	
    }
    
    /**
     * @author Kevin van As
     * @param triangle
     */
    public void removeFromGraph(Triangle triangle){
    	triGraph.remove(triangle);
    }
	
    /**
     * Main program; used for testing.
     */
    public static void main (String[] args) {
        Triangle tri =
            new Triangle(new Pnt(-10,10), new Pnt(10,10), new Pnt(0,-10));
        System.out.println("Triangle created: " + tri);
        Triangulation dt = new Triangulation(tri,new BowyerWatson());
        System.out.println("DelaunayTriangulation created: " + dt);
        dt.delaunayPlace(new Pnt(0,0));
        dt.delaunayPlace(new Pnt(1,0));
        dt.delaunayPlace(new Pnt(0,1));
        System.out.println("After adding 3 points, we have a " + dt);
        Triangle.moreInfo = true;
        System.out.println("Triangles: " + dt.triGraph.nodeSet());
    }
    


	/**
	 * 
	 * DEBUG METHOD
	 * 
	 * @author Kevin van As
	 * @param trilation
	 */
	public void isGraphStillCorrect(String method){
		boolean graphIsCorrect = isGraphStillCorrect(method,false);
		System.out.println("(" + method + ") The graph is correct: " + graphIsCorrect);
	}

	/**
	 * 
	 * DEBUG METHOD
	 * 
	 * @author Kevin van As
	 * @param trilation
	 */
	public boolean isGraphStillCorrect(String method, boolean debug){
        Triangle.moreInfo = true;
		if(debug)System.out.println("(" + method + ") Starting a total graph validation.");
		boolean graphIsCorrect = true;
		for(Triangle triangle : triGraph.nodeSet()){
			Set<Triangle> NBs = this.neighbors(triangle);
			int numNBs = NBs.size();
			boolean correctNB = true;
			for(Triangle NB : NBs){
				correctNB = correctNB && NB.isNeighbor(triangle);
			}
			
			int numInitialVertices = 0;
			if(triangle.contains(initialTriangle.get(0))){
				numInitialVertices++;
			}
			if(triangle.contains(initialTriangle.get(1))){
				numInitialVertices++;
			}
			if(triangle.contains(initialTriangle.get(2))){
				numInitialVertices++;
			}
			if(numInitialVertices == 2)
				graphIsCorrect = graphIsCorrect && numNBs == 2;
			else if(numInitialVertices == 3)
				graphIsCorrect = graphIsCorrect && numNBs == 0;				
			else
				graphIsCorrect = graphIsCorrect && numNBs == 3;
			
			if(debug)System.out.println("Triangle " + triangle.toString() + " has " + numNBs + " neighbours in the graph.\tStill correct? " + graphIsCorrect);
			graphIsCorrect = graphIsCorrect && correctNB;
			if(debug)System.out.println("These neighbours are indeed neighbours: " + correctNB + ";\tStill correct? " + graphIsCorrect);
		}
		if(debug)System.out.println("(" + method + ") The graph is correct: " + graphIsCorrect);

        Triangle.moreInfo = false;
		return graphIsCorrect;
		
	}
}
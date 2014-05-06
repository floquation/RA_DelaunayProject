package triangulation.delaunay.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import triangulation.Pnt;
import triangulation.Triangle;
import triangulation.Triangulation;
import triangulation.delaunay.DelaunayUtils;
import triangulation.delaunay.FacetTrianglePair;

/**
 * 
 * Lawson's algorithm is an edge-swapping algorithm.
 * 
 * @author Kevin van As
 *
 */
public class Lawson implements DelaunayAlgorithm {
	
	private final static boolean debug = false;

    /**
     * Place a new site into the DT.
     * Nothing happens if the site matches an existing DT vertex.
     * @author Kevin van As
     * @param site the new Pnt
     * @throws IllegalArgumentException if site does not lie in any triangle
     */
	@Override
	public void delaunayPlace(Pnt site, Triangulation trilation) {
        // Uses straightforward scheme rather than best asymptotic time

        // Locate containing triangle
        Triangle triangle = trilation.locate(site);
        // Give up if no containing triangle or if site is already in DT
        if (triangle == null)
            throw new IllegalArgumentException("No containing triangle");
        if (triangle.contains(site)) return;
        
    	// Connect the new site to the vertices of the containing triangle
        Set<Triangle> newTriangles = new HashSet<Triangle>();
        for (Pnt vertex: triangle) {
            // First create the new triangle (one per vertex of the containing triangle):        	
            Set<Pnt> facet = triangle.facetOpposite(vertex); //Get one of the facets of the containing triangle
            facet.add(site); //Facet now is a new triangle.
            Triangle newTriangle = new Triangle(facet);
            newTriangles.add(newTriangle);
            trilation.addToGraph(newTriangle);
            
            // Now add the appropriate links:
            Triangle nb = trilation.neighborOpposite(vertex, triangle);
            if(nb!=null) trilation.addLinkToGraph(newTriangle, nb); //nb may be null if the site is outside the domain.
        }
        
        // Add the links to each other:
        for (Triangle newTriangle: newTriangles)
        	for (Triangle other: newTriangles)
    			trilation.addLinkToGraph(newTriangle, other);
        			
        // And remove the containing triangle, which no longer is a valid triangle (we inserted a site in its interior):
        trilation.removeFromGraph(triangle);
        	
        // Update the triangulation using an edge-flip algorithm, to make it Delaunay again:
        edgeFlip(site, trilation, newTriangles);		
	}

	/**
	 * 
	 * TODO:
	 * WARNING: Only works for `neat' cases.
	 * I.e., it will fail when the boundary intersects with lines of which their outline forms a concave polygon...
	 * 
	 * 
	 * @author Kevin van As
	 * @param 
	 * @return success?
	 */
	@Override
	public boolean delaunayPlaceBoundary(Pnt site, Pnt anchor, 
			Triangulation trilation) {
		
        // Locate containing triangle
        Triangle primary_triangle = trilation.locate(site);
        
        // Give up if no containing triangle
        if (primary_triangle == null)
            throw new IllegalArgumentException("No containing triangle");
        
        if(primary_triangle.contains(anchor)){
        	//The newly placed boundary does not intersect any triangles. Treat the site like a normal point.
        	delaunayPlace(site,trilation);
        	return true;
        }
        
        if (primary_triangle.contains(site)){
        	//The triangle already contains the site. So no new point is being added, just a boundary line.
        	//Lawson's job is done!
        	if(debug)System.out.println("(Lawson) Triangle already contains `site'?");
        	return true;
        }else{	
        	//Step 1: connect the new site
        	
        	// Connect the new site to the vertices of the containing triangle
            Set<Triangle> newTriangles = new HashSet<Triangle>();
            Set<Triangle> affectedTriangles = new HashSet<Triangle>();
            Set<Triangle> removableTriangles = new HashSet<Triangle>();
            for (Pnt vertex: primary_triangle) {
                // First create the new triangle (one per vertex of the containing triangle):        	
                Set<Pnt> facet = primary_triangle.facetOpposite(vertex); //Get one of the facets of the containing triangle
                facet.add(site); //Facet now is a new triangle.
                Triangle newTriangle = new Triangle(facet);
                newTriangles.add(newTriangle);
                affectedTriangles.add(newTriangle);
//                trilation.addToGraph(newTriangle);
//                
//                // Now add the appropriate links:
//                Triangle nb = trilation.neighborOpposite(vertex, primary_triangle);
//                if(nb!=null) trilation.addLinkToGraph(newTriangle, nb); //nb may be null if the site is outside the domain.
            }
            affectedTriangles.addAll(trilation.neighbors(primary_triangle));
            removableTriangles.add(primary_triangle);
            
            //Step 2: Find the troublesome facet (intersecting with the line connecting `anchor' and `site')
            Set<Pnt> facet_AB = new HashSet<Pnt>();
            facet_AB.add(anchor);
            facet_AB.add(site);
            
            Triangle triangle_previous = primary_triangle;
            Set<Pnt> facet_pq_previous = null;
            Set<Pnt> facet_pq = getIntersectingFacet(triangle_previous,facet_AB,facet_pq_previous);
            if(trilation.isPSLG(facet_pq)){
            	if(debug)System.out.println("(Lawson)(1) face is already PSLG: " + facet_pq.toString());
            	return false; //The input boundary does not satisfy PSLG: boundaries are overlapping.
            }
            
            //First remove the newly created triangle containing facet_pq...
            Triangle incorrectTriangle = null;
            int i;
            for(Triangle newTriangle : newTriangles){
            	i = 0;
            	for(Pnt vertex : newTriangle){
            		if(facet_pq.contains(vertex)){
            			i++;
	            		if(i == 2){ //If the triangle contains facet_pq
	            			incorrectTriangle = newTriangle;
	            			break;
            			}
            		}
            	}
            }
            if(debug)Triangle.moreInfo = true;
            if(debug)System.out.println("(Lawson) incorrectTriangle = " + incorrectTriangle.toString());
			newTriangles.remove(incorrectTriangle);
			affectedTriangles.remove(incorrectTriangle);
			incorrectTriangle = null;

            Pnt pnt_C = null;
			Set<Pnt> pnt_C_history = new HashSet<Pnt>(); //Stores all pnt_C's from the past
			Set<Set<Pnt>> facet_CDs = new HashSet<Set<Pnt>>(); //Stores all facets connecting C with D which have been generated. Used for checking intersection to detect when we need to connect C to A instead of to B.
	        for(Pnt vertex : facet_pq){
	        	pnt_C_history.add(vertex);
	        }
	        
			while(true){
            	facet_pq_previous = facet_pq;
	            
	            //Step 3: Find & Connect C (=vertex opposite to facet in adjacent triangle) with B (=site)
	            triangle_previous = trilation.neighborOpposite(triangle_previous.getVertexButNot(facet_pq_previous.toArray(new Pnt[0])), triangle_previous); //triangle_previous is now the new triangle.
	            facet_pq = getIntersectingFacet(triangle_previous,facet_AB,facet_pq_previous);
	            if(trilation.isPSLG(facet_pq_previous)){
	            	if(debug)System.out.println("(Lawson)(2) face is already PSLG: " + facet_pq.toString() + ";\t facet_pq_previous = " + facet_pq_previous.toString());
	            	return false; //The input boundary does not satisfy PSLG: boundaries are overlapping.
	            }
	            pnt_C = triangle_previous.getVertexButNot(facet_pq_previous.toArray(new Pnt[0]));
	            
	            //Now connect C to either A or B.
	            Set<Pnt> vertices = new HashSet<Pnt>();
	            Set<Pnt> facet_BC = new HashSet<Pnt>();
	            Pnt pnt_D = null;
	            for(Pnt vertex : triangle_previous){ //Find the third point for the triangle.
	            	if(!facet_pq.contains(vertex)){
	            		pnt_D = vertex;
	            		break;
	            	}
	            }
        		vertices.add(pnt_D);
	            vertices.add(pnt_C);
	            pnt_C_history.add(pnt_C);
	            pnt_C_history.add(pnt_D);
	            
	            //Check if C should be connected to B, otherwise connect to A:
        		facet_BC.add(pnt_C);
	            facet_BC.add(site);
	            
	            boolean connectToB = true;
	            for(Set<Pnt> old_facet : facet_CDs){
	            	if(DelaunayUtils.intersect(old_facet, facet_BC)){
	            		//Intersection detected: connect to A, not to B.
	            		connectToB = false;
	            		break;
	            	}
	            }

            	Set<Pnt> facet_CD = new HashSet<Pnt>();
            	facet_CD.add(pnt_C);
            	facet_CD.add(pnt_D);
	            facet_CDs.add(facet_CD);
	            if(connectToB){
	            	if(debug)System.out.println("(Lawson) Connect to B");
		            vertices.add(site);
	            }else{ //connectToA
	            	if(debug)System.out.println("(Lawson) Connect to A");
	            	vertices.add(anchor);
	            }

	            Triangle newTriangle = new Triangle(vertices);
	            newTriangles.add(newTriangle);
	            affectedTriangles.add(newTriangle);
	            affectedTriangles.addAll(trilation.neighbors(triangle_previous));
	            removableTriangles.add(triangle_previous);
	            
	            
	            
	            Triangle.moreInfo = true;
	            if(debug)System.out.println("(Lawson) site = " + site.toString() + ";\t anchor = " + anchor.toString() + ";\t C = " + pnt_C.toString() + ";\t size(vertices) = " + vertices.size());
	            if(debug)System.out.println("(Lawson) facet_pq = " + facet_pq.toString() + ";\t triangle_previous = " + triangle_previous.toString());
	            Triangle.moreInfo = false;
	            
	            //Step 4: C != A (=anchor) --> goto Step2, otherwise continue to step 5
	            if(pnt_C.equals(anchor)){
	            	//We have arrived at A. We still miss one or two triangles in the triangulation:
	            	//the triangle connecting A, B, and the missing vertex (stored in `pnt_C_prime'):
	            	
	            	pnt_C = triangle_previous.getVertexButNot(vertices.toArray(new Pnt[0]));
	            	vertices = new HashSet<Pnt>();
	            	vertices.add(anchor);
	            	vertices.add(site);
	            	vertices.add(pnt_C);
	            	newTriangle = new Triangle(vertices);
	            	if(!newTriangles.contains(newTriangle)){
			            newTriangles.add(newTriangle);	
			            affectedTriangles.add(newTriangle);     
			            if(debug)System.out.println("(Lawson) EXTRA triangle:");
			            if(debug)System.out.println("(Lawson) vertices = " + vertices.toString());       		
	            	}
	            	/*
	            	//To solve this, find that vertex that does not intersect with any of the `facet_CDs' when it is connected with both A and B.
	            	pnt_C_history.remove(anchor); //to be sure
	            	pnt_C_history.remove(site); //to be sure
	            	boolean isTheOne;
	            	System.out.println("(Lawson) pnt_C_history = " + pnt_C_history.toString());
	            	for(Pnt vertex : pnt_C_history){
	            		isTheOne = true;
	            		//Check C to site:
	            		facet_BC = new HashSet<Pnt>();
	            		facet_BC.add(vertex);
	            		facet_BC.add(site);
	    	            for(Set<Pnt> old_facet : facet_CDs){
	    	            	System.out.println("(Lawson) trying to intersect " + vertex.toString() + " with " + old_facet.toString());
	    	            	if(!old_facet.contains(vertex) && DelaunayUtils.intersect(old_facet, facet_BC)){
	    	            		System.out.println("(Lawson) result: intersection");
	    	            		isTheOne = false;
	    	            		break;
	    	            	}
	    	            }
	    	            //Check C to anchor:
	            		facet_BC = new HashSet<Pnt>();
	            		facet_BC.add(vertex);
	            		facet_BC.add(anchor);
	    	            for(Set<Pnt> old_facet : facet_CDs){
	    	            	System.out.println("(Lawson) trying to intersect " + vertex.toString() + " with " + old_facet.toString());
	    	            	if(!old_facet.contains(vertex) && DelaunayUtils.intersect(old_facet, facet_BC)){
	    	            		System.out.println("(Lawson) result: intersection");
	    	            		isTheOne = false;
	    	            		break;
	    	            	}
	    	            }
	    	            if(isTheOne){
	    	            	System.out.println("(Lawson) this point isTheOne: " + vertex.toString());
	    	            	//Connect vertex-site-anchor:
	    	            	vertices = new HashSet<Pnt>();
	    	            	vertices.add(anchor);
	    	            	vertices.add(site);
	    	            	vertices.add(vertex);
	    	            	newTriangle = new Triangle(vertices);
	    		            if(!newTriangles.containsAll(newTriangle)){ //Check if the triangle already existed
	    		            	newTriangles.add(newTriangle);
		    		            affectedTriangles.add(newTriangle);
	    		            }
	    	            }//Else : try the next vertex.
	            	}
	            	*/
	            	
	            	
	            	break; //goto 5
	            }
	            
	            //Prepare next loop-iteration
	            facet_pq_previous = facet_pq;
            }
            
            // Step: update the triangulation


			// Remove the old triangles:
			for (Triangle oldTriangle: removableTriangles){
				trilation.removeFromGraph(oldTriangle);
				affectedTriangles.remove(oldTriangle);
			}
			
            // Add all new triangles:
            for (Triangle newTriangle: newTriangles)
            	trilation.addToGraph(newTriangle);
			
            // Add the links to each other:
            for (Triangle newTriangle: newTriangles)
            	for (Triangle other: affectedTriangles)
                    if (other != null && newTriangle.isNeighbor(other))
                    	trilation.addLinkToGraph(newTriangle, other);
           
            if(debug)System.out.println("(Lawson) the newTriangles are: " + newTriangles.toString());
            
            trilation.isGraphStillCorrect("Lawson place boundary - before edge-flip",true);
            
            if(debug)System.out.println("Start edge-flip");
            //Step 5: Run edge-swap algorithm on all newly created facets + the boundary.
			Queue<FacetTrianglePair> facetQueue = new LinkedList<FacetTrianglePair>();
	    	Set<Set<Pnt>> marked = new HashSet<Set<Pnt>>(); //facet storage
	    	//Initial facets:
	    	for (Triangle triangle: newTriangles){
	    		for (Pnt vertex : triangle){
		        	Set<Pnt> facet = triangle.facetOpposite(vertex);
		        	if(!marked.contains(facet)){
			        	facetQueue.add(new FacetTrianglePair(facet,triangle));
			        	marked.add(facet);
		        	}
	    		}
	        }
            edgeFlip(trilation,facetQueue);
            if(debug)System.out.println("Finish edge-flip");
            
        }
        
        return true;
		
	}
	
	/**
	 * Obtain the intersecting facet.
	 * 
	 * @author Kevin van As
	 * @param triangle: triangle to be intersected by facet_AB
	 * @param facet_AB: line to check intersection with
	 * @param facet_pq_previous: exclude this facet
	 * @return the facet of triangle which intersects facet_AB, but excluding facet_pq_previous. null if there is none.
	 */
	private Set<Pnt> getIntersectingFacet(Triangle triangle, Set<Pnt> facet_AB, Set<Pnt> facet_pq_previous){
		Set<Pnt> facet_pq = null;
        for (Pnt vertex: triangle) {
        	facet_pq = triangle.facetOpposite(vertex);
        	if(!facet_pq.equals(facet_pq_previous)) //Do not move back, only forward.
        		if(DelaunayUtils.intersect(facet_pq, facet_AB)) break; //facet_pq now contains the troublesome facet
        }
        return facet_pq;
	}


	
	/**
     * Update the triangulation using an edge-flip algorithm.
     * 
     * @author Kevin van As
     * @param site the site that created the cavity
     * @param trilation Link to the Triangulation class
     * @param newTriangles all altered triangles. Each of those triangle's facets will be marked
     */
	private void edgeFlip(Pnt site, Triangulation trilation, Set<Triangle> newTriangles){

    	Queue<FacetTrianglePair> toBeChecked = new LinkedList<FacetTrianglePair>(); //facet + adjacent triangle storage
    	Set<Set<Pnt>> marked = new HashSet<Set<Pnt>>(); //facet storage
    	//Initial facets:
    	for (Triangle triangle: newTriangles){
        	Set<Pnt> facet = triangle.facetOpposite(site);
        	if(!marked.contains(facet)){
	        	toBeChecked.add(new FacetTrianglePair(facet,triangle));
	        	marked.add(facet);
        	}
        }
    	
    	edgeFlip(trilation,toBeChecked);
	}
	
	/**
	 * TODO: not-BUGGED. SOMETIMES MESSES UP THE GRAPH STRUCTURE. (e.g. triangles have 1, or 4 neighbours, while they should have 3.)
	 * 
     * Update the triangulation using an edge-flip algorithm.
     * 
     * @author Kevin van As
     * @param trilation: Link to the Triangulation class
     * @param toBeChecked: a queue with unique facets which must be checked for flipping
     */
    private void edgeFlip (Triangulation trilation, Queue<FacetTrianglePair> toBeChecked) {
        //Now start the edge-flipping algorithm:
    	
        /*Queue<Facet> toBeChecked = new LinkedList<Facet>();
        Set<Facet> marked = new HashSet<Facet>();
        for (Triangle newTriangle: newTriangles){
        	Facet facet1 = new Facet(site,newTriangle);
        	Triangle oppositeTriangle = trilation.neighborOpposite(site, newTriangle);
        	Facet facet2 = new Facet(newTriangle.)
        	marked.add(facet);
        }*/
    	
    	Set<Set<Pnt>> marked = new HashSet<Set<Pnt>>(); //facet storage
    	for (FacetTrianglePair triangle: toBeChecked){
        	marked.add(triangle.facet);
        }
    	
    	while(!toBeChecked.isEmpty()){
    		//Pop next facet:
    		FacetTrianglePair pair_x = toBeChecked.remove();
    		Set<Pnt> facet_x = pair_x.facet;
    		Triangle triangle_x = pair_x.triangle;
    		/*unmark*/ marked.remove(facet_x);
    		
//    		System.out.println("FacetTrianglePair = correct? " + triangle_x.containsAll(facet_x));
//    		System.out.println("FacetTrianglePair in graph? " + trilation.contains(triangle_x));
    		
    		//Check if the facet may be flipped (i.e., not part of the PSLG)
    		if(trilation.isPSLG(facet_x)) continue;
    		
    		//Check if locally Delaunay:
    		Pnt pnt = triangle_x.getVertexButNot(facet_x.toArray(new Pnt[0]));
    		Triangle triangle_opp = trilation.neighborOpposite(pnt, triangle_x);
    		if(triangle_opp == null){
    			continue;
    		}
//    		Triangle.moreInfo=true;
//    		System.out.println("(Lawson) triangle_x = " + triangle_x.toString());
//    		System.out.println("(Lawson) triangle_opp = " + triangle_opp.toString());
    		Pnt pnt_opp = triangle_opp.getVertexButNot(facet_x.toArray(new Pnt[0]));
    		if(!DelaunayUtils.localDelaunay(pnt_opp,triangle_x)){
    			//Find the affected triangles for neighbour setting:
    			Set<Triangle> affectedTriangles = new HashSet<Triangle>();
    			affectedTriangles.addAll(trilation.neighbors(triangle_x));
    			affectedTriangles.addAll(trilation.neighbors(triangle_opp));
    			affectedTriangles.remove(triangle_x);
    			affectedTriangles.remove(triangle_opp);
    			
//    			remove facet x from trilation
//    			add facet y to trilation
    			
    			//Define facet y:
    			Set<Pnt> facet_y = new HashSet<Pnt>();
    			facet_y.add(pnt);
    			facet_y.add(pnt_opp);
    			    			
    			//Edge-flip:
    			Set<Triangle> newTriangles = new HashSet<Triangle>();
    			Set<Pnt> newTrianglePnts;
    			for(Pnt x_pnt : facet_x){
    				newTrianglePnts = new HashSet<Pnt>(facet_y);
    				newTrianglePnts.add(x_pnt);
        			Triangle triangle_repl = new Triangle(newTrianglePnts);
        			trilation.addToGraph(triangle_repl);
        			affectedTriangles.add(triangle_repl);
        			newTriangles.add(triangle_repl);
        			if(debug)System.out.println("(Lawson) triangle_repl = " + triangle_repl.toString());
    			}

    			if(debug)System.out.println("(Lawson) facet_x = " + facet_x.toString());
    			if(debug)System.out.println("(Lawson) facet_y = " + facet_y.toString());
    			if(debug)System.out.println("(Lawson) triangle_x = " + triangle_x.toString());
    			if(debug)System.out.println("(Lawson) triangle_opp = " + triangle_opp.toString());
    			//Remove the old triangles, which disappeared thanks to the edge-flip:
    			trilation.removeFromGraph(triangle_x);
    			trilation.removeFromGraph(triangle_opp);
    			
    	    	// Update the graph links for each new triangle
//    	        for (Triangle t1: affectedTriangles)
//    	            for (Triangle t2: newTriangles)
//    	                if (t1.isNeighbor(t2))
//    	                    trilation.addLinkToGraph(t1, t2);
//    	        for (Triangle t1: newTriangles)
//    	            for (Triangle t2: newTriangles)
//	                    trilation.addLinkToGraph(t1, t2);

    	        for (Triangle t1: newTriangles)
    	            for (Triangle t2: affectedTriangles)
    	                if (t1.isNeighbor(t2))
    	                    trilation.addLinkToGraph(t1, t2);
    	        
                boolean result = trilation.isGraphStillCorrect("Edge-Flip",false);
             //   if(!result) System.exit(0);
                
//    			push facet d, e, f and g (IF UNMARKED) and mark them.
//				if one of them IS marked, update the adjacent triangle to the appropriate new triangle.
    	        
    	        //Find {d,e,f,g} as the boundary of the two newTriangles.
	            for (Triangle triangle: newTriangles) {
	                for (Pnt vertex: triangle) {
	                    Set<Pnt> facet = triangle.facetOpposite(vertex);
	                    if(!facet.equals(facet_y)){
	                    	//Not the interior facet
	    	            	if(marked.contains(facet)){
	    	            		//Already marked, update its adjacent triangle:
	    	            		//First find the facet in the toBeChecked list...:
	    	            		for (FacetTrianglePair pair : toBeChecked) {
	    	            	        if (pair.facet.equals(facet)){
	    	            	        	pair.triangle = triangle;
	    	            	        	break;
	    	            	        }
	    	            	    }
	    	            	}else{
	    	            		//Not yet marked: mark it and add it to the to-do-list:
	    	            		marked.add(facet);
	    	                	toBeChecked.add(new FacetTrianglePair(facet,triangle));	    	            		
	    	            	}	                    	
	                    }
	                }
	            }
	            
    	        
    		}//else locally delaunay
    	}
    	
    }

	@Override
	public void splitBoundary(Set<Pnt> segmentAB, Triangulation trilation) {
		Pnt[] segmentAB_ar = segmentAB.toArray(new Pnt[0]);			
		Pnt pntC = segmentAB_ar[0].midPoint(segmentAB_ar[1]);
		
		//Add the new segments:
		Set<Pnt> segmentAC = new HashSet<Pnt>();
		segmentAC.add(segmentAB_ar[0]);
		segmentAC.add(pntC);

        Triangle primary_triangle = trilation.locate(segmentAB_ar[0]);

        List<Triangle> surTriangles = trilation.surroundingTriangles(segmentAB_ar[0], primary_triangle);
        Triangle[] adjTriangles = new Triangle[2];
        int i = 0;
        for(Triangle triangle : surTriangles){
        	if(triangle.contains(segmentAB_ar[1])){
        		adjTriangles[i++] = triangle;
        	}
        }
        
        
        System.out.println("(Lawson) There are " + adjTriangles.length + " adjacent triangles.");
        Triangle.moreInfo = true;
        System.out.println(adjTriangles[0].toString());
        System.out.println(adjTriangles[1].toString());
        
        //Define the four new adjacent triangles, after splitting the boundary:
        Triangle triangle;
        Set<Triangle> newTriangles = new HashSet<Triangle>();
        Pnt pntD = adjTriangles[0].getVertexButNot(segmentAB_ar);
        newTriangles.add(triangle = new Triangle(pntD,pntC,segmentAB_ar[0]));
        trilation.addToGraph(triangle);
        newTriangles.add(triangle = new Triangle(pntD,pntC,segmentAB_ar[1]));
        trilation.addToGraph(triangle);
        pntD = adjTriangles[1].getVertexButNot(segmentAB_ar);
        newTriangles.add(triangle = new Triangle(pntD,pntC,segmentAB_ar[0]));
        trilation.addToGraph(triangle);
        newTriangles.add(triangle = new Triangle(pntD,pntC,segmentAB_ar[1]));
        trilation.addToGraph(triangle);
        
        // Add the links to each other:
        Set<Triangle> affectedTriangles = new HashSet<Triangle>();
        affectedTriangles.addAll(trilation.neighbors(adjTriangles[0]));
        affectedTriangles.addAll(trilation.neighbors(adjTriangles[1]));
        affectedTriangles.addAll(newTriangles);
        
        trilation.removeFromGraph(adjTriangles[0]);
        trilation.removeFromGraph(adjTriangles[1]);        
        affectedTriangles.remove(adjTriangles[0]);
        affectedTriangles.remove(adjTriangles[1]);
        
        for (Triangle newTriangle: newTriangles)
        	for (Triangle other: affectedTriangles)
        		if(newTriangle.isNeighbor(other))
        			trilation.addLinkToGraph(newTriangle, other);
        
        //Edge-flip the new vertices:
        edgeFlip(pntC,trilation,newTriangles);
	}
	
	//@Override
	public void new_delaunayRemove(Pnt site, Triangulation trilation) {
		// One of the triangles which contains site
		Triangle primary_triangle = trilation.locate(site);
		
		// All triangles that contain site
		List<Triangle> surTriangles = trilation.surroundingTriangles(site, primary_triangle);
		
		// When site is removed,
		// the remaining points form a polygon that needs to be triangulated
		List<Pnt> polygonPoints = new ArrayList<Pnt>();
		Pnt previousPoint = site;
		for(Triangle t: surTriangles) {
			Pnt nextPoint = t.getVertexButNot(previousPoint, site);
			polygonPoints.add(nextPoint);
			previousPoint = nextPoint;
		}
		
		// Triangulate that polygon using an ear-clipping method
		int index = 0;
		
		while(!polygonPoints.isEmpty()) {
			// Just take some points
			int n = polygonPoints.size();
			Pnt points[] = new Pnt[] {
					polygonPoints.get(index % n), 
					polygonPoints.get((index + 1) % n), 
					polygonPoints.get((index + 2) % n)};
			
			// Now we want to know if these points make an ear
			// According to http://stackoverflow.com/questions/2816572/diagonal-of-polygon-is-inside-or-outside
			// p1.x * p2.y + p2.x * p3.y + p3.x * p1.y - p2.x * p1.y - p3.x * p2.y - p1.x * p3.y
			// That looks a bit like the sum of a cross product to me. so let's do that
			
			// First the points are transposed
			Pnt x = new Pnt(points[0].coord(0), points[1].coord(0), points[2].coord(0));
			Pnt y = new Pnt(points[0].coord(1), points[1].coord(1), points[2].coord(1));
			Pnt cross = x.cross(y);
			double sum = cross.coord(0) + cross.coord(1) + cross.coord(2);
			boolean isEar = sum > 0;
			
			// If it's an ear, we can remove it..
			if(isEar) {
				;
			}
			
			// vertices[1] is an ear if 
			// the line between (vertices[0], vertices[1]) crosses the polygon
		}
		
		// I guess I have an idea how to calculate the new triangles.
		// How do I add them??
		
		// The code seems pretty messed up
		//
		
		
	}

	/**
	 * TODO: BUGGED. Sometimes generates incorrect triangles (negative surface)
	 */
	@Override
	public void delaunayRemove(Pnt site, Triangulation trilation) {
		Triangle primary_triangle = trilation.locate(site);
		System.out.println("(Lawson) site = " + site.toString() + ",\t triangle1 = " + primary_triangle.toString());
		List<Triangle> surTriangles = trilation.surroundingTriangles(site, primary_triangle);
		
		Pnt anchor = primary_triangle.getVertexButNot(site);
		
		Set<Triangle> newTriangles = new HashSet<Triangle>();
		Set<Triangle> affectedTriangles = new HashSet<Triangle>();
		
		//To remove a point, we will connect all neighbours to some anchor and then run the edge-flipping algorithm.
		for(Triangle triangle : surTriangles){
			Set<Pnt> vertices = triangle.facetOpposite(site);
			affectedTriangles.addAll(trilation.neighbors(triangle));
			if(vertices.contains(anchor))continue;
			vertices.add(anchor);
			System.out.println("(Lawson) vertices = " + vertices.toString() + ";\t anchor = " + anchor.toString());
			Triangle newTriangle = new Triangle(vertices);
			newTriangles.add(newTriangle);
			trilation.addToGraph(newTriangle);
			affectedTriangles.add(newTriangle);
		}
		for(Triangle triangle : surTriangles){
			Triangle.moreInfo=true;
			System.out.println("(Lawson) removing triangle : " + triangle.toString());
			Triangle.moreInfo=false;
			trilation.removeFromGraph(triangle);
			affectedTriangles.remove(triangle);
		}
		

		Triangle.moreInfo=true;
		System.out.println("(Lawson) newTriangles : " + newTriangles.toString());
		System.out.println("(Lawson) affectedTriangles : " + affectedTriangles.toString());
		Triangle.moreInfo=false;
		
        // Add the links to each other:
        for (Triangle newTriangle: newTriangles)
        	for (Triangle other: affectedTriangles)
        		if(other != null && newTriangle.isNeighbor(other) && !newTriangle.containsAll(other))
        			trilation.addLinkToGraph(newTriangle, other);
        
        
        //Edge-flip the new vertices:
    	Queue<FacetTrianglePair> toBeChecked = new LinkedList<FacetTrianglePair>(); //facet + adjacent triangle storage
    	Set<Set<Pnt>> marked = new HashSet<Set<Pnt>>(); //facet storage
    	//Initial facets:
    	for (Triangle triangle: newTriangles){
    		for(Pnt vertex: triangle){
            	Set<Pnt> facet = triangle.facetOpposite(vertex);
            	if(!marked.contains(facet)){
    	        	toBeChecked.add(new FacetTrianglePair(facet,triangle));
    	        	marked.add(facet);
            	}    			
    		}
        }
    	
    	System.out.println("(Lawson) Graph is correct pre-edge-flip?");
    	trilation.isGraphStillCorrect("Lawson remover", true);
    	
        edgeFlip(trilation,toBeChecked);
	}
	
	// Tests removing a point from a convex polygon
	public static void debugRemoval() {
		// This constructor is obviously  insane
		Triangle initialTriangle = new Triangle(Arrays.asList(
				new Pnt(-10000, -1000),
				new Pnt(0, 1000),
				new Pnt(1000, 0)));
		Triangulation trilation = new Triangulation(initialTriangle, new Lawson());
		
		// Construct a convex polygon
		Pnt malware = new Pnt(0, 3);
		trilation.delaunayPlace(new Pnt(0, 5));  // top
		trilation.delaunayPlace(new Pnt(-2, 0)); // left below
		trilation.delaunayPlace(malware);
		trilation.delaunayPlace(new Pnt(0, 1));  // bottom
		trilation.delaunayPlace(new Pnt(2, 0));  // right below
		trilation.isGraphStillCorrect("debugRemoval - Before removal");
		
		// Omg, I can't believe you did that!?
		trilation.delaunayRemove(malware);
		trilation.isGraphStillCorrect("debugRemoval - After removal");
	}
	
    public static void main(String[] args){
    	debugRemoval();
    	
    	/*
    	//Check if differently ordered set is identical:
    	Pnt pnt1 = new Pnt(10, 10);
    	Pnt pnt2 = new Pnt(30, -10);
    	Set<Pnt> facet1 = new HashSet<Pnt>();
    	facet1.add(pnt1);
    	facet1.add(pnt2);
    	Set<Pnt> facet2 = new HashSet<Pnt>();
    	facet2.add(pnt2);
    	facet2.add(pnt1);
    	
    	System.out.println("facet1 == facet2? " + facet1.equals(facet2));
    	*/
    }
    
}

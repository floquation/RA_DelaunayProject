package triangulation.delaunay.algorithms;

import java.util.HashSet;
import java.util.Set;

import triangulation.Pnt;
import triangulation.Triangle;
import triangulation.Triangulation;
import triangulation.delaunay.DelaunayUtils;

/**
 * 
 * @author Kevin van As
 *
 */
public class BowyerWatson implements DelaunayAlgorithm {

    /**
     * Place a new site into the DT.
     * Nothing happens if the site matches an existing DT vertex.
     * @author Paul Chew
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

        // Determine the cavity and update the triangulation
        Set<Triangle> cavity = DelaunayUtils.getCavity(site, triangle, trilation);
        update(site, cavity, trilation);		
	}

	@Override
	public boolean delaunayPlaceBoundary(Pnt site, Pnt anchor,
			Triangulation trilation) {
        //TODO: Does not work with PSLG yet.
		return false;
	}

	/**
     * Update the triangulation by removing the cavity triangles and then
     * filling the cavity with new triangles.
     * @author Paul Chew
     * @param site the site that created the cavity
     * @param cavity the triangles with site in their circumcircle
     */
    private void update (Pnt site, Set<Triangle> cavity, Triangulation trilation) {
        Set<Set<Pnt>> boundary = new HashSet<Set<Pnt>>();
        Set<Triangle> theTriangles = new HashSet<Triangle>();

        // Find boundary facets and adjacent triangles
        for (Triangle triangle: cavity) {
            theTriangles.addAll(trilation.neighbors(triangle));
            for (Pnt vertex: triangle) {
                Set<Pnt> facet = triangle.facetOpposite(vertex);
                if (boundary.contains(facet)) boundary.remove(facet); //This will trigger twice for interior facets, causing the net effect to be their removal.
                else boundary.add(facet);
            }
        }
        theTriangles.removeAll(cavity);        // Adj triangles only

        // Remove the cavity triangles from the triangulation
        for (Triangle triangle: cavity) trilation.removeFromGraph(triangle);

        // Build each new triangle and add it to the triangulation: Bowyer/Watson algorithm.
        Set<Triangle> newTriangles = new HashSet<Triangle>();
        for (Set<Pnt> vertices: boundary) {
            vertices.add(site);
            Triangle tri = new Triangle(vertices);
            trilation.addToGraph(tri);
            newTriangles.add(tri);
        }

        // Update the graph links for each new triangle
        theTriangles.addAll(newTriangles);    // Adj triangle + new triangles
        for (Triangle triangle: newTriangles)
            for (Triangle other: theTriangles)
                if (triangle.isNeighbor(other))
                    trilation.addLinkToGraph(triangle, other);
        
        
    }

	@Override
	public void splitBoundary(Set<Pnt> segment, Triangulation trilation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delaunayRemove(Pnt site, Triangulation trilation) {
		// TODO Auto-generated method stub
		
	}
	
}

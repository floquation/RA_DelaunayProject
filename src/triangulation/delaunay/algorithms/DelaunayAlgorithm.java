package triangulation.delaunay.algorithms;

import java.util.Set;

import triangulation.Pnt;
import triangulation.Triangulation;

/**
 * 
 * All algorithms to compute a Delaunay triangulation must implement this interface.
 * This way, we may easily test multiple algorithms with just an instance switch.
 * 
 * @author Kevin van As
 *
 */
public interface DelaunayAlgorithm {
	
	/**
     * Place a new site into the DT.
     * Nothing happens if the site matches an existing DT vertex.
     * @param site the new Pnt
     * @throws IllegalArgumentException if site does not lie in any triangle
     */
	public void delaunayPlace(Pnt site, Triangulation trilation);
	
	/**
	 * Add a solid boundary to triangulation.
	 * 
	 * This boundary can not be crossed or removed.
	 * Therefore this boundary must be part of the triangulation.
	 * 
	 * @param site Where the boundary begins
	 * @param anchor Where the boundary ends
	 */
	public boolean delaunayPlaceBoundary(Pnt site, Pnt anchor,
			Triangulation trilation);

	/**
	 * Split a solid boundary in 2 smaller boundaries of the same length.
	 * @param segment The boundary that is splitted (2 elements)
	 */
	public void splitBoundary(Set<Pnt> segment, Triangulation trilation);

	/**
	 * Remove site from trilation
	 * @param site Vertex to remove
	 */
	public void delaunayRemove(Pnt site, Triangulation trilation);
}

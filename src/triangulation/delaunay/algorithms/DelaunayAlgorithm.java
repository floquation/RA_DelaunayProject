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
	
	/// Add boundary vertex from site to anchor????
	public boolean delaunayPlaceBoundary(Pnt site, Pnt anchor,
			Triangulation trilation);

	/// ???
	public void splitBoundary(Set<Pnt> segment, Triangulation trilation);

	/// Remove site from trilation???
	public void delaunayRemove(Pnt site, Triangulation trilation);
}

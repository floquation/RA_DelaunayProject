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
	
	public void delaunayPlace(Pnt site, Triangulation trilation);
	
	public boolean delaunayPlaceBoundary(Pnt site, Pnt anchor,
			Triangulation trilation);
    
	public void splitBoundary(Set<Pnt> segment, Triangulation trilation);

	public void delaunayRemove(Pnt site, Triangulation trilation);
}

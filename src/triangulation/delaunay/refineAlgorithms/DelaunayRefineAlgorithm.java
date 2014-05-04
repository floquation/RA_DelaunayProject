package triangulation.delaunay.refineAlgorithms;

import triangulation.Triangulation;

/**
 * 
 * All algorithms to compute a Delaunay triangulation must implement this interface.
 * This way, we may easily test multiple algorithms with just an instance switch.
 * 
 * @author Kevin van As
 *
 */
public interface DelaunayRefineAlgorithm {
	
	public void refine(Triangulation trilation, double minangle, double maxArea);
	
}

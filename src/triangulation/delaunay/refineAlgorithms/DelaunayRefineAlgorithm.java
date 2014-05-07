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
	
	/**
     * Assure that in each triangle each corner is bigger than minangle.
     * Assure that each triangle has an area below maxArea.
	 */
	public void refine(Triangulation trilation, double minangle, double maxArea);
	
}

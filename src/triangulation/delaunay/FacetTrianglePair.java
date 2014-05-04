package triangulation.delaunay;

import java.util.Set;

import triangulation.Pnt;
import triangulation.Triangle;


/**
 * 
 * Stores a pair: a facet with one of its adjacent triangles.
 * Used for Lawson's edge-flip algorithm.
 * 
 * @author Kevin van As
 *
 */
public class FacetTrianglePair{
	public Set<Pnt> facet;
	public Triangle triangle;
	
	public FacetTrianglePair(Set<Pnt> facet, Triangle triangle){
		this.facet = facet;
		this.triangle = triangle;
	}
	
	public boolean equals(FacetTrianglePair other){
		return facet.equals(other.facet) && triangle.equals(other.triangle);
	}
}
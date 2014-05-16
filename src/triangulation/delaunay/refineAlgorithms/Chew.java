package triangulation.delaunay.refineAlgorithms;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import triangulation.ArraySet;
import triangulation.Pnt;
import triangulation.Triangulation;
import triangulation.Triangle;
import triangulation.delaunay.DelaunayUtils;
import triangulation.delaunay.algorithms.Lawson;

public class Chew implements DelaunayRefineAlgorithm{
	
	// Chew's defines bad triangles as triangles where circumradius / shortest edge > 1.
	private static final boolean debug = true;
	
	/**
	 * Return the segment between triangle and point.
	 * If there are more segments, return the one closest to triangle // TODO (not yet)
	 * If there are no segments, return null  
	 */
	private static Set<Pnt> blockingSegmentOrNull(
			Triangulation trilation,
			Triangle triangle, 
			Pnt point) {
		
		// Is there a segment between badTriangle and circumCentre 
		// that is visible from triangle?
		
		// Three lines from triangle to point
		Set<Set<Pnt>> ccLines = new ArraySet<Set<Pnt>>();
		for(Pnt corner: triangle) {
			ccLines.add(new ArraySet<Pnt>(Arrays.asList(point, corner)));
		}
		
		// Does the triangle itself contain an encroached segment?
		for(Pnt oppositeVertex: triangle) {
			Set<Pnt> facet = triangle.facetOpposite(oppositeVertex);
			if(trilation.isPSLG(facet)) {
				for(Set<Pnt> ccLine: ccLines) {
					if(DelaunayUtils.intersect(ccLine, facet,false)) {
						return facet;
					}
				}
			}
		}
		
		// Does one of the neighbours contain an encroached segment?
		Set<Pnt> blockingSegment = null;
		for(Triangle neighbour: trilation.neighbors(triangle)) {
			Set<Pnt> union = new ArraySet<Pnt>(triangle);
			union.retainAll(neighbour);
			
			for(Pnt samePoint: union) {
				// facet is visible from triangle
				Set<Pnt> facet = neighbour.facetOpposite(samePoint);
				
				// Check if it's an encroaching segment
				if(trilation.isPSLG(facet)) {
					for(Set<Pnt> ccLine: ccLines) {
						if(DelaunayUtils.intersect(ccLine, facet,false)) {
							return facet;
						}
					}
				}
			}
			
			if(blockingSegment != null) break;
		}
		
		// TODO How to handle multiple blockingSegments??
		// The description says only to remove the blockingSegment that is visible from the interior
		// But it seems there are multiple such segments or my implementation is wrong?
		return blockingSegment;
	}
	
	@Override
	public void refine(Triangulation trilation, double minAngle, double maxArea) {
		System.out.println("Chew is called!");
		Queue<Triangle> badTriangles = DelaunayUtils.obtainBadTriangles(
				trilation, minAngle, maxArea);
		int numBadTriangles = badTriangles.size();

		while(!badTriangles.isEmpty()) {
			if(debug) System.out.println("Chew: " + badTriangles.size() + " bad triangles left");
			
			Triangle badTriangle = badTriangles.poll();
			if(!trilation.contains(badTriangle))
				continue;
			
			Pnt circumCenter = badTriangle.getCircumcenter();
			
			// Print info
			if(debug) {
				Triangle.moreInfo=true;
				System.out.println("Chew: " + badTriangle + " is a bad triangle");
				Triangle.moreInfo=false;
				System.out.println("Chew: It's circumcenter is " + circumCenter);
			}
			
			Set<Pnt> blockingSegment = blockingSegmentOrNull(
					trilation, badTriangle, circumCenter);
			if(blockingSegment == null) {
				if(circumCenter.vsCircumcircle(trilation.obtainInitialTriangle().toArray(new Pnt[0])) == -1){
					if(debug)System.out.println("Chew: We can safely insert the circumcenter");
					if(!trilation.delaunayPlace(circumCenter)) {
						if(debug)System.out.println("Chew: Failed to insert circumcenter at " + circumCenter);
					}
				} else {
					if(debug)System.out.println("Chew: I'm not going to insert a circumcenter at: " + circumCenter);
				}
			} else {
				if(debug)System.out.println("Chew: A circumcentre would encroach: " + blockingSegment);
				Queue<Pnt> toRemove = new LinkedList<Pnt>();
				
				// We need to remove circumcenters from the diameter circle of the segment.
				Pnt[] blockingSegmentArray = blockingSegment.toArray(new Pnt[0]);
				Pnt midpoint = blockingSegmentArray[0].midPoint(blockingSegmentArray[1]);
				for(Pnt point: trilation.obtainAllPoints()) {
					Set<Pnt> midPointFacet = new ArraySet<Pnt>(Arrays.asList(point, midpoint));
					if(point.vsDiamcircle(blockingSegmentArray) <= 0) {
						boolean isPSLG = false;
						boolean isVisible = true;
						for(Set<Pnt> segment: trilation.obtainBoundary()) {
							// We may not remove points that are part of the boundary
							if(segment.contains(point)) {
								isPSLG = true;
								break;
							}
							// We may only remove points that are visible from the midpoint.
							if(DelaunayUtils.intersect(segment, midPointFacet,false)) {
								isVisible = false;
								break;
							}
						}
						if(!isPSLG && isVisible)
							toRemove.add(point);
					}
				}
				
				// Execute toRemove list
				if(debug)System.out.println("Chew: " + toRemove.size() + 
						" elements need to be removed: " + toRemove);
				for(Pnt site: toRemove) {
					// FIXME: Illegal argument exception??
					trilation.delaunayRemove(site);
				}
				
				// Split the segment
				if(debug)System.out.println("Chew: The segment is splitted");
				assert(trilation.isPSLG(blockingSegment));
				trilation.splitBoundary(blockingSegment);
			}
			
			if(badTriangles.isEmpty()) {
				badTriangles = DelaunayUtils.obtainBadTriangles(
						trilation, minAngle, maxArea);
				if(debug)System.out.println("Chew: New bad triangles " + badTriangles.size());
				
				if(numBadTriangles <= badTriangles.size()) {
					if(debug) System.out.println("Chew: Escaped from infinite loop: " + 
							badTriangles.size() + " instead of " + numBadTriangles + 
							" triangles left");
					return;
				} else {
					numBadTriangles = badTriangles.size();
				}
			}
			System.out.println("Num baddies is " + badTriangles.size());
		}
		System.out.println("Chew Finished with " + badTriangles.size() + " bad triangles");
	}
	
	public static void main(String args[]) {
		// This constructor is obviously insane
		Triangle initialTriangle = new Triangle(Arrays.asList(
				new Pnt(-10000, -10000),
				new Pnt(10000, -10000),
				new Pnt(0, 10000)));

		Triangulation trilation = new Triangulation(initialTriangle, new Lawson());
		/*trilation.delaunayPlaceBoundary(new Pnt(-5, 5), new Pnt(-5, 0));
		trilation.delaunayPlaceBoundary(new Pnt(-5, 0), new Pnt(5, 0));
		trilation.delaunayPlaceBoundary(new Pnt(5, 0), new Pnt(5, 5));
		trilation.delaunayPlaceBoundary(new Pnt(5, 5), new Pnt(0, 3));
		trilation.delaunayPlaceBoundary(new Pnt(0, 3), new Pnt(-5, 5));*/
		
		// Here one blocking segment is ISPL.
		trilation.delaunayPlaceBoundary(new Pnt(252.0,461.0), new Pnt(249.0,212.0));
		trilation.delaunayPlaceBoundary(new Pnt(252.0,461.0), new Pnt(503.0,461.0));
		trilation.delaunayPlaceBoundary(new Pnt(481.0,206.0), new Pnt(503.0,461.0));
		trilation.delaunayPlaceBoundary(new Pnt(481.0,206.0), new Pnt(369.0,349.0));
		trilation.delaunayPlaceBoundary(new Pnt(369.0,349.0), new Pnt(249.0,212.0));
		
		DelaunayRefineAlgorithm alg = new Chew();
		for(int i = 0; i < 5; i++)
			trilation.refine(trilation, alg, 20d/180*Math.PI,200);
	}
}


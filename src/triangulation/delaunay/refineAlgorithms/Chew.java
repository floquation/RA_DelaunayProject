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
	//private static final boolean debug = true;
	
	/**
	 * Return the segment between triangle and point.
	 * If there are more segments, return the one closest to triangle // TODO (not yet)
	 * If there are no segments, return null  
	 */
	private static Set<Pnt> blockingSegmentOrNull(
			Triangulation trilation,
			Triangle triangle, 
			Pnt point) {
		
		// Three lines from triangle to point
		Set<Set<Pnt>> ccLines = new ArraySet<Set<Pnt>>();
		for(Pnt corner: triangle) {
			ccLines.add(new ArraySet<Pnt>(Arrays.asList(point, corner)));
		}
		
		// Is there a segment between badTriangle and circumCentre 
		// that is visible from triangle?
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
						if(DelaunayUtils.intersect(ccLine, facet))
							blockingSegment = facet;
					}
				}
			}
			
			if(blockingSegment != null) break;
		}
		
		return blockingSegment;
	}

	@Override
	public void refine(Triangulation trilation, double minAngle, double maxArea) {
		System.out.println("Chew is going to refine this!");
		Queue<Triangle> badTriangles = DelaunayUtils.obtainBadTriangles(
				trilation, minAngle, maxArea);

		while(!badTriangles.isEmpty()) {
			Triangle badTriangle = badTriangles.poll();
			if(!trilation.contains(badTriangle))
				continue;
			
			// Print info
			Pnt circumCentre = badTriangle.getCircumcenter();
			Triangle.moreInfo=true;
			System.out.println("Chew: " + badTriangle + " is a bad triangle");
			Triangle.moreInfo=false;
			System.out.println("Chew: It's circumcenter is " + circumCentre);
			
			Set<Pnt> blockingSegment = blockingSegmentOrNull(
					trilation, badTriangle, circumCentre);
			if(blockingSegment == null) {
				System.out.println("Chew: We can safely insert the circumcentre");
				trilation.delaunayPlace(circumCentre);
			} else {
				System.out.println("Chew: A circumcentre would encroach: " + blockingSegment);
				Queue<Pnt> toRemove = new LinkedList<Pnt>();
				
				// We need to remove everything from the diameter circle of the segment.
				// except the points that are part of the PSLG.
				// TODO only the circumcenters that are visible from the inserted midpoint?
				Pnt[] blockingSegmentArray = blockingSegment.toArray(new Pnt[0]); 
				for(Pnt point: trilation.obtainAllPoints()) {
					if(point.vsDiamcircle(blockingSegmentArray) < 0) {
						boolean isPSLG = false;
						for(Set<Pnt> unremovable: trilation.obtainBoundary()) {
							if(unremovable.contains(point))
								isPSLG = true;
						}
						if(!isPSLG)
							toRemove.add(point);
					}
				}
				
				
				// Execute toRemove list
				System.out.println("Chew " + toRemove.size() + " elements need to be removed: " + 
						toRemove);
				for(Pnt site: toRemove) {
					trilation.delaunayRemove(site);
				}
				
				// Split the segment
				System.out.println("Chew: The segment is splitted");
				assert(trilation.isPSLG(blockingSegment));
				trilation.splitBoundary(blockingSegment);
			}
			
			if(badTriangles.isEmpty()) {
				badTriangles = DelaunayUtils.obtainBadTriangles(
						trilation, minAngle, maxArea);
				System.out.println("Bad triangles " + badTriangles.size());
			}
			
			break;
		}
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


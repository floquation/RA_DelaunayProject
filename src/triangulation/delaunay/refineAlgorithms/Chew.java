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
		
		// Is there a segment between badTriangle and circumCentre?
		// neigbours / facetOpposite
		// Utils/intersect
		Set<Pnt> blockingSegment = null;
		
		for(Set<Pnt> segment : trilation.obtainBoundary()) {
			for(Set<Pnt> ccLine: ccLines)
				if(DelaunayUtils.intersect(ccLine, segment))
					blockingSegment = segment;
			
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
			
			Pnt circumCentre = badTriangle.getCircumcenter();
			Triangle.moreInfo=true;
			System.out.println("Chew: " + badTriangle + " is a naughty boy");
			Triangle.moreInfo=false;
			System.out.println("Chew: It's circumcenter is " + circumCentre);
			
			Set<Pnt> blockingSegment = blockingSegmentOrNull(
					trilation, badTriangle, circumCentre);
			if(blockingSegment == null) {
				System.out.println("Chew: We are gonna split a triangle");
				trilation.delaunayPlace(circumCentre);
			} else {
				System.out.println("Chew: An elephant is blocking our way" + blockingSegment);
				Queue<Pnt> toRemove = new LinkedList<Pnt>();
				// FIXME: The current solution is brute-force
				// FIXME: java.util.ConcurrentModificationException
				Pnt[] blockingSegmentArray = blockingSegment.toArray(new Pnt[0]); 
				for(Pnt point: trilation.obtainAllPoints()) {
					if(point.vsDiamcircle(blockingSegmentArray) < 0) {
						// FIXME Hack to ignore boundary
						boolean f = true;
						for(Set<Pnt> unremovable: trilation.obtainBoundary()) {
							if(unremovable.contains(point))
								f = false;
						}
						if(f)
							toRemove.add(point);
					}
				}
				
				// Die
				System.out.println("Chew " + toRemove.size() + " elements need to be removed: " + 
						toRemove);
				for(Pnt site: toRemove)
					trilation.delaunayRemove(site);
				
				System.out.println("Chew: The segment is splitted");
				//Pnt midpoint = blockingSegmentArray[0].midPoint(blockingSegmentArray[1]);
				//trilation.delaunayPlace(midpoint);
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
		trilation.delaunayPlaceBoundary(new Pnt(-5, 5), new Pnt(-5, 0));
		trilation.delaunayPlaceBoundary(new Pnt(-5, 0), new Pnt(5, 0));
		trilation.delaunayPlaceBoundary(new Pnt(5, 0), new Pnt(5, 5));
		trilation.delaunayPlaceBoundary(new Pnt(5, 5), new Pnt(0, 3));
		trilation.delaunayPlaceBoundary(new Pnt(0, 3), new Pnt(-5, 5));
		/*
		trilation.delaunayPlaceBoundary(new Pnt(248.0,253.0), new Pnt(244.0,596.0));
		trilation.delaunayPlaceBoundary(new Pnt(719.0,599.0), new Pnt(244.0,596.0));
		trilation.delaunayPlaceBoundary(new Pnt(711.0,262.0), new Pnt(719.0,599.0));
		trilation.delaunayPlaceBoundary(new Pnt(482.0,435.0), new Pnt(711.0,262.0));
		trilation.delaunayPlaceBoundary(new Pnt(482.0,435.0), new Pnt(248.0,253.0));*/
		
		DelaunayRefineAlgorithm alg = new Chew();
		for(int i = 0; i < 5; i++)
			trilation.refine(trilation, alg, 20d/180*Math.PI,200);
		
		/*
		// Construct a convex polygon
		Pnt malware = new Pnt(0, 3);
		Pnt bottom = new Pnt(0, 1);
		trilation.delaunayPlace(malware);
		trilation.delaunayPlace(bottom);
		trilation.isGraphStillCorrect("debugRemoval - Before removal");

		// I can't believe you did that!?
		trilation.delaunayRemove(malware);
		trilation.isGraphStillCorrect("debugRemoval - After removal");
		*/
	}
}

//int numBadTriangles = badTriangles.size();
// Infinite loop detector
/*int newNumBadTriangles = badTriangles.size();
if(newNumBadTriangles == numBadTriangles) {
	System.err.println("Phew. Escaped from an infinite loop!");
	return;
} else {
	numBadTriangles = newNumBadTriangles;
}

	private static Set<Pnt> blockingSegmentOrNull1(
			Triangulation trilation,
			Triangle triangle, 
			Pnt point) {
		
		// Three lines from triangle to point
		Set<Set<Pnt>> ccLines = new ArraySet<Set<Pnt>>();
		for(Pnt corner: triangle) {
			ccLines.add(new ArraySet<Pnt>(Arrays.asList(point, corner)));
		}
		
		// Is there a segment between badTriangle and circumCentre?
		// neigbours / facetOpposite
		// Utils/intersect
		Set<Pnt> blockingSegment = null;
		for(Triangle neighbour: trilation.neighbors(triangle)) {
			Set<Pnt> union = new ArraySet<Pnt>(triangle);
			union.retainAll(neighbour);
			
			for(Pnt samePoint: union) {
				Set<Pnt> segment = neighbour.facetOpposite(samePoint);
				for(Set<Pnt> ccLine: ccLines) {
					if(DelaunayUtils.intersect(ccLine, segment))
						blockingSegment = segment;
				}
			}
			
			if(blockingSegment != null) break;
		}
		
		return blockingSegment;
		/*if(blockingSegment != null) {
			Pnt[] blockingSegmentArray = new Pnt[2];
			return blockingSegment.toArray(blockingSegmentArray);
		} else
			return null;* /
	}

*
*/


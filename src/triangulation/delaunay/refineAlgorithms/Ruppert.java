package triangulation.delaunay.refineAlgorithms;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import triangulation.Pnt;
import triangulation.Triangle;
import triangulation.Triangulation;
import triangulation.delaunay.DelaunayUtils;

public class Ruppert implements DelaunayRefineAlgorithm{
	
	private static final boolean debug = false;

	@Override
	public void refine(Triangulation trilation, double minAngle, double maxArea) {
		if(debug)System.out.println("(Ruppert) Ruppert algorithm begins. Criterions specified: minAngle = " + minAngle + ",\t maxArea = " + maxArea);
		
		Queue<Set<Pnt>> toDoList_segment = new LinkedList<Set<Pnt>>();
		Queue<Triangle> toDoList_triangle = new LinkedList<Triangle>();
		Set<Triangle> marked_tr = new HashSet<Triangle>();
		Set<Set<Pnt>> marked_fc = new HashSet<Set<Pnt>>();
		
		while(true){ //While there are troublesome triangles:
			//Create a troublesome list:
			Queue<Triangle> toDoListTriangles = DelaunayUtils.obtainBadTriangles(trilation,minAngle,maxArea);
			toDoList_triangle.addAll(toDoListTriangles);
			if(debug)System.out.println("(Ruppert) toDoList_triangle.size() = " + toDoList_triangle.size());
			if(toDoList_triangle.isEmpty()) break; //We are done! Algorithm terminates.
			while(!toDoList_triangle.isEmpty() || !toDoList_segment.isEmpty()){ //While there are still items in our current list
				if(toDoList_segment.isEmpty()){ //Troublesome triangle should be chosen
					Triangle cur_triangle = toDoList_triangle.remove();
					/*unmark*/marked_tr.remove(cur_triangle);
					if(!trilation.contains(cur_triangle)) continue; //The triangle was removed in a previous process. Do not process it.
					
					//Check if the triangle's circumcenter is encroaching any segment
					//I.e., is the circumcenter in the diametrical circle of any segment?
					Pnt center = cur_triangle.getCircumcenter();
					Set<Set<Pnt>> boundaryList = trilation.obtainBoundary(); //unmodifiable list of boundary segments
					Set<Pnt> encroachedSegment = null;
					for(Set<Pnt> segment : boundaryList){ //for each PSLG...
						if(center.vsDiamcircle(segment.toArray(new Pnt[0])) == -1){
							//We found AN encroached segment.
							encroachedSegment = segment;
							//Segment must be split
							if(!marked_fc.contains(encroachedSegment)){
								toDoList_segment.add(encroachedSegment);
								marked_fc.add(encroachedSegment);
							}
						}
					}
					//If there was no encroached segment, insert the circumcenter of the triangle.
					if(encroachedSegment == null){
						//TODO: Only add the circumcenter if it is reasonably nearby
						if(center.vsCircumcircle(trilation.obtainInitialTriangle().toArray(new Pnt[0])) == -1){
							Triangle t = trilation.locate(center);
//							if(t.containsAny(trilation.obtainInitialTriangle())){
//								System.out.println("(Ruppert) Point being added outside domain: don't allow it.");
//							}else{
								trilation.delaunayPlace(center);									
//							}						
						}else{
							//We have a very troublesome triangle, with two very small minAngles.
							//Kevin's alternative to Ruppert: remove the point with the big angle.
							Pnt bigAnglePoint = cur_triangle.getMaxAnglePoint();
							if(debug)System.out.println("(Ruppert) We have a poor triangle: " + cur_triangle.toString() + ";\t with poor point: " + bigAnglePoint.toString());
							//trilation.delaunayRemove(bigAnglePoint); //TODO: BUGGED because Lawson's remover is bugged.
						}
					}
					
				}else{ //Troublesome segment should be treated
					Set<Pnt> segment = toDoList_segment.remove();
					/*unmark*/marked_fc.remove(segment);
					trilation.splitBoundary(segment);
					//break outerLoop;
				}
			}
			if(debug) break; //This will terminate after 1 iteration, allowing a step-by-step visualisation.
		}
		if(debug)System.out.println("(Ruppert) Ruppert terminates.");
	}

}

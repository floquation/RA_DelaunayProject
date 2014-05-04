package GUI;

import triangulation.Pnt;

public enum MouseModes {
	free("Free mouse"),
	point("Point placer"),
	boundaryBuilder("Boundary line placer"),
	remove("Remove point");
	

	private String name;
	
	private MouseModes(String name){
		this.name = name;
	}
	
	public String toString(){
		return name;
	}
	
	public void onModeChange(DelaunayPanel delaunayPanel){
		switch(this){
		case free: //Free mouse, don't do anything.
			delaunayPanel.resetLastPnt();
			break;
		case boundaryBuilder:
			break;
		case point:
			delaunayPanel.resetLastPnt();
			break;
		case remove:
			delaunayPanel.resetLastPnt();
			break;			
		default:
			break;
		}
	}
	
	public void onLeftClick(int x, int y, DelaunayPanel delaunayPanel){
		Pnt pnt = new Pnt(x,y);
		switch(this){
		case free: //Free mouse, don't do anything.
			break;
		case boundaryBuilder:
			delaunayPanel.addBoundarySite();
			break;
		case point:
			delaunayPanel.addSite(pnt);
			break;
		case remove:
			delaunayPanel.removeSite();
			break;
		default:
			break;
		}
	}
	
	public void onRightClick(int x, int y, DelaunayPanel delaunayPanel){
		//Pnt pnt = new Pnt(x,y);
		switch(this){
		case free: //Free mouse, don't do anything.
			break;
		case boundaryBuilder:
			delaunayPanel.resetLastPnt();
			break;
		case point:
			break;
		default:
			break;
		}
	}
	
	public void onMouseMove(int x, int y, DelaunayPanel delaunayPanel){
		Pnt pnt = new Pnt(x,y);
		switch(this){
		case free: //Free mouse, don't do anything.
		case point:
			break;
		case boundaryBuilder:
			delaunayPanel.changeBoundaryPointer(pnt);
			break;
		case remove:
			delaunayPanel.changeBoundaryPointer(pnt);
			break;
		default:
			break;
		}		
	}
}

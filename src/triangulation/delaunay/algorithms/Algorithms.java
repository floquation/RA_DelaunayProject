package triangulation.delaunay.algorithms;


public enum Algorithms {
	Lawson("Lawson"),
	BowyerWatson("Bowyer Watson");
	
	private String name;
	
	private Algorithms(String name){
		this.name = name;
	}
	
	public String toString(){
		return name;
	}
	
	public DelaunayAlgorithm createAlgorithm(){
		if(this == BowyerWatson){
			return new BowyerWatson();			
		}
		if(this == Lawson){
			return new Lawson();			
		}
		return null;
	}
}

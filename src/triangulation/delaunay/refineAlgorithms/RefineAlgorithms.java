package triangulation.delaunay.refineAlgorithms;

public enum RefineAlgorithms {
	Null("<<RefineAlgorithms>>"),
	Ruppert("Ruppert"),
	Chew("Chew");
	
	private String name;
	
	private RefineAlgorithms(String name){
		this.name = name;
	}
	
	public String toString(){
		return name;
	}
	
	public DelaunayRefineAlgorithm createAlgorithm(){
		if(this == Ruppert){
			return new Ruppert();			
		}
		if(this == Chew){
			return new Chew();				
		}
		return null;
	}
}

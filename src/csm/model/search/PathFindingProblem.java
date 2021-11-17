package csm.model.search;

public class PathFindingProblem {

	private String originURI, destinationURI;
	private Goal goal;
	
	public PathFindingProblem(String originURI, String destinationURI) {
		this.originURI = originURI;
		this.destinationURI = destinationURI;
		goal = new Goal(destinationURI);
	}
	
	public String getOriginURI() {
		return originURI;
	}
	
	public String getDestinationURI() {
		return destinationURI;
	}
	
	public Goal getGoal() {
		return goal;
	}
}

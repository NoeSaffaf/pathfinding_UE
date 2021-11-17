package csm.model.solution;

import csm.model.search.Node;

public class SegmentSolution {

	private String resource;
	private Node origin;
	private Node destination;
	
	public SegmentSolution(Node origin, Node destination, String resource){
		this.origin = origin;
		this.destination = destination;
		this.resource = resource;
	}
	
	public Node getOrigin()
	{
		return origin;
	}
	
	public Node getDestination()
	{
		return destination;
	}
	
	

	@Override
	public String toString() {
		String segmentString = null;
		if (origin != null){
			String resourceString = resource;
			segmentString = origin.getURI() + "  -  " + resourceString + " [Cost: "
					+ (destination.getG() - destination.getParent().getG()) + "]" + "  -  " + destination.getURI();
		}
		return segmentString;
	}	
}
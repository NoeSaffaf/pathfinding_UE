package csm.model.solution;

import java.util.ArrayList;
import java.util.List;

import csm.model.search.Node;


public class Solution {

	private Node goalNode;
	private List<SegmentSolution> segments;
	private long discoveredTime;
	private String origin, destination;
	
	private Solution(Node goalNode){
		this.goalNode = goalNode;
		this.segments = new ArrayList<>();
		
		backTrack();
	}
	
	public Solution(Node goalNode, long discoveredTime){
		this(goalNode);
		this.discoveredTime = discoveredTime;
	}
	
	public String getOrigin(){
		return origin;
	}
	
	public String getDestination(){
		return destination;
	}
	
	public long getDiscoveredTime(){
		return discoveredTime;
	}
	
	public void setDiscoveredTime(long time){
		this.discoveredTime = time;
	}
	
	public Node getGoalNode(){
		return goalNode;
	}
	
	public List<SegmentSolution> getSegments() {
		return segments;
	}
	
	public List<String> getPathNodesURI() {
		List<String> pathNodeUris = new ArrayList<>();
		for (int i = 1; i < segments.size(); i++) {
			SegmentSolution segment = segments.get(i);
			pathNodeUris.add(segment.getOrigin().getURI());
			if (i + 1 == segments.size()) {
				pathNodeUris.add(segment.getDestination().getURI());
			}
		}
		return pathNodeUris;
	}
	
	public List<Node> getPathNodes() {
		List<Node> pathNodes = new ArrayList<>();
		for (int i = 1; i < segments.size(); i++) {
			SegmentSolution segment = segments.get(i);
			pathNodes.add(segment.getOrigin());
			if (i + 1 == segments.size()) {
				pathNodes.add(segment.getDestination());
			}
		}
		return pathNodes;
	}
	
	private void backTrack(){
		Node currentNode = goalNode;
		while(currentNode != null){
			segments.add(0, new SegmentSolution(currentNode.getParent(), currentNode, currentNode.getConnectingResource()));
			currentNode = currentNode.getParent();
		}
	}

	@Override

	public String toString() {
		String solutionString = "Total cost: "+goalNode.getG()+"\n";
		for (SegmentSolution segment : segments){
			if (segment.toString() != null)
				solutionString += "   Segment - " + segment.toString() +"\n";
		}
		return solutionString;
	}	
}
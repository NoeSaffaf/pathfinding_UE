package csm.model.search;

import java.io.Serializable;

public class Arc implements Comparable<Arc>, Serializable{
	private static final long serialVersionUID = 1L;

	private Node parentNode;
	private String childIRI;
	private String connectingResourceIRI;
	private int arcCost;
	
	// For removing non-responding request
	private double REQUESTED_TIME;

	// For communication
	private int arcID;

	
	public Arc(Node parentNode, String childIRI, String connectingResourceIRI){
		this.parentNode = parentNode;
		this.childIRI = childIRI;
		this.connectingResourceIRI = connectingResourceIRI;
		arcCost = -1;
		REQUESTED_TIME = -1;
	}
	
	public void setArcID(int arcID) {
		this.arcID = arcID;
	}
	
	public int getArcID() {
		return arcID;
	}
	
	// Called when its cost is being retrieved by a resource agent
	public void setToPendingMode(){ 
		REQUESTED_TIME = System.currentTimeMillis();
	}
	
	public void setParentNode(Node parentNode){
		this.parentNode = parentNode;
	}

	public void setchildURI(String childIRI){
		this.childIRI= childIRI;
	}
	
	public void setArcCost(int arcCost){
		this.arcCost = arcCost;
	}
	
	
	public double getRequestTime(){
		return REQUESTED_TIME;
	}
	
	public Node getParentNode(){
		return this.parentNode;
	}
	
	public String getchildIRI(){
		return this.childIRI;
	}
	
	public String getConnectingResourceURI(){
		return this.connectingResourceIRI;
	}
	
	public int getArcCost(){
		return this.arcCost;
	}
	
	public boolean isSame(Arc arc){
		return parentNode.getURI().equals(arc.getParentNode().getURI())
				&& childIRI.equals(arc.getchildIRI())
				&& connectingResourceIRI.equals(arc.getConnectingResourceURI());
	}
	
	@Override
	public int compareTo(Arc arc) {		
		if (this.parentNode.getG() == arc.parentNode.getG()){
			return 0;
		}else if (this.parentNode.getG() < arc.parentNode.getG()){
			return -1;
		}else{
			return 1;
		}
	}
	
	@Override
	public String toString() {
		String action = "Parent: ";
		if (parentNode != null){
			action += parentNode.getURI();
		}
		action += "\t " + connectingResourceIRI + "\n";
		action += "\t\t Child: "+ childIRI + "\n";
		action += "\t\t Cost: "+ arcCost +"\n";
		
		return action;
	}
}
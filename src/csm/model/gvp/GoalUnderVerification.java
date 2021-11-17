package csm.model.gvp;

import csm.model.search.Node;

// Represent the potential goal node found by a search agent pending verification
public class GoalUnderVerification {

	private Node goalNode;
	private int numberOfVerifiedResponse = 0; // response from the other search agents
	private String founderAgent; // agent who found the node
	private String senderAgent; 
	private long discoveredTime;
	
	
	public GoalUnderVerification(Node goalNode, String founderAgent, String senderAgent){
		this.goalNode = goalNode;
		this.founderAgent = founderAgent;
		this.senderAgent = senderAgent;
		this.discoveredTime = System.currentTimeMillis();
	}
	
	public String getSenderAgent() {
		return senderAgent;
	}
	
	public Node getGoalNode(){
		return goalNode;
	}
	
	public void addAVerificationResponse(){
		numberOfVerifiedResponse++;
	}
	
	public int getNumberOfVerificationResponse(){
		return numberOfVerifiedResponse;
	}
	
	public String getFounderAgent(){
		return founderAgent;
	}
	
	public long getDiscoveredTime(){
		return discoveredTime;
	}
}
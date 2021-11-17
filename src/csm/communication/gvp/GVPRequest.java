package csm.communication.gvp;

import java.io.Serializable;

import csm.model.search.Node;

// Represent the request to verify a potential goal node found by a search agent
public class GVPRequest implements Serializable{
	private static final long serialVersionUID = 1L;

	private Node goalNode;
	private String goalAgent; // the agent that found the node
	private String senderAgent; // the agent that sends the request
	
	public  GVPRequest(Node goalNode, String goalAgent, String senderAgent){
		this.goalNode = goalNode;
		this.goalAgent = goalAgent;
		this.senderAgent = senderAgent;
	}
	
	public Node getGoalNode(){
		return this.goalNode;
	}
	
	public String getGoalAgent(){
		return goalAgent;
	}
	
	public String getSenderAgent() {
		return senderAgent;
	}
}

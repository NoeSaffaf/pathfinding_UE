package csm.communication.dynamics;

import java.io.Serializable;

public class ForwardNodeUpdatesToGoalAgentRequest implements Serializable{
	private static final long serialVersionUID = 1L;

	private String nodeURI;
	private String goalAgent;
	
	public ForwardNodeUpdatesToGoalAgentRequest(String nodeURI, String goalAgent) {
		this.nodeURI = nodeURI;
		this.goalAgent = goalAgent;
	}
	
	public String getNodeURI() {
		return nodeURI;
	}
	
	public String getGoalAgent() {
		return goalAgent;
	}
}
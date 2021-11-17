package csm.communication.dynamics;

import java.io.Serializable;
import java.util.List;

public class SubscribeSuccessorNodeUpdatesRequest implements Serializable{
	private static final long serialVersionUID = 1L;

	private String nodeURI;
	private String parentNodeURI;
	private String goalAgent;
	private List<String> ancestorList;
	
	public SubscribeSuccessorNodeUpdatesRequest(String nodeURI, String parentNodeURI, String goalAgent, List<String> ancestorList) {
		this.nodeURI = nodeURI;
		this.parentNodeURI = parentNodeURI;
		this.goalAgent = goalAgent;
		this.ancestorList = ancestorList;
	}
	
	public String getNodeURI() {
		return nodeURI;
	}
	
	public String getParentNodeURI() {
		return parentNodeURI;
	}
	
	public String getGoalAgent() {
		return goalAgent;
	}
	
	public List<String> getAncestorList(){
		return ancestorList;
	}
	
}
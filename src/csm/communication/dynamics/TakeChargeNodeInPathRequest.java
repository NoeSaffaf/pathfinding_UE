package csm.communication.dynamics;

import java.io.Serializable;
import java.util.List;

public class TakeChargeNodeInPathRequest implements Serializable{
	private static final long serialVersionUID = 1L;

	private String nodeURI;
	private String goalAgent;
	private List<String> ancestorList;
	
	public TakeChargeNodeInPathRequest(String nodeURI, String goalAgent, List<String> ancestorList) {
		this.nodeURI = nodeURI;
		this.goalAgent = goalAgent;
		this.ancestorList = ancestorList;
	}
	
	public String getNodeURI() {
		return nodeURI;
	}
	
	public String getGoalAgent() {
		return goalAgent;
	}
	
	public List<String> getAncestorList(){
		return ancestorList;
	}
	
}
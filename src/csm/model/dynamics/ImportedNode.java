package csm.model.dynamics;

public class ImportedNode {

	private String agentResponsible;
	private String nodeURI;
	
	public ImportedNode(String nodeURI, String agentResponsible) {
		this.nodeURI = nodeURI;
		this.agentResponsible = agentResponsible;
	}
	
	public String getNodeURI() {
		return nodeURI;
	}
	
	public String getAgentResponsible() {
		return agentResponsible;
	}
}

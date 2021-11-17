package csm.model.dynamics;

public class MonitoredNode{

	private String parentNodeURI;
	private String nodeURI;
	private String connectingResource;
	private String requesterAgent;
	
	public MonitoredNode(String nodeURI, String parentNodeURI, String connectingResource, String requesterAgent) {
		this.nodeURI = nodeURI;
		this.parentNodeURI = parentNodeURI;
		this.connectingResource = connectingResource;
		this.requesterAgent = requesterAgent;
	}
	
	public String getNodeURI() {
		return nodeURI;
	}
	
	public String getParentNodeURI() {
		return parentNodeURI;
	}
	
	public String getConnectingResource() {
		return connectingResource;
	}
	
	public String getRequesterAgent() {
		return requesterAgent;
	}

	@Override
	public String toString() {
		return parentNodeURI + " - " + nodeURI;
	}
	
	
}

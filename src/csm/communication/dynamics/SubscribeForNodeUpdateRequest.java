package csm.communication.dynamics;

import java.io.Serializable;

public class SubscribeForNodeUpdateRequest implements Serializable{
	private static final long serialVersionUID = 1L;

	private String nodeURI;
	private String parentURI;
	private String connectingResource;
	private String requesterAgent;
	
	public SubscribeForNodeUpdateRequest(String nodeURI, String parentURI, String connectingResource, String requesterAgent) {
		this.nodeURI = nodeURI;
		this.parentURI = parentURI;
		this.connectingResource = connectingResource;
		this.requesterAgent = requesterAgent;
	}
	
	public String getNodeURI() {
		return nodeURI;
	}
	
	public String getParentURI() {
		return parentURI;
	}
	
	public String getConnectingResource() {
		return connectingResource;
	}
	
	public String getRequesterAgent() {
		return requesterAgent;
	}
}
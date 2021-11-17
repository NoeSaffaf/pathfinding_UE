package csm.communication.dynamics;

import java.io.Serializable;

public class NodeUpdateNotification implements Serializable{
	private static final long serialVersionUID = 1L;

	private String nodeURI;
	private String parentURI;
	private String connectingResource;
	private int newArcCost;
	
	public NodeUpdateNotification(String nodeURI, String parentURI, String connectingResource, int newArcCost) {
		this.nodeURI = nodeURI;
		this.parentURI = parentURI;
		this.connectingResource = connectingResource;
		this.newArcCost = newArcCost;
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
	
	public int getNewArcCost() {
		return newArcCost;
	}
}
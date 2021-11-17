package csm.communication.dynamics;

import java.io.Serializable;

import csm.model.search.Node;

public class TakeChargeNodeInPathResponse implements Serializable{
	private static final long serialVersionUID = 1L;

	private Node node;
	private String agentInCharge;
	
	public TakeChargeNodeInPathResponse(Node node, String agentInCharge) {
		this.node = node;
		this.agentInCharge = agentInCharge;
	}
	
	public Node getNode() {
		return node;
	}
	
	public String getAgentInCharge() {
		return agentInCharge;
	}
	
}
package csm.communication.dynamics;

import java.io.Serializable;

public class RemoteSuccessorUpdate implements Serializable{
	private static final long serialVersionUID = 1L;

	private String nodeURI;
	private String parentURI;
	private int newArcCost;
	private int h;
	private String agentInChargeOfSuccessor;
	
	public RemoteSuccessorUpdate(String nodeURI, String parentURI, int h, int newArcCost, String agentInChargeOfSuccessor) {
		this.h = h;
		this.nodeURI = nodeURI;
		this.parentURI = parentURI;
		this.newArcCost = newArcCost;
		this.agentInChargeOfSuccessor = agentInChargeOfSuccessor;
	}

	public int getH() {
		return h;
	}

	public String getNodeURI() {
		return nodeURI;
	}
	
	public String getParentURI() {
		return parentURI;
	}
	
	public int getNewArcCost() {
		return newArcCost;
	}
	
	public String getAgentInChargeOfSuccessor() {
		return agentInChargeOfSuccessor;
	}

	@Override
	public String toString() {
		return "RemoteSuccessorUpdate{" +
				"nodeURI='" + nodeURI + '\'' +
				", parentURI='" + parentURI + '\'' +
				", newArcCost=" + newArcCost +
				", h=" + h +
				", agentInChargeOfSuccessor='" + agentInChargeOfSuccessor + '\'' +
				'}';
	}
}
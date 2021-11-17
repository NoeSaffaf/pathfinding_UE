package csm.communication.dynamics;

import java.io.Serializable;

public class RemoteSuccessorInfo implements Serializable{
	private static final long serialVersionUID = 1L;

	private String nodeURI;
	private String parentURI;
	private int g, h;
	private String agentInChargeOfSuccessor;
	
	public RemoteSuccessorInfo(String nodeURI, String parentURI, int g, int h, String agentInChargeOfSuccessor) {
		this.nodeURI = nodeURI;
		this.parentURI = parentURI;
		this.g = g;
		this.h = h;
		this.agentInChargeOfSuccessor = agentInChargeOfSuccessor;
	}
	
	public String getNodeURI() {
		return nodeURI;
	}
	
	public String getParentURI() {
		return parentURI;
	}
	
	public int getG() {
		return g;
	}
	
	public int getH() {
		return h;
	}
	
	public String getAgentInChargeOfSuccessor() {
		return agentInChargeOfSuccessor;
	}
}
package csm.model.dynamics;

public class PathNodeSuccessor {

	private String nodeURI;
	private int g, h;
	private String agentInCharge;
	
	public PathNodeSuccessor(String nodeURI, int g, int h, String agentInCharge) {
		this.nodeURI = nodeURI;
		this.g = g;
		this.h = h;
		this.agentInCharge = agentInCharge;
	}
		
	public String getNodeURI() {
		return nodeURI;
	}
	
	public int getG() {
		return g;
	}
	
	public int getH() {
		return h;
	}
	
	public boolean costUnknown() {
		return g == -1;
	}
	
	public String getAgentInCharge() {
		return agentInCharge;
	}
}

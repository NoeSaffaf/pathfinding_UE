package csm.communication.routing;

import java.io.Serializable;

public class FindAgentResponsibleResponseToRequesterAgent implements Serializable{
	private static final long serialVersionUID = 1L;

	private String agentIncharge;
	private String node;
	
	public FindAgentResponsibleResponseToRequesterAgent(String agentInCharge, String node) {
		this.agentIncharge = agentInCharge;
		this.node = node;
	}
	
	public String getAgentInCharge() {
		return agentIncharge;
	}
	
	public String getNode() {
		return node;
	}
}

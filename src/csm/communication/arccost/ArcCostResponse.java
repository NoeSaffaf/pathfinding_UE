package csm.communication.arccost;

import jade.util.leap.Serializable;

// Represent the response to the ArcCostRequest, containing the cost requested by a search agent (sent by a resource agent)
public class ArcCostResponse implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private int arcID;
	private int arcCost;

	public ArcCostResponse(int arcID, int arcCost) {
		this.arcID = arcID;
		this.arcCost = arcCost;
	}
	
	public int getArcID() {
		return arcID;
	}
	
	public int getArcCost() {
		return arcCost;
	}
}
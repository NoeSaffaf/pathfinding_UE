package csm.communication.gvp;

import java.io.Serializable;

import csm.model.search.Node;

// Represent the response the a GVPRequest stating that the goal node is verifed to be local optimal by the search agent sending the response
public class GVPResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	private Node goalNode;
	private boolean isVerifiedLocally;

	public GVPResponse(Node goalNode, boolean isVerifiedLocally){
		this.goalNode = goalNode;
		this.isVerifiedLocally = isVerifiedLocally;
	}

	public Node getGoalNode() {
		return this.goalNode;
	}

	public boolean isVerifiedLocally() {
		return this.isVerifiedLocally;
	}
}

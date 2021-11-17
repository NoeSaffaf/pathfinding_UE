package csm.communication.arccost;

import java.io.Serializable;

// Represent the request for cost of moving between a parent node and a child node (sent by a search agent to a resource agent)
public class ArcCostRequest implements Serializable{
	private static final long serialVersionUID = 1L;

	private int arcID;
	private String parentIRI; // IRI of the parent node (i.e., origin node)
	private String childIRI; // IRI of the child node (i.e., destination node)
	private String connectingResourceIRI; // IRI of the connecting resource from which the cost of moving can be retrieved
	
	public ArcCostRequest(int arcID, String parentIRI, String childIRI, String connectingResource) {
		this.arcID = arcID;
		this.parentIRI = parentIRI;
		this.childIRI = childIRI;
		this.connectingResourceIRI = connectingResource;
	}
	
	
	public int getArcID() {
		return arcID;
	}
	
	public String getParentIRI() {
		return parentIRI;
	}
	
	public String getChildIRI() {
		return childIRI;
	}
	
	public String getConnectingResourceIRI() {
		return connectingResourceIRI;
	}
}
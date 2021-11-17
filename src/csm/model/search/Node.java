package csm.model.search;

import java.io.Serializable;
import java.util.Collection;

public class Node implements Comparable<Node>, Serializable {
	private static final long serialVersionUID = 1L;
	
	// Variable
	private Node parent;
	private int parentG;
	private int arcCost;
	
	// Constant
	private String URI;
	private int h;
	private String connectingResource;
		
	public Node(String URI, Node parent, String connectingResource, int parentG, int arcCost, int h) {
		this.URI = URI;
		this.parent = parent;
		this.connectingResource = connectingResource;
		this.parentG = parentG;
		this.arcCost = arcCost;
		this.h = h;
	}
	
	public String getConnectingResource() {
		return connectingResource;
	}
	
	public String getURI() {
		return URI;
	}
	
	public Node getParent() {
		return parent;
	}
	
	public void setH(int h) {	// set to 0 to show that heuristic is not used in the algorithm
		this.h = h;
	}
	
	public int getH() {
		return h;
	}
	
	public void setParentG(int parentG) {
		this.parentG = parentG;
	}
	
	public int getParentG() {
		return parentG;
	}
	
	public void setArcCost(int arcCost) {
		this.arcCost = arcCost;
	}
	
	public int getArcCost() {
		return arcCost;
	}
	
	public int getG() {
		// static
		return parentG + arcCost; 		

		// dynamic
		/*if (parent == null)
			return 0;
		else
			return parent.getG() + arcCost;*/
	}
	
	public int getCost() {
		return getG() + h;
	}

	@Override
	public int compareTo(Node node) {
		if (getCost() == node.getCost()) {
			return 0;
		}else if (getCost() < node.getCost()) {
			return -1;
		}else {
			return 1;
		}
	}
	
	public Node existsIn(Collection<Node> list) {
		for (Node node : list) {
			if (isSame(node)) {
				return node;
			}
		}
		return null;
	}

	public boolean isSame(Node node) {
		return this.URI.equals(node.URI);
	}

	public boolean isIdentical(Node node) {
		return isSame(node) && (this.getG() == node.getG());
	}
}

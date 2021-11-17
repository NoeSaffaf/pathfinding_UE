package csm.model.search;

import java.io.Serializable;

public class Goal implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private String goalURI;
	
	public Goal(String goalURI){
		this.goalURI = goalURI;
	}
	
	public String getURI() {
		return goalURI;
	}
	
	public boolean isGoal(String nodeIRI){
		return goalURI.equals(nodeIRI);
	}
}

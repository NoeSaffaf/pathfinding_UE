package csm.communication.routing;

import java.io.Serializable;
import java.util.List;

import csm.model.search.Arc;

// Request send by a search agent to find another search agent responsible for a given node
public class FindAgentResponsibleRequest implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private Arc arc;
	private String requesterAgentName;
	private List<String> ancestorList;
	
	public FindAgentResponsibleRequest(Arc arc, String requesterAgentName, List<String> ancestorList){
		this.arc = arc;
		this.requesterAgentName = requesterAgentName;
		this.ancestorList = ancestorList;
	}
	
	
	public Arc getArc(){
		return arc;
	}
	
	public String getRequesterAgent(){
		return requesterAgentName;
	}
	
	public List<String> getAncestorList(){
		return ancestorList;
	}
}

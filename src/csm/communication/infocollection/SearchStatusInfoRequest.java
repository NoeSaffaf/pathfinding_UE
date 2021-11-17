package csm.communication.infocollection;

import java.io.Serializable;

// Request sent to search agents when the solution is found to collect the stats
public class SearchStatusInfoRequest implements Serializable{
	private static final long serialVersionUID = 1L;
	
	// Empty for now -> used for dynamics
	// ...

	private String senderAgent;
	private String requesterAgent;
	
	public SearchStatusInfoRequest(String requesterAgent, String senderAgent) {
		this.requesterAgent = requesterAgent;
		this.senderAgent = senderAgent;
	}
	
	public String getRequester() {
		return requesterAgent;
	}
	
	public String getSender() {
		return senderAgent;
	}
}

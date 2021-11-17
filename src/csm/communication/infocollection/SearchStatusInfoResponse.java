package csm.communication.infocollection;

import java.io.Serializable;

// Response to SearchStatusInfoRequest
public class SearchStatusInfoResponse implements Serializable{
	private static final long serialVersionUID = 1L;

	private int expands; // number of nodes expanded by the search agent sending the message
	private int requests; // number of requests sent to resource agents by the search agent sending the message
	
	public SearchStatusInfoResponse(int expands, int requests) {
		this.expands = expands;
		this.requests = requests;
	}
	
	public int getExpands() {
		return expands;
	}
	
	public int getRequests() {
		return requests;
	}
}
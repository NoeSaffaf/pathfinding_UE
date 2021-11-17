package csm.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import csm.communication.ConversationIDs;
import csm.communication.dynamics.*;
import csm.communication.gvp.GVPRequest;
import csm.communication.gvp.GVPResponse;
import csm.communication.infocollection.SearchStatusInfoRequest;
import csm.communication.routing.FindAgentResponsibleRequest;
import csm.communication.routing.FindAgentResponsibleResponseToRequesterAgent;
import csm.model.gvp.GoalUnderVerification;
import csm.model.search.Arc;
import csm.model.search.Goal;
import csm.model.search.Node;
import csm.utility.GraphUtility;
import csm.utility.PlatformUtility;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;


public class NetworkAgent extends CSMAgent {
	private static final long serialVersionUID = 1L;

	// Criteria for creating an agent
	private int NA_MAX_OE = 10; // default OEs of the same level
	private int SA_MAX_RESPONSIBLE_NODES = 100; // default Heuristics for load distribution decision (more to be added)

	// Platform attributes
	List<String> agentContainers;

	// CSM attributes
	private List<String> resourceAgentNames;
	private String parentAgentName;
	private List<String> responsibledOE;
	private Map<String, List<String>> childSearchAgentsAndRespNode;
	private Map<String, List<String>> childNetworkAgentsAndRespOE;
	private int IDChildAgent = 1;

	// Routing protocol attributes
	private enum RoutingProtocol {
		DIRECT, INDIRECT, UNRELATED
	}

	// Goal verification attributes
	private GoalUnderVerification currentGoal = null;
	private int numVerificationRequired;

	// Search attributes
	private Goal goal;
	
	// Used in dynamic handling
	private Map<String, String> forwardNodeAgents;
	

//-------------------------------------------------------------- Setup block	
	protected void setup() {
		initAttributes();
		retrieveDataFromArguments();
		addBehaviour(new ProcessMessageBehaviour());
	}

	private void initAttributes() {
		responsibledOE = new ArrayList<>();
		childSearchAgentsAndRespNode = new HashMap<>();
		childNetworkAgentsAndRespOE = new HashMap<>();
		forwardNodeAgents = new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	private void retrieveDataFromArguments() {
		// Retrieve data passed to the agent
		Object[] arguments = getArguments();

		// Responsibled OE
		if (arguments.length > 0)
			responsibledOE.add((String) arguments[0]);
		// Parent network agent
		if (arguments.length > 1)
			parentAgentName = (String) arguments[1];
		// Child search agents and their responsibled nodes
		if (arguments.length > 2 && arguments[2] != null) {
			List<String> responsibledNodes = null;
			if (arguments.length > 3 && arguments[3] != null)
				responsibledNodes = (List<String>) arguments[3];
			childSearchAgentsAndRespNode.put((String) arguments[2], responsibledNodes);
		}
		// Child network agents and their responsibled OE(s)
		if (arguments.length > 4 && arguments[4] != null) {
			List<String> responsibledEntities = null;
			if (arguments.length > 5 && arguments[5] != null)
				responsibledEntities = (List<String>) arguments[5];
			childNetworkAgentsAndRespOE.put((String) arguments[4], responsibledEntities);
		}
		// Goal (destination IRI and isGoal())
		if (arguments.length > 6)
			goal = (Goal) arguments[6];
		// Resource agents
		if (arguments.length > 7)
			resourceAgentNames = (List<String>) arguments[7];
		// Agent containers
		if (arguments.length > 8)
			agentContainers = (List<String>) arguments[8];
		// NA_MAX_OE
		if (arguments.length > 9)
			NA_MAX_OE = (Integer) arguments[9];
		// SA_MAX_RESPONSIBLE_NODES
		if (arguments.length > 10)
			SA_MAX_RESPONSIBLE_NODES = (Integer) arguments[10];

	}

//-------------------------------------------------------------- Behaviour block	
	class ProcessMessageBehaviour extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			while (myAgent.getCurQueueSize() > 0) { // process all messages at once
				ACLMessage msg = myAgent.receive();
				if (msg != null) {
					// Routing-related messages
					if (msg.getConversationId().equals(ConversationIDs.FIND_AGENT_RESPONSIBLE)) {
						processFindAgentResponsibleMessage(msg);
					} else if (msg.getConversationId().equals(ConversationIDs.ASSIGN_ORGANISATIONAL_ENTITY)) {
						processAddOrganisationalEntityMessage(msg);
					// GVP-related messages
					} else if (msg.getConversationId().equals(ConversationIDs.PROPAGATE_GVP_REQUEST)) {
						processRequestGoalVerificationMessage(msg);
					} else if (msg.getConversationId().equals(ConversationIDs.PROPAGATE_GVP_RESPONSE)) {
						processResponseGoalVerificationMessage(msg);
					} else if (msg.getConversationId().equals(ConversationIDs.PROPAGATE_SEARCH_STATUS_INFO_REQUEST)) {
						processPropagateSearchStatusInfoRequest(msg);
					// Dynamic handling messages
					} else if (msg.getConversationId().equals(ConversationIDs.REQUEST_TAKE_CHARGE_NODE_IN_PATH)) {
						processRequestTakeChargeNodeInPath(msg);
					} else if (msg.getConversationId().equals(ConversationIDs.REQUEST_FORWARD_NODE_INFO_TO_GOAL_AGENT)) {
						processRequestForwardNodeInfoToGoalAgent(msg);
					} else if (msg.getConversationId().equals(ConversationIDs.REQUEST_SUBSCRIBE_SUCCESSOR_NODE_UPDATES)) {
						processRequestSubscribeSuccessorNodeUpdates(msg);
					} else if (msg.getConversationId().equals(ConversationIDs.REQUEST_SUCCESSOR_NODE_INFO)) {
						processRequestSuccessorNodeInfo(msg);
					} else if (msg.getConversationId().equals(ConversationIDs.REQUEST_RESUME_SEARCH)){
						processRequestResumeSearch(msg);
					} else if (msg.getConversationId().equals(ConversationIDs.REQUEST_REEXPAND_SUCCESSOR_NODE)){
						processRequestReexpandSuccessorNode(msg);
					}
				}
			}
			block();
		}
	}


	//-------------------------------------------------------------- Routing block
	private void processFindAgentResponsibleMessage(ACLMessage message) {
		FindAgentResponsibleRequest request = retrieveRequestFromFindAgentResponsibleMessage(message);
		List<String> ancestorList = request.getAncestorList(); // Bottom-up list (leaf to root)
		Arc arc = request.getArc(); // The arc pointing to the node being routed to the search agent responsible
		String requesterAgent = request.getRequesterAgent(); // the agent that initiated the routing protocol (i.e., the
																// agent that found the arc)
		String matchedOE = getMatchedOE(ancestorList); // Check if the network agent is in charge of one of the OEs of
														// the node

		RoutingProtocol selectedProtocol = selectRoutingProtocol(matchedOE, ancestorList);
		switch (selectedProtocol) {
		case DIRECT:
			if (forwardNodeAgents.containsKey(arc.getchildIRI())) {
				forwardNodeUpdatesToGoalAgent(arc, requesterAgent);
			} else {
				determineChildSearchAgentToBeAgentResponsible(arc, requesterAgent);
			}
			break;
		case INDIRECT:
			askChildNetworkAgentToFindAgentResponsible(matchedOE, ancestorList, arc, requesterAgent);
			break;
		default:
			askParentNetworkAgentToFindAgentResponsible(arc, requesterAgent, ancestorList);
			break;
		}
	}

	private FindAgentResponsibleRequest retrieveRequestFromFindAgentResponsibleMessage(ACLMessage message) {
		FindAgentResponsibleRequest request = null;
		try {
			request = (FindAgentResponsibleRequest) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return request;
	}

	private String getMatchedOE(List<String> ancestorList) {
		String matchedOE = null;
		for (String oe : responsibledOE) {
			if (ancestorList.contains(oe)) {
				matchedOE = oe;
				break;
			}
		}
		return matchedOE;
	}

	private RoutingProtocol selectRoutingProtocol(String matchedOE, List<String> ancestorList) {
		if (matchedOE != null) {
			if (ancestorList.get(0).equals(matchedOE)) { // It's the direct parent of the node
				return RoutingProtocol.DIRECT;
			} else { // It's not the direct parent of the node
				return RoutingProtocol.INDIRECT;
			}
		} else { // It's not the direct nor indirect parent
			return RoutingProtocol.UNRELATED;
		}
	}

// Routing protocol: direct parent of the node
	private void determineChildSearchAgentToBeAgentResponsible(Arc arc, String requesterAgent) {
		String chosenSearchAgent = null;
		if (childSearchAgentsAndRespNode.isEmpty()) { // No children -> create a new search agent to be the agent
														// responsible
			chosenSearchAgent = createNewSearchAgentToBeAgentResponsible(arc);
		} else {
			chosenSearchAgent = findAgentResponsibleAmongChildSearchAgents(arc.getchildIRI()); // Select an existing search agent to
																							   // be the agent responsible
			if (chosenSearchAgent != null) { // Agent responsible is among the child search agents
				PlatformUtility.sendMessage(this, ACLMessage.REQUEST, chosenSearchAgent, arc,
						ConversationIDs.REQUEST_SEARCH_AGENT_TO_HANDLE_ACTION);
			} else {
				chosenSearchAgent = chooseAgentResponsibleAmongChildSearchAgents(arc);
				if (chosenSearchAgent != null) { // the least load agent can handle the new node
					assignNewNodeToSearchAgent(arc, chosenSearchAgent);
				} else { // the least load agent is full
					chosenSearchAgent = createNewSearchAgentToBeAgentResponsible(arc); // Create new child to be the
																						// agent responsible
				}
			}
		}
		// Inform the search agent that initated the routing protocol
		informAgentRequesterAboutAgentInCharge(requesterAgent, chosenSearchAgent, arc.getchildIRI());
	}
	
	private void informAgentRequesterAboutAgentInCharge(String requesterAgent, String chosenSearchAgent, String nodeURI) {
		// Inform the search agent that initated the routing protocol
		PlatformUtility.sendMessage(this, ACLMessage.INFORM, requesterAgent,
				new FindAgentResponsibleResponseToRequesterAgent(chosenSearchAgent, nodeURI),
				ConversationIDs.INFORM_AGENT_RESPONSIBLE_TO_AGENT_REQUESTER);
	}

	private String createNewSearchAgentToBeAgentResponsible(Arc arc) {
		AgentController searchAgent = PlatformUtility.createAndStartSearchAgent(getContainerController(),
				generateNewChildSearchAgentName(), null, resourceAgentNames, this.getName(), arc, goal,
				agentContainers);
		String searchAgentName = null;
		try {
			searchAgentName = searchAgent.getName();
			// update local information of child search agents
			List<String> responsibilities = new ArrayList<>();
			responsibilities.add(arc.getchildIRI());
			childSearchAgentsAndRespNode.put(searchAgentName, responsibilities);
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
		return searchAgentName;
	}

	private String findAgentResponsibleAmongChildSearchAgents(String nodeURI) {
		String searchAgentInCharge = null;
		for (Map.Entry<String, List<String>> entry : childSearchAgentsAndRespNode.entrySet()) {
			List<String> responsibledNodes = entry.getValue();
			if (responsibledNodes.contains(nodeURI)) { // Search agent found
				searchAgentInCharge = entry.getKey();
				break;
			}
		}
		return searchAgentInCharge;
	}

	// Choose based on agent's loads
	private String chooseAgentResponsibleAmongChildSearchAgents(Arc arc) {
		String leastLoadAgent = getChildSearchAgentWithLeastLoad();
		int leastLoad = childSearchAgentsAndRespNode.get(leastLoadAgent).size();
		if (leastLoad < SA_MAX_RESPONSIBLE_NODES) {
			return leastLoadAgent;
		}
		return null;
	}

	private String getChildSearchAgentWithLeastLoad() {
		if (childSearchAgentsAndRespNode.isEmpty())
			return null;
		String leastLoadAgent = null;
		int leastLoad = 0;
		for (Map.Entry<String, List<String>> entry : childSearchAgentsAndRespNode.entrySet()) {
			List<String> responsibilities = entry.getValue();
			if (leastLoadAgent == null || leastLoad > responsibilities.size()) {
				leastLoadAgent = entry.getKey();
				leastLoad = responsibilities.size();
			}
		}
		return leastLoadAgent;
	}

	private void assignNewNodeToSearchAgent(Arc arc, String agentName) {
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, agentName, arc,
				ConversationIDs.ASSIGN_NEW_NODE_VIA_ACTION);
		childSearchAgentsAndRespNode.get(agentName).add(arc.getchildIRI()); // update local information of child search
																			// agents
	}

// Routing protocol: indirect parent of the node
	private void askChildNetworkAgentToFindAgentResponsible(String matchedOE, List<String> ancestorList, Arc arc,
			String requesterAgent) {
		String childNetworkAgent = null;
		String childOEOfMatchedOE = getChildOEOfMatchedOE(matchedOE, ancestorList);
		if (childNetworkAgentsAndRespOE.isEmpty()) {
			childNetworkAgent = createNetworkAgentAndAssignOE(childOEOfMatchedOE);
		} else {
			childNetworkAgent = getChildNetworkAgentInChargeOE(childOEOfMatchedOE);
			if (childNetworkAgent == null) { // childOEofMatchedOE has no network agent in charge yet
				childNetworkAgent = chooseAgentResponsibledAmongChildNetworkAgents();
				if (childNetworkAgent == null) { // all child agents are full
					childNetworkAgent = createNetworkAgentAndAssignOE(childOEOfMatchedOE);
				} else { // agent in charge chosen from the child agents
					assignNewHierarcyEntityToExistingChild(childOEOfMatchedOE, childNetworkAgent);
				}
			}
		}
		// Forward the routing request to the child network agent
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, childNetworkAgent,
				new FindAgentResponsibleRequest(arc, requesterAgent, ancestorList),
				ConversationIDs.FIND_AGENT_RESPONSIBLE);
	}

	private String getChildOEOfMatchedOE(String matchedOE, List<String> ancestorList) {
		int ancestorPosition = ancestorList.indexOf(matchedOE);
		return ancestorList.get(ancestorPosition - 1);
	}

	private String getChildNetworkAgentInChargeOE(String OE) {
		String childNetworkAgent = null;
		for (Map.Entry<String, List<String>> entry : childNetworkAgentsAndRespOE.entrySet()) {
			List<String> responsibilities = entry.getValue();
			if (responsibilities != null && responsibilities.contains(OE)) {
				childNetworkAgent = entry.getKey();
				break;
			}
		}
		return childNetworkAgent;
	}

	private String createNetworkAgentAndAssignOE(String OE) {
		AgentController networkAgent = PlatformUtility.createAndStartNetworkAgent(getContainerController(),
				generateNewChildNetworkAgentName(OE), OE, getName(), null, new ArrayList<>(), null, new ArrayList<>(),
				goal, resourceAgentNames, agentContainers, NA_MAX_OE, SA_MAX_RESPONSIBLE_NODES);
		String newNetworkAgent = null;
		try {
			newNetworkAgent = networkAgent.getName();
			// Update
			List<String> responsibilities = new ArrayList<>();
			responsibilities.add(OE);
			childNetworkAgentsAndRespOE.put(newNetworkAgent, responsibilities);
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
		return newNetworkAgent;
	}

	private String chooseAgentResponsibledAmongChildNetworkAgents() {
		String leastLoadAgent = getChildNetworkAgentWithLeastLoad();
		if (childNetworkAgentsAndRespOE.get(leastLoadAgent).size() < NA_MAX_OE) {
			return leastLoadAgent;
		}
		//println(leastLoadAgent + " - " + childNetworkAgentsAndRespOE.get(leastLoadAgent) + "\n");
		return null;
	}

	private String getChildNetworkAgentWithLeastLoad() {
		String leastLoadAgent = null;
		int load = 0;
		for (Map.Entry<String, List<String>> entry : childNetworkAgentsAndRespOE.entrySet()) {
			List<String> responsibilities = entry.getValue();
			if (leastLoadAgent == null || load > responsibilities.size()) {
				leastLoadAgent = entry.getKey();
				load = responsibilities.size();
			}
		}
		return leastLoadAgent;
	}

	private void assignNewHierarcyEntityToExistingChild(String OE, String childNetworkAgent) {
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, childNetworkAgent, OE,
				ConversationIDs.ASSIGN_ORGANISATIONAL_ENTITY);
		childNetworkAgentsAndRespOE.get(childNetworkAgent).add(OE); // update
	}

// Routing protocol: not direct nor indirect parent of the node
	private void askParentNetworkAgentToFindAgentResponsible(Arc arc, String requesterAgent,
			List<String> ancestorList) {
		if (parentAgentName == null) { // Only happen to HierarchyAgent because SearchAgents, except the first one, are
										// created by HierarchyAgents
			String OE = GraphUtility.getParentEntityFromURI(responsibledOE.get(0));
			if (OE == null)
				return; // Reached the top of the hierarchy
			createParentNetworkAgentAndAssignOE(OE);
		}
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, parentAgentName,
				new FindAgentResponsibleRequest(arc, requesterAgent, ancestorList),
				ConversationIDs.FIND_AGENT_RESPONSIBLE);
	}

	private void createParentNetworkAgentAndAssignOE(String OE) {
		AgentController networkAgent = PlatformUtility.createAndStartNetworkAgent(getContainerController(),
				generateNewChildNetworkAgentName(OE), OE, null, null, null, getName(), responsibledOE, goal,
				resourceAgentNames, agentContainers, NA_MAX_OE, SA_MAX_RESPONSIBLE_NODES);
		// Update
		try {
			parentAgentName = networkAgent.getName();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

// Child network agent receiving OE assignment
	private void processAddOrganisationalEntityMessage(ACLMessage message) {
		try {
			String organisationalEntityIRI = (String) message.getContentObject();
			if (responsibledOE.contains(organisationalEntityIRI)) {
			} else { // prevent duplicates due to delay in processing OE assignement message
				responsibledOE.add(organisationalEntityIRI);
			}
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}

//-------------------------------------------------------------- GVP block
	private void processRequestGoalVerificationMessage(ACLMessage message) {
		GVPRequest request = retrieveGVPRequestObject(message);
		Node goalNode = request.getGoalNode();
		String senderAgent = request.getSenderAgent(); // agent that sent the message
		String requesterAgent = request.getGoalAgent(); // agent that initiated GVP
		if (currentGoal == null || currentGoal.getGoalNode().getCost() > goalNode.getCost()) { // if received goal is
																								// better than the
																								// current one
			setupGoalForLocalVerification(goalNode, requesterAgent, senderAgent);
			numVerificationRequired = propagateGVPMessage(goalNode, requesterAgent, senderAgent);
			if (numVerificationRequired == 0) { // No children and parent -> Auto verified
				PlatformUtility.sendMessage(this, ACLMessage.INFORM, senderAgent,
						new GVPResponse(currentGoal.getGoalNode(), true), ConversationIDs.PROPAGATE_GVP_RESPONSE);
			}
		}
	}

	private GVPRequest retrieveGVPRequestObject(ACLMessage message) {
		GVPRequest request = null;
		try {
			request = (GVPRequest) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return request;
	}

	private void setupGoalForLocalVerification(Node goalNode, String requesterAgent, String senderAgent) {
		currentGoal = new GoalUnderVerification(goalNode, requesterAgent, senderAgent);
	}

	private int propagateGVPMessage(Node goalNode, String requesterAgent, String senderAgent) {
		int numberOfRequiredResponse = 0;
		GVPRequest request = new GVPRequest(goalNode, requesterAgent, getName());

		// To parent
		if (parentAgentName != null && !parentAgentName.equals(senderAgent)) {
			sendPropagateGVPRequest(request, parentAgentName);
			numberOfRequiredResponse++;
		}

		// To child network agents
		for (String childAgent : childNetworkAgentsAndRespOE.keySet()) {
			if (childAgent.equals(senderAgent))
				continue;
			sendPropagateGVPRequest(request, childAgent);
			numberOfRequiredResponse++;
		}

		// To child search agents
		for (String childAgent : childSearchAgentsAndRespNode.keySet()) {
			if (childAgent.equals(senderAgent))
				continue;
			sendPropagateGVPRequest(request, childAgent);
			numberOfRequiredResponse++;
		}

		return numberOfRequiredResponse;
	}

	private void sendPropagateGVPRequest(GVPRequest request, String destination) {
		PlatformUtility.sendMessage(this, ACLMessage.PROPAGATE, destination, request,
				ConversationIDs.PROPAGATE_GVP_REQUEST);
	}

	private void processResponseGoalVerificationMessage(ACLMessage message) {
		GVPResponse obj;
		try {
			obj = (GVPResponse) message.getContentObject();
			Node goalNode = obj.getGoalNode();
			String senderAgent = currentGoal.getSenderAgent();
			if (goalNode.isIdentical(currentGoal.getGoalNode())) {
				currentGoal.addAVerificationResponse();
			}
			if (currentGoal.getNumberOfVerificationResponse() == numVerificationRequired) {
				PlatformUtility.sendMessage(this, ACLMessage.INFORM, senderAgent,
						new GVPResponse(currentGoal.getGoalNode(), true), ConversationIDs.PROPAGATE_GVP_RESPONSE);
			}
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}

	// Search agent receiving the message pauses the search process and reply to the Goal agent Expands & Requests
	private void processPropagateSearchStatusInfoRequest(ACLMessage message) {
		SearchStatusInfoRequest verifiedGoal = retrieveSearchInfoRequestFromMessage(message);
		String senderAgent = verifiedGoal.getSender();
		String requesterAgent = verifiedGoal.getRequester();
		SearchStatusInfoRequest request = new SearchStatusInfoRequest(requesterAgent, getName());
		// To parent
		if (parentAgentName != null && !parentAgentName.equals(senderAgent)) {
			sendPropagateVerifiedGoalMessage(request, parentAgentName);
		}
		// To child network agents
		for (String childAgent : childNetworkAgentsAndRespOE.keySet()) {
			if (childAgent.equals(senderAgent))
				continue;
			sendPropagateVerifiedGoalMessage(request, childAgent);
		}
		// To child search agents
		for (String childAgent : childSearchAgentsAndRespNode.keySet()) {
			if (childAgent.equals(senderAgent))
				continue;
			sendPropagateVerifiedGoalMessage(request, childAgent);
		}
	}

	private SearchStatusInfoRequest retrieveSearchInfoRequestFromMessage(ACLMessage message) {
		SearchStatusInfoRequest verifiedGoal = null;
		try {
			verifiedGoal = (SearchStatusInfoRequest) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return verifiedGoal;
	}

	private void sendPropagateVerifiedGoalMessage(SearchStatusInfoRequest request, String destination) {
		PlatformUtility.sendMessage(this, ACLMessage.PROPAGATE, destination, request,
				ConversationIDs.PROPAGATE_SEARCH_STATUS_INFO_REQUEST);
	}


//-------------------------------------------------------------- Dynamic block
	private void processRequestSuccessorNodeInfo(ACLMessage message) {

		RequestSuccessorNodeInfo request = retrieveRemoteSuccessorInfoRequestFromMessage(message);
		List<String> ancestorList = request.getAncestorList();
		String matchedOE = getMatchedOE(ancestorList);
		String agentReceiver = null;
		RoutingProtocol selectedProtocol = selectRoutingProtocol(matchedOE, ancestorList);
		switch (selectedProtocol) {
			case DIRECT:
				agentReceiver = findAgentResponsibleAmongChildSearchAgents(request.getNodeURI());
				break;
			case INDIRECT:
				String childOEOfMatchedOE = getChildOEOfMatchedOE(matchedOE, ancestorList);
				agentReceiver = getChildNetworkAgentInChargeOE(childOEOfMatchedOE);
				break;
			default:
				agentReceiver = parentAgentName;
				break;
		}
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, agentReceiver, request, ConversationIDs.REQUEST_SUCCESSOR_NODE_INFO);
	}

	private RequestSuccessorNodeInfo retrieveRemoteSuccessorInfoRequestFromMessage(ACLMessage message) {
		RequestSuccessorNodeInfo request = null;
		try {
			request = (RequestSuccessorNodeInfo) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return request;
	}


 //-------------------------------------------------------------- Dynamic: Take charge
	/**
	 * Forward the request to the agent responsisble for the node
	 * @param message
	 */
	private void processRequestTakeChargeNodeInPath(ACLMessage message) {
		TakeChargeNodeInPathRequest request = retrieveTakeChargeNodeInPathRequestFromMessage(message);
		List<String> ancestorList = request.getAncestorList();
		String matchedOE = getMatchedOE(ancestorList);
		String agentReceiver = null;
		RoutingProtocol selectedProtocol = selectRoutingProtocol(matchedOE, ancestorList);
		switch (selectedProtocol) {
		case DIRECT:
			agentReceiver = findAgentResponsibleAmongChildSearchAgents(request.getNodeURI());
			break;
		case INDIRECT:
			String childOEOfMatchedOE = getChildOEOfMatchedOE(matchedOE, ancestorList);
			agentReceiver = getChildNetworkAgentInChargeOE(childOEOfMatchedOE);
			break;
		default:
			agentReceiver = parentAgentName;
			break;
		}
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, agentReceiver, request, ConversationIDs.REQUEST_TAKE_CHARGE_NODE_IN_PATH);
	}
	private TakeChargeNodeInPathRequest retrieveTakeChargeNodeInPathRequestFromMessage(ACLMessage message) {
		TakeChargeNodeInPathRequest request = null;
		try {
			request = (TakeChargeNodeInPathRequest) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return request;
	}
	
	/**
	 * Forward node info from a search agent to the goal agent
	 * @param message
	 */
	private void processRequestForwardNodeInfoToGoalAgent(ACLMessage message) {
		ForwardNodeUpdatesToGoalAgentRequest request = retrieveForwardNodeUpdatesToGoalAgentRequestFromMessage(message);
		forwardNodeAgents.put(request.getNodeURI(), request.getGoalAgent());
		//println("Forward: " + request.getNodeURI() + " - " + request.getGoalAgent());
	}
	private ForwardNodeUpdatesToGoalAgentRequest retrieveForwardNodeUpdatesToGoalAgentRequestFromMessage(ACLMessage message) {
		ForwardNodeUpdatesToGoalAgentRequest request = null;
		try {
			request = (ForwardNodeUpdatesToGoalAgentRequest) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return request;
	}
	
	private void forwardNodeUpdatesToGoalAgent(Arc arc, String requesterAgent) {
		String goalAgent = forwardNodeAgents.get(arc.getchildIRI());
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, goalAgent, arc,
				ConversationIDs.REQUEST_SEARCH_AGENT_TO_HANDLE_ACTION);
		// Inform the search agent that initated the routing protocol
		informAgentRequesterAboutAgentInCharge(requesterAgent, goalAgent, arc.getchildIRI());
	}
 
 //-------------------------------------------------------------- Dynamic: Subscribe successor updates	
	private void processRequestSubscribeSuccessorNodeUpdates(ACLMessage message) {
		SubscribeSuccessorNodeUpdatesRequest request = retrieveSubscribeSuccessorNodeUpdatesRequest(message);
		List<String> ancestorList = request.getAncestorList();
		String matchedOE = getMatchedOE(ancestorList);
		String agentReceiver = null;
		RoutingProtocol selectedProtocol = selectRoutingProtocol(matchedOE, ancestorList);
		switch (selectedProtocol) {
		case DIRECT:
			agentReceiver = findAgentResponsibleAmongChildSearchAgents(request.getNodeURI());
			break;
		case INDIRECT:
			String childOEOfMatchedOE = getChildOEOfMatchedOE(matchedOE, ancestorList);
			agentReceiver = getChildNetworkAgentInChargeOE(childOEOfMatchedOE);
			break;
		default:
			agentReceiver = parentAgentName;
			break;
		}
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, agentReceiver, request, ConversationIDs.REQUEST_SUBSCRIBE_SUCCESSOR_NODE_UPDATES);
	}
	
	private SubscribeSuccessorNodeUpdatesRequest retrieveSubscribeSuccessorNodeUpdatesRequest(ACLMessage message) {
		SubscribeSuccessorNodeUpdatesRequest request = null;
		try {
			request = (SubscribeSuccessorNodeUpdatesRequest) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return request;
	}

	//-------------------------------------------------------------- Dynamic: ReexpandSearch
	private void processRequestReexpandSuccessorNode(ACLMessage message) {
		ReexpandSuccessorNodeRequest request = retrieveReexpandSuccessorNodeRequest(message);
		List<String> ancestorList = request.getAncestorsList();
		String matchedOE = getMatchedOE(ancestorList);
		String agentReceiver = null;
		RoutingProtocol selectedProtocol = selectRoutingProtocol(matchedOE, ancestorList);
		switch (selectedProtocol) {
			case DIRECT:
				agentReceiver = findAgentResponsibleAmongChildSearchAgents(request.getNodeURI());
				break;
			case INDIRECT:
				String childOEOfMatchedOE = getChildOEOfMatchedOE(matchedOE, ancestorList);
				agentReceiver = getChildNetworkAgentInChargeOE(childOEOfMatchedOE);
				break;
			default:
				agentReceiver = parentAgentName;
				break;
		}
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, agentReceiver, request, ConversationIDs.REQUEST_REEXPAND_SUCCESSOR_NODE);
	}

	private ReexpandSuccessorNodeRequest retrieveReexpandSuccessorNodeRequest(ACLMessage message) {
		ReexpandSuccessorNodeRequest request = null;
		try {
			request = (ReexpandSuccessorNodeRequest) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return request;
	}

	//------------------------------------------------------------- Reexpand Search block
	private void processRequestResumeSearch(ACLMessage message) {
		ResumeSearchRequest request = retrieveResumeSearchRequest(message);
		String senderAgent = request.getSenderAgent(); // agent that sent the message
		String goalAgent = request.getGoalAgent();
		List<String> rootAffectedNodesList = request.getRootAffectedNodeList();
		propagateResumeMessage(goalAgent, senderAgent, rootAffectedNodesList);

	}

	private ResumeSearchRequest retrieveResumeSearchRequest(ACLMessage message){
		ResumeSearchRequest request = null;
		try {
			request = (ResumeSearchRequest) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return request;
	}

	private void propagateResumeMessage(String goalAgent, String senderAgent, List<String> rootAffectedNodesList){
		ResumeSearchRequest request = new ResumeSearchRequest(goalAgent,getName(), rootAffectedNodesList);

		// To parent
		if (parentAgentName != null && !parentAgentName.equals(senderAgent)) {
			sendPropagateResumeSearchRequest(request, parentAgentName);
		}

		// To child network agents
		for (String childAgent : childNetworkAgentsAndRespOE.keySet()) {
			if (childAgent.equals(senderAgent)) {
				continue;
			}
			sendPropagateResumeSearchRequest(request, childAgent);
		}

		// To child search agents
		for (String childAgent : childSearchAgentsAndRespNode.keySet()) {
			if (childAgent.equals(senderAgent)) {
				continue;
			}
			sendPropagateResumeSearchRequest(request, childAgent);
		}
	}

	private void sendPropagateResumeSearchRequest(ResumeSearchRequest request, String destination)
	{
		PlatformUtility.sendMessage(this, ACLMessage.PROPAGATE, destination, request,
				ConversationIDs.REQUEST_RESUME_SEARCH);
	}

//-------------------------------------------------------------- Utility functions block
	private String generateNewChildSearchAgentName() {
		return "Sa_" + IDChildAgent++ + "-" + getLocalName();
	}

	private String generateNewChildNetworkAgentName(String childResponsibledHierarchyIRI) {
		return "Na_" + GraphUtility.getOrganisationalEntitysNameFromIRI(childResponsibledHierarchyIRI);
	}
}
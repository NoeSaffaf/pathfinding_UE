package csm.agent;

import java.util.*;

import csm.Evaluation.LatencyHolder;
import csm.communication.ConversationIDs;
import csm.communication.arccost.ArcCostRequest;
import csm.communication.arccost.ArcCostResponse;
import csm.communication.dynamics.*;
import csm.communication.gvp.GVPRequest;
import csm.communication.gvp.GVPResponse;
import csm.communication.infocollection.SearchStatusInfoRequest;
import csm.communication.infocollection.SearchStatusInfoResponse;
import csm.communication.routing.FindAgentResponsibleRequest;
import csm.communication.routing.FindAgentResponsibleResponseToRequesterAgent;
import csm.model.dynamics.ImportedNode;
import csm.model.dynamics.MonitoredNode;
import csm.model.dynamics.PathNodeSuccessor;
import csm.model.gvp.GoalUnderVerification;
import csm.model.search.Arc;
import csm.model.search.Goal;
import csm.model.search.Node;
import csm.model.search.PathFindingProblem;
import csm.model.solution.Solution;
import csm.scenario.Main;
import csm.utility.GraphUtility;
import csm.utility.ListUtility;
import csm.utility.PlatformUtility;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

public class SearchAgent extends CSMAgent {
	private static final long serialVersionUID = 1L;

	// Stats
	private static final double PENDING_TIME_LIMIT = 60000;
	private int expands = 0;
	private int requests = 0;
	private int totalExpands = 0;
	private int totalRequests = 0;
	private int expectedStatisticsResponse = 0;
	private long endTime;
	
	// Dynamic hanlding
	private String goalAgentName;
	private boolean inWaitingMode = false;
	private boolean dynamicCase = false;
	private Solution solution;
	private List<ImportedNode> importedNodes; // nodes in initial solution path and take over from other search agents
	private List<MonitoredNode> monitoredNodes; // nodes to monitor for changes
	private List<Node> nodesInPath;
	private Map<String, List<PathNodeSuccessor>> pathNodeSuccessors;
	private int takeChargeNodesPendingResponse = 0;
	private int collectRemoteSuccessorsInfoPendingResponse = 0;
	private List<String[]> pendingListToCompareSuccessors;

	//Testing
	private boolean test1 = false;
	private boolean test2 = false;
	private boolean test3 = false;

	// Platform
	List<String> containers;
	
	// Problem-specific attributes
	private Goal goal;
	private List<String> resourceAgentNames;
	
	// Agent network
	private String parentAgentName;
	
	// Search attributes
	private Queue<Node> openList;
	private Set<Node> closedList;
	private Queue<Arc> arcList;
	private Set<Arc> arcPendingVerification;
	private Map<Integer, Arc> pendingList;
	private Set<String> responsibledNodes;
	private Map<String, String> mapNodeAndAgentInResponsible;
	private int arcID = 1;
	
	// GVP attributes
	private GoalUnderVerification currentGoal = null;
	private boolean goalVerifiedLocally = false;
	private boolean isGoalAgent = false;
	private int numVerificationRequired;
	
	// Behaviours
	private Behaviour processMessageBehaviour;
	private Behaviour expandBehaviour;
	private Behaviour computeArcCostBehaviour;
	private Behaviour verifyGoalLocallyBehaviour;
	
	
//-------------------------------------------------------------- Setup block		
	protected void setup() {
		initAttributes();
		retrieveDataFromArguments();
		addBehaviour(processMessageBehaviour);
		addBehaviour(expandBehaviour);
		addBehaviour(computeArcCostBehaviour);
		addBehaviour(verifyGoalLocallyBehaviour);
	}
	
	private void initAttributes() {
		openList = new PriorityQueue<>();
		closedList = new HashSet<>();
		arcList = new PriorityQueue<>();
		pendingList = new HashMap<>();
		responsibledNodes = new HashSet<>();
		mapNodeAndAgentInResponsible = new HashMap<>();
		arcPendingVerification = new HashSet<>();
		
		// Dynamic
		importedNodes = new ArrayList<>();
		monitoredNodes = new ArrayList<>();
		pathNodeSuccessors = new HashMap<>();
		pendingListToCompareSuccessors = new ArrayList<String[]>();
		
		processMessageBehaviour = new ProcessMessageBehaviour();
		expandBehaviour = new ExpandBehaviour();
		computeArcCostBehaviour = new ComputeArcCostBehaviour();
		verifyGoalLocallyBehaviour = new VerifyUnverifiedGoalLocallyBehaviour();
	}

	@SuppressWarnings("unchecked")
	private void retrieveDataFromArguments() {
		Object[] arguments = getArguments();
		// Pathfinding problem
		if (arguments.length > 0 && arguments[0] instanceof PathFindingProblem) { // First search agent only
			PathFindingProblem problem = (PathFindingProblem) arguments[0];
			// Create the origin node and add to openList
			String originURI = problem.getOriginURI();
			Node origin = new Node(originURI, null, null, 0, 0, 0);
			origin.setH(heuristic(origin.getURI()));
			openList.offer(origin);
			// Update
			responsibledNodes.add(origin.getURI());
			// Goal
			goal = problem.getGoal();
		}
		// Resource agents
		if (arguments.length > 1)
			resourceAgentNames = (List<String>) arguments[1];
		// Parent network agent
		if (arguments.length > 2)
			parentAgentName = (String) arguments[2];
		// Arc for starting the search (the case for search agent created during the routing protocol)
		if (arguments.length > 3 && arguments[3] != null) { 
			Arc arc = (Arc) arguments[3];
			addArcToArcList(arc);
			responsibledNodes.add(arc.getchildIRI()); // Update
		}
		// Goal
		if (arguments.length > 4 && arguments[4] != null)
			goal = (Goal) arguments[4];
		if (arguments.length > 5 && arguments[5] != null)
			containers = (List<String>) arguments[5];
	}

	private int heuristic(String nodeURI) { // To customise according to the heuristic used
		return 0;
	}
	
//-------------------------------------------------------------- Behaviour block			
	class ProcessMessageBehaviour extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			while (myAgent.getCurQueueSize() > 0) { // process all messages at once
				ACLMessage message = myAgent.receive();
				// Arc cost
				if (message.getConversationId().equals(ConversationIDs.REQUEST_ARC_COST)) {
					processArcCostResponseMessage(message);
				} else if (message.getConversationId().equals(ConversationIDs.INFORM_UPDATED_ARC_COST)) {
					processInformUpdatedArcCostMessage(message);
				// Routing - node assignment
				} else if (message.getConversationId().equals(ConversationIDs.REQUEST_SEARCH_AGENT_TO_HANDLE_ACTION)) {
					processRequestToHandleActionMessage(message);
				} else if (message.getConversationId().equals(ConversationIDs.ASSIGN_NEW_NODE_VIA_ACTION)) {
					processNewNodeAssignmentViaActionMessage(message);
				} else if (message.getConversationId()
						.equals(ConversationIDs.INFORM_AGENT_RESPONSIBLE_TO_AGENT_REQUESTER)) {
					processInformAgentResponsibleMessage(message);
				// GVP
				} else if (message.getConversationId().equals(ConversationIDs.PROPAGATE_GVP_REQUEST)) {
					processPropagateGVPRequestMessage(message);
				} else if (message.getConversationId().equals(ConversationIDs.PROPAGATE_GVP_RESPONSE)) {
					processPropagateGVPResponseMessage(message);
				// Search status info collection
				} else if (message.getConversationId().equals(ConversationIDs.PROPAGATE_SEARCH_STATUS_INFO_REQUEST)) {
					processSearchStatusInfoRequest(message);
				} else if (message.getConversationId().equals(ConversationIDs.PROPAGATE_SEARCH_STATUS_INFO_RESPONSE)) {
					processSearchStatusInfoResponse(message);
				// Dynamic: take charge
				} else if (message.getConversationId().equals(ConversationIDs.REQUEST_TAKE_CHARGE_NODE_IN_PATH)) {
					processRequestTakeChargeNodeInPath(message);
				} else if (message.getConversationId().equals(ConversationIDs.RESPONSE_TAKE_CHARGE_NODE_IN_PATH)) {
					processResponseTakeChargeNodeInPath(message);
				} else if (message.getConversationId().equals(ConversationIDs.REQUEST_SUBSCRIBE_SUCCESSOR_NODE_UPDATES)) {
					processRequestSubscribeSuccessorNodeUpdates(message); // Goal agent to agents in charge of remote successor nodes
				} else if (message.getConversationId().equals(ConversationIDs.INFORM_REMOTE_SUCCESSOR_INFO)) {
					processRemoteSuccessorInfoMessage(message); // Successor agents to Goal agent
				// Dynamic: handle updates
				} else if (message.getConversationId().equals(ConversationIDs.REQUEST_SUCCESSOR_NODE_INFO)) {
					processRequestSuccessorNodeInfo(message); // Resource agent to search agent
				} else if (message.getConversationId().equals(ConversationIDs.NOTIFICATION_NODE_UPDATES)) {
					processNotificationNodeUpdates(message); // Resource agent to search agent
				} else if (message.getConversationId().equals(ConversationIDs.INFORM_REMOTE_SUCCESSOR_UPDATES)) {
					processRemoteSuccessorUpdatesMessage(message); // Successor agents to Goal agent
				} else if (message.getConversationId().equals(ConversationIDs.REQUEST_REEXPAND_SUCCESSOR_NODE)) {
					processReexpandSuccessorNodeMessage(message);
				} else if (message.getConversationId().equals(ConversationIDs.REQUEST_RESUME_SEARCH)) {
					processResumeSearch(message);
				}
			}
			block();
		}
	}


	/*
	 * Expand nodes
	 */
	class ExpandBehaviour extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			if (inWaitingMode)
				block();
			else 				
				expand();
		}
	}
	
	/*
	 * Compute the cost between 2 nodes
	 */
	class ComputeArcCostBehaviour extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			if (inWaitingMode) 
				block();
			else 
				computeArcCost();
		}
	}
	
	/**
	 * Verify goal locally
	 */
	class VerifyUnverifiedGoalLocallyBehaviour extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			if (inWaitingMode) {
				block();
			} else {
				if (currentGoal != null)
					verifyUnverifiedGoalLocally(); // GVP
			}
		}
	}

	
//-------------------------------------------------------------- Message handling block
  //-------------------------------------------------------------- Message handling: Arc cost
	// Remark: cost of an arc to a responsibled node
	private void processArcCostResponseMessage(ACLMessage message) {
		//handleResponseActionCostMessage(message);
		ArcCostResponse arcCostResponse = retrieveArcCostResponseFromMessage(message);
		int arcCost = arcCostResponse.getArcCost();
		Arc arc = removeArcFromPendingList(arcCostResponse);
		Node newNode = createNewNode(arc, arcCost);
		// Filtering arcs -> Missing arcs to a responsibled node
		updateArcPendingVerification(arc, newNode);
		// Filtering nodes -> Missing responsibled nodes (from OL or CL)
		if (!isMoreCostlyThanCurrentGoal(newNode)) {
			pruning(newNode);
		}
	}

	private ArcCostResponse retrieveArcCostResponseFromMessage(ACLMessage message) {
		ArcCostResponse responseObject = null;
		try {
			responseObject = (ArcCostResponse) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return responseObject;
	}

	private Arc removeArcFromPendingList(ArcCostResponse arcCostResponse) {
		int arcID = arcCostResponse.getArcID();
		return pendingList.remove(arcID); 
	}

	private Node createNewNode(Arc arc, int arcCost) {
		int h = heuristic(arc.getchildIRI()); // To be replaced by h(n) - heuristic function

		//Pour test
		/*
		if (arc.getParentNode().getURI().equals("http://127.0.0.1/Graph/location/location_3") && arc.getchildIRI().equals("http://127.0.0.1/Graph/location/location_5")) {
			Node newNode = new Node(arc.getchildIRI(), arc.getParentNode(), arc.getConnectingResourceURI(),
					arc.getParentNode().getG(), 10, h);
			return newNode;
		}*/

		Node newNode = new Node(arc.getchildIRI(), arc.getParentNode(), arc.getConnectingResourceURI(),
				arc.getParentNode().getG(), arcCost, h);
		return newNode;
	}

	private void updateArcPendingVerification(Arc arc, Node newNode) {
		List<Arc> iteratedArcs = new ArrayList<>();
		for (Arc arcUnderVerification : arcPendingVerification) {
			if (arcUnderVerification.getchildIRI().equals(arc.getchildIRI())) {
				iteratedArcs.add(arcUnderVerification);
				if (arcUnderVerification.getParentNode().getG() < newNode.getG()) {
					addArcToArcList(arcUnderVerification);
				}
			}
		}
		arcPendingVerification.removeAll(iteratedArcs);
	}

	private boolean isMoreCostlyThanCurrentGoal(Node newNode) {
		return (currentGoal != null && newNode.getG() >= currentGoal.getGoalNode().getG());
	}

	private void pruning(Node newNode) {
		Node newNodeInOL = newNode.existsIn(openList);
		Node newNodeInCL = newNode.existsIn(closedList);

		if (newNodeInOL == null && newNodeInCL == null) { // new node
			openList.offer(newNode);
		} else if (newNodeInOL != null && newNode.getG() < newNodeInOL.getG()) { // better node
			openList.remove(newNodeInOL);
			openList.offer(newNode);
		} else if (newNodeInCL != null && newNode.getG() < newNodeInCL.getG()) { // node been expanded
			// Local update - Update CL
			closedList.remove(newNodeInCL);
			closedList.add(newNode);
			// Update to relevant nodes
			updateAffectedNodes(newNode);
		}
	}

	private void updateAffectedNodes(Node updatedNode) {
		List<Arc> arcs = GraphUtility.getArcs(updatedNode); // *File access - Critical
		if (arcs == null)
			return;
		for (Arc arc : arcs) {
			String affectedNodeIRI = arc.getchildIRI();
			if (isAgentResponsible(affectedNodeIRI)) {
				handleArcToExistingResponsibledNode(arc); // Update parent of affected node is enough, why request again?
			} else {
				informAgentsInChargeOfAffectedNodes(affectedNodeIRI, arc);
			}
		}
	}
	private void informAgentsInChargeOfAffectedNodes(String affectedNodeIRI, Arc arc) {
		String agentInCharge = mapNodeAndAgentInResponsible.get(affectedNodeIRI);
		if (agentInCharge != null) {
			PlatformUtility.sendMessage(this, ACLMessage.REQUEST, agentInCharge, arc,
					ConversationIDs.INFORM_UPDATED_ARC_COST);
		} else {
			findAgentResponsible(arc); // Address the timing issue - in finding agent responsible
		}
	}
	
	private void processInformUpdatedArcCostMessage(ACLMessage message) {
		try {
			handleArcToExistingResponsibledNode((Arc)message.getContentObject());
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}

  //-------------------------------------------------------------- Message handling: Routing
	private void processRequestToHandleActionMessage(ACLMessage message) {
		try {
			handleArcToExistingResponsibledNode((Arc)message.getContentObject());
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}
	
	private void processNewNodeAssignmentViaActionMessage(ACLMessage message) {
		try {
			Arc arc = (Arc) message.getContentObject();
			addArcToArcList(arc);
			responsibledNodes.add(arc.getchildIRI()); // Update
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}
	
	// Inform the requester agent who is the agent in charge of the routed node
	private void processInformAgentResponsibleMessage(ACLMessage message) {
		try {
			FindAgentResponsibleResponseToRequesterAgent obj = (FindAgentResponsibleResponseToRequesterAgent) message
					.getContentObject();
			mapNodeAndAgentInResponsible.put(obj.getNode(), obj.getAgentInCharge());
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}

  //-------------------------------------------------------------- Message handling: GVP	
	private void processPropagateGVPRequestMessage(ACLMessage message) {
		GVPRequest gvpRequest;
		try {
			gvpRequest = (GVPRequest) message.getContentObject();
			Node goalNode = gvpRequest.getGoalNode();
			String goalAgent = gvpRequest.getGoalAgent();
			String senderAgent = gvpRequest.getSenderAgent(); 
			
			if (currentGoal == null || currentGoal.getGoalNode().getCost() > goalNode.getCost()) {
				currentGoal = new GoalUnderVerification(goalNode, goalAgent, senderAgent);
				isGoalAgent = false;
				goalVerifiedLocally = false;
			}
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}
	
	private void processPropagateGVPResponseMessage(ACLMessage message) {
		GVPResponse gvpResponse;
		try {
			gvpResponse = (GVPResponse) message.getContentObject();
			if (gvpResponse.isVerifiedLocally()) {
				Node goalNode = gvpResponse.getGoalNode();

				if (currentGoal!=null && goalNode.isIdentical(currentGoal.getGoalNode())) {
					currentGoal.addAVerificationResponse();
				}
			}
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}
	
 //-------------------------------------------------------------- Message handling: Info collection		
	private void processSearchStatusInfoRequest(ACLMessage message) {
		try {
			resetGVP();
			String goalAgent = ((SearchStatusInfoRequest) message.getContentObject()).getRequester();
			PlatformUtility.sendMessage(this, ACLMessage.INFORM, goalAgent,
					new SearchStatusInfoResponse(expands, requests),
					ConversationIDs.PROPAGATE_SEARCH_STATUS_INFO_RESPONSE);
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}
	
	private void processSearchStatusInfoResponse(ACLMessage message) {
		try {
			SearchStatusInfoResponse response = (SearchStatusInfoResponse)message.getContentObject();
			int receivedRequests = response.getRequests();
			int receivedExpands = response.getExpands();
			totalExpands += receivedExpands;
			totalRequests += receivedRequests;
			showExecutionDetails();

			/*if (--expectedStatisticsResponse == 0) {
				showExecutionDetails();
			}*/
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}
	
 //-------------------------------------------------------------- Message handling: Dynamic - Take charge	
	/**
	 * Process the request from the goal agent to take charge of a node in charge by the receiver agent
	 * @param message
	 */
	private void processRequestTakeChargeNodeInPath(ACLMessage message) {
		TakeChargeNodeInPathRequest request = retrieveTakeChargeNodeInPathRequestFromMessage(message);
		Node nodeInPath = getNodeFromClosedList(request.getNodeURI());
		if (nodeInPath != null)
		{
			sendRequestedNodeInPathToGoalAgent(nodeInPath, request.getGoalAgent());
			sendRequestToParentAgentToRouteNodeInfoToGoalAgent(nodeInPath.getURI(), request.getGoalAgent());
			closedList.remove(nodeInPath);
		}
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
	private void sendRequestedNodeInPathToGoalAgent(Node node, String goalAgent) {
		TakeChargeNodeInPathResponse response = new TakeChargeNodeInPathResponse(node, getName());
		PlatformUtility.sendMessage(this, ACLMessage.INFORM, goalAgent, response,
				ConversationIDs.RESPONSE_TAKE_CHARGE_NODE_IN_PATH);
	}
	private void sendRequestToParentAgentToRouteNodeInfoToGoalAgent(String nodeURI, String goalAgent) {
		ForwardNodeUpdatesToGoalAgentRequest request = new ForwardNodeUpdatesToGoalAgentRequest(nodeURI, goalAgent);
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, parentAgentName, request, ConversationIDs.REQUEST_FORWARD_NODE_INFO_TO_GOAL_AGENT);
	}
	
	/**
	 * Process a response from an agent to a take charge request
	 * @param message
	 */
	private void processResponseTakeChargeNodeInPath(ACLMessage message) {
		TakeChargeNodeInPathResponse response = retrieveTakeChargeNodeInPathResponseFromMessage(message);
		Node importedNode = response.getNode();
		closedList.add(importedNode);
		importedNodes.add(new ImportedNode(importedNode.getURI(), response.getAgentInCharge()));
		verifyTakeChargeComplete();
	}
	
	private TakeChargeNodeInPathResponse retrieveTakeChargeNodeInPathResponseFromMessage(ACLMessage message) {
		TakeChargeNodeInPathResponse request = null;
		try {
			request = (TakeChargeNodeInPathResponse) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return request;
	}
	
	private void verifyTakeChargeComplete() {
		if (--takeChargeNodesPendingResponse == 0)
			println("Take charge of nodes in the path completed.");
	}

 // ------------------------------------------------------------- Message handling: Dynamic - ask current info
 	private void processRequestSuccessorNodeInfo(ACLMessage message) {
		RequestSuccessorNodeInfo request = retrieveRequestSuccessorNodeInfo(message);
	 	Node concernedNode = getNodeFromClosedList(request.getNodeURI());
	 	RemoteSuccessorInfo remoteSuccessorInfo = new RemoteSuccessorInfo(request.getNodeURI(),request.getParentNodeURI(),concernedNode.getG(),concernedNode.getH(),getName());
	 	PlatformUtility.sendMessage(this, ACLMessage.REQUEST, request.getGoalAgent(), remoteSuccessorInfo,ConversationIDs.INFORM_REMOTE_SUCCESSOR_INFO);
 	}

	private RequestSuccessorNodeInfo retrieveRequestSuccessorNodeInfo(ACLMessage message) {
		RequestSuccessorNodeInfo responseObject = null;
		try {
			responseObject = (RequestSuccessorNodeInfo) message.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		return responseObject;
	}


 //-------------------------------------------------------------- Message handling: Dynamic - Subscribe updates
	/**
	 * Ask resource agent to monitor the node
	 * Send the current info of the node to the goal agent
	 * @param message
	 */
	private void processRequestSubscribeSuccessorNodeUpdates(ACLMessage message)
	{
		try {
			SubscribeSuccessorNodeUpdatesRequest request = (SubscribeSuccessorNodeUpdatesRequest) message.getContentObject();
			goalAgentName = request.getGoalAgent();
			Node requestedNode = getNodeFromClosedList(request.getNodeURI());

			if (requestedNode != null) {
				sendSubscribeNodeUpdatesMessage(request.getNodeURI(), request.getParentNodeURI(),
						requestedNode.getConnectingResource(), getName());

				monitoredNodes.add(new MonitoredNode(request.getNodeURI(), request.getParentNodeURI(), requestedNode.getConnectingResource(), getName()));
			}
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		//TODO
		//Pour donner les infos des voisins des nodes du path goal
	}	
	
	/**
	 * Process the response from an agent responsible for a successor node (current info of the successor node)
	 * @param message
	 */
	//TODO
	//
	private void processRemoteSuccessorInfoMessage(ACLMessage message) {
		try {
			RemoteSuccessorInfo response = (RemoteSuccessorInfo) message.getContentObject();
			collectRemoteSuccessorsInfoPendingResponse++;


			//System.out.println("Current number Successor node responded : " + collectRemoteSuccessorsInfoPendingResponse);
			//TODO

			boolean addNodeInPathToOpenList = false;
			List<String> rootAffectedNodesList = new ArrayList<String>();
			for (int i = pendingListToCompareSuccessors.size() -1; i > -1; i--) {
				String[] compareSet = pendingListToCompareSuccessors.get(i);
				String root = compareSet[0];
				String childInPath = compareSet[1];
				String childSuccessor = compareSet[2];
				Node nodeChildInPath = GraphUtility.getNodeFromList(nodesInPath, childInPath);


				//System.out.println("Root : " + compareSet[0] +", child in path : "+compareSet[1]+ ", childSuccessor : "+ compareSet[2]);
				//System.out.println("responseParent : "+ response.getParentURI()+", responseChild : "+response.getNodeURI());
				if (response.getParentURI().equals(root) && response.getNodeURI().equals(childSuccessor))
				{
					if (nodeChildInPath.getG()+nodeChildInPath.getH() > response.getG()+response.getH()){
						/*
						Node backwardNode = GraphUtility.getNodeFromList(nodesInPath,goal.getURI());
						while (!backwardNode.getURI().equals(root))
						{
							closedList.remove(backwardNode);
							backwardNode = backwardNode.getParent();
						}
						openList.add(backwardNode);
						 */

						//closedList.clear();
						//arcList.clear();
						rootAffectedNodesList.add(response.getNodeURI());

						addNodeInPathToOpenList = true;
						sendMessageReexpandSuccesorNode(response.getNodeURI(), response.getParentURI(), response.getAgentInChargeOfSuccessor());

						/*
						println("Openlist");
						for (Node n : openList){
							println(n.getURI());
						}
						println("closed");
						for (Node n : closedList){
							println(n.getURI());
						}
						println("fin");
						 */
					}
					pendingListToCompareSuccessors.remove(i);
				}

				//Si il ya eu un alternative trouvé, on remet la node du path dans l'openList et on reset
				if (addNodeInPathToOpenList)
				{
					if(!GraphUtility.isNodeInList(openList, nodeChildInPath.getURI())){
						openList.offer(nodeChildInPath);

						//test
						removeClosedlistAffectedNodes(nodeChildInPath.getURI());
						//closedList.remove(nodeChildInPath);

					}
					rootAffectedNodesList.add(nodeChildInPath.getURI());
					resetGVP();
					inWaitingMode = false;
					dynamicCase = true;
					sendMessageResumeSearch(rootAffectedNodesList);
					reinitiateMetrics();
				}
			}



		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}

	private void sendMessageResumeSearch(List<String> rootAffectedNodesList)
	{
		PlatformUtility.sendMessage(this,ACLMessage.PROPAGATE, parentAgentName, new ResumeSearchRequest(getName(),getName(), rootAffectedNodesList), ConversationIDs.REQUEST_RESUME_SEARCH);
	}

	private void sendMessageReexpandSuccesorNode(String nodeURI, String nodeParentURI, String successorAgent)
	{
		List<String> ancestorsList = GraphUtility.getAncestorList(nodeURI);
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, parentAgentName, new ReexpandSuccessorNodeRequest(nodeURI,nodeParentURI, ancestorsList), ConversationIDs.REQUEST_REEXPAND_SUCCESSOR_NODE);
	}
	
 //-------------------------------------------------------------- Message handling: Dynamic - handling changes
	/**
	 * Process node changes sent by a resource agent
	 * @param message
	 */
	private void processNotificationNodeUpdates(ACLMessage message) {
		//TODO
		try {
			NodeUpdateNotification notification = (NodeUpdateNotification) message.getContentObject();

			// Locally modifying the node
			int newArcCost = notification.getNewArcCost();
			Node modifiedNode = getNodeFromClosedList(notification.getNodeURI());
			modifiedNode.setArcCost(newArcCost);

			//case node in Path
			if (GraphUtility.isNodeInList(nodesInPath, modifiedNode.getURI()))
			{
				Node nodeInPath = GraphUtility.getNodeFromList(nodesInPath, modifiedNode.getURI());
				nodeInPath.setArcCost(newArcCost);
			}

			RemoteSuccessorUpdate request = new RemoteSuccessorUpdate(modifiedNode.getURI(), modifiedNode.getParent().getURI(), modifiedNode.getH(), newArcCost, getName());
			PlatformUtility.sendMessage(this, ACLMessage.REQUEST, goalAgentName, request, ConversationIDs.INFORM_REMOTE_SUCCESSOR_UPDATES);

		} catch (UnreadableException e) {
			e.printStackTrace();
		}

		//TODO
		System.err.println("RECEIVING changes from RA!");
	}
	
	private void processRemoteSuccessorUpdatesMessage(ACLMessage message) {
		try {
			RemoteSuccessorUpdate response = (RemoteSuccessorUpdate) message.getContentObject();
			List<String> rootAffectedNodeList = new ArrayList<String>();

			// Cas 1, l'arc modifié est celui d'un noeud dans le parcours, dans ce cas, on demande des informations de tous les noeuds successeurs
			if(ListUtility.getURIListFromNodeList(nodesInPath).contains(response.getNodeURI()) && ListUtility.getURIListFromNodeList(nodesInPath).contains(response.getParentURI())) {
				Node concernedNode = getNodeFromClosedList(response.getNodeURI());
				concernedNode.setArcCost(response.getNewArcCost());

				Node rootNode = getNodeFromClosedList(response.getParentURI());
				List<Arc> arcList = GraphUtility.getArcs(rootNode);
				for (Arc arc : arcList)
				{
					if (!ListUtility.getURIListFromNodeList(nodesInPath).contains(arc.getchildIRI()))
					{
						RequestSuccessorNodeInfo request = new RequestSuccessorNodeInfo(arc.getchildIRI(), arc.getParentNode().getURI(), getName(), GraphUtility.getAncestorList(arc.getchildIRI()));

						PlatformUtility.sendMessage(this, ACLMessage.REQUEST, parentAgentName, request, ConversationIDs.REQUEST_SUCCESSOR_NODE_INFO);

						String[] compareSet = {response.getParentURI(),response.getNodeURI(),arc.getchildIRI()};
						pendingListToCompareSuccessors.add(compareSet);
					}
				}
			}

			//Cas 2, l'arc modifié est celui d'un noeud successeur, on a alors juste à le comparer à celui appartenant au chemin trouvé
			else if(ListUtility.getURIListFromNodeList(nodesInPath).contains(response.getParentURI())){
				int newArc = response.getNewArcCost();
				Node rootNode = getNodeFromClosedList(response.getParentURI());
				Node nodeInPathToCompare = getNextNodeInPath(rootNode);
				if (rootNode.getG()+newArc+response.getH() <= nodeInPathToCompare.getH()+nodeInPathToCompare.getG())
				{

					if(!GraphUtility.isNodeInList(openList, nodeInPathToCompare.getURI())){
						openList.offer(nodeInPathToCompare);

						//test
						removeClosedlistAffectedNodes(nodeInPathToCompare.getURI());
						//closedList.remove(nodeChildInPath);

					}
					resetGVP();
					inWaitingMode = false;
					dynamicCase = true;

					rootAffectedNodeList.add(response.getNodeURI());
					rootAffectedNodeList.add(nodeInPathToCompare.getURI());
					sendMessageResumeSearch(rootAffectedNodeList);
					sendMessageReexpandSuccesorNode(response.getNodeURI(),response.getParentURI(),response.getAgentInChargeOfSuccessor());
					reinitiateMetrics();
				}
			}


		} catch (UnreadableException e) {
			e.printStackTrace();
		}
		//TODO
	}

	private void processResumeSearch(ACLMessage message) {
		try {
			//Lire le message n'est pas utile pour le search agent (seulement le network agent), car dès qu'on le recoit, on change du waiting mode
			ResumeSearchRequest request = (ResumeSearchRequest) message.getContentObject();
			List<String> rootAffectedNodesList = request.getRootAffectedNodeList();
			removeClosedlistAffectedNodesList(rootAffectedNodesList);
			resetGVP();
			dynamicCase = true;
			inWaitingMode = false;
			reinitiateMetrics();
		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}

	private void processReexpandSuccessorNodeMessage(ACLMessage message) {
		try {
			ReexpandSuccessorNodeRequest request = (ReexpandSuccessorNodeRequest) message.getContentObject();
			String nodeUri = request.getNodeURI();
			Node nodeToExpand = getNodeFromClosedList(nodeUri);
			if (nodeToExpand != null) {
				openList.offer(nodeToExpand);

				//test
				removeClosedlistAffectedNodes(nodeToExpand.getURI());
				//closedList.remove(nodeToExpand);
			}

			/*
			println("We added : "+ nodeToExpand.getURI());
			println("Openlist");
			for (Node n : openList){
				println(n.getURI());
			}
			println("ClosedList");
			for (Node n : closedList){
				println(n.getURI());
			}*/


		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}
	
	
//-------------------------------------------------------------- Expand block
	private void expand() {
		if (openList.isEmpty()) 
			return;		
		Node expandedNode = openList.poll();
		println("Expand: " + expandedNode.getURI() + " --- " + expandedNode.getG());
		
		if (goal.isGoal(expandedNode.getURI())) { // GVP - goal detection
			System.err.println("--------------__GVP__");
			initiateGVP(expandedNode);
			return;
		}
		expandedNode = filterNodesBeforeExpanding(expandedNode); // Filter unpromising nodes
		if (expandedNode != null) { // Expand
			closedList.add(expandedNode);
			routeArcsToAgentInCharge(expandedNode);// Find the agent responsible
			expands++; // Experiments
		}
	}
	
	private void initiateGVP(Node expandedNode) {
		if (currentGoal == null || currentGoal.getGoalNode().getG() > expandedNode.getG()) {
			currentGoal = new GoalUnderVerification(expandedNode, getName(), getName());
			goalVerifiedLocally = false;
			isGoalAgent = true;
			numVerificationRequired = propagateGVPMessage(expandedNode);
		}
		closedList.add(expandedNode);
	}
	
	private int propagateGVPMessage(Node goalNode) {
		PlatformUtility.sendMessage(this, ACLMessage.PROPAGATE, parentAgentName,
				new GVPRequest(goalNode, getName(), getName()), ConversationIDs.PROPAGATE_GVP_REQUEST);
		return 1;
	}
	
	private Node filterNodesBeforeExpanding(Node expandedNode) {
		if (currentGoal != null) {
			while (expandedNode != null && currentGoal.getGoalNode().getCost() <= expandedNode.getCost()) {
				expandedNode = openList.poll();
			}
		}
		return expandedNode;
	}

	private void routeArcsToAgentInCharge(Node expandedNode) {
		List<Arc> arcs = GraphUtility.getArcs(expandedNode); 
		if (arcs != null)
			for (Arc arc : arcs) {
				findAgentResponsible(arc);
			}
		else
			println("Unaccessible  - " + expandedNode.getURI());
	}

	
//-------------------------------------------------------------- Routing block		
	private void findAgentResponsible(Arc arc) {
		String nodeIRI = arc.getchildIRI();
		if (isAgentResponsible(nodeIRI)) {
			handleArcToExistingResponsibledNode(arc); // Handle locally
		} else {
			askParentAgentToFindAgentResponsible(arc); // Route to agent responsible
		}
	}

	private boolean isAgentResponsible(String nodeIRI) {
		for (String responsibledNode : responsibledNodes) {
			if (nodeIRI.equals(responsibledNode))
				return true;
		}
		return false;
	}

	private void handleArcToExistingResponsibledNode(Arc arc) {
		String nodeIRI = arc.getchildIRI();
		int arcParentCost = arc.getParentNode().getG();
		// the existing node - in OL
		for (Node node : openList) {
			if (node.getURI().equals(nodeIRI)) {
				if (arcParentCost < node.getG())
					addArcToArcList(arc);
				return;
			}
		}
		// the existing node - in CL
		for (Node node : closedList) {
			if (node.getURI().equals(nodeIRI)) {
				if (arcParentCost < node.getG())
					addArcToArcList(arc);
				return;
			}
		}//
		//the existing node - in AL/PL - verify later
		arcPendingVerification.add(arc);
	}
	
	private void addArcToArcList(Arc arc) {
		arcList.offer(arc);
	}

	private void askParentAgentToFindAgentResponsible(Arc arc) {
		List<String> ancestorList = GraphUtility.getAncestorList(arc.getchildIRI());
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, parentAgentName,
				new FindAgentResponsibleRequest(arc, getName(), ancestorList), ConversationIDs.FIND_AGENT_RESPONSIBLE);
	}
	
	
//-------------------------------------------------------------- Compute arc cost block
	private void computeArcCost() {
		Arc arc = arcList.poll();
		temporaryFixArcVerification(arc);
		arc = filterArcBeforeComputingArcCost(arc);
		if(arc != null) {
			addArcToPendingListWaitingCostResponse(arc);
			sendArcCostRequestToResourceAgent(arc);
			requests++; // Experiments
		}
	}
	
	// Temporary workaround - why sometimes pending list empty but arcPendingVerification is not - > because Expand filter GVP(n) 
	private void temporaryFixArcVerification(Arc arc) {
		List<Arc> iteratedActions;
		if (arc == null && pendingList.isEmpty()) {
			iteratedActions = new ArrayList<>();
			for (Arc apv : arcPendingVerification) {
				addArcToArcList(apv);
				iteratedActions.add(apv);
			}//
			arcPendingVerification.removeAll(iteratedActions);
		}
	}
	
	private Arc filterArcBeforeComputingArcCost(Arc arc) {
		if (currentGoal != null) {
			while (arc != null && arc.getParentNode().getG() >= currentGoal.getGoalNode().getG()) {
				arc = arcList.poll();
			}
		}
		return arc;
	}
	
	private void addArcToPendingListWaitingCostResponse(Arc arc) {
		arc.setArcID(arcID++); // set arc ID for identifying the arc cost response from RA
		arc.setToPendingMode(); // to keep track of time limit
		pendingList.put(arc.getArcID(), arc);
	}
	
	private void sendArcCostRequestToResourceAgent(Arc arc) {
		ArcCostRequest request = new ArcCostRequest(arc.getArcID(), arc.getParentNode().getURI(), arc.getchildIRI(), arc.getConnectingResourceURI());
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, resourceAgentNames.get(0), request, ConversationIDs.REQUEST_ARC_COST);
	}
	
	
//-------------------------------------------------------------- Goal verification block
	private void verifyUnverifiedGoalLocally() {
		if (!goalVerifiedLocally) {
			goalVerifiedLocally = verifyGoalLocally(currentGoal.getGoalNode());
			if (!test3 && goalVerifiedLocally) {
				//println("test3");
				//println(goalVerifiedLocally);
				test3 = true;
			}
		}

		if (!isGoalAgent && goalVerifiedLocally) { // send response to the goal agent
			String senderAgent = currentGoal.getSenderAgent();
			sendGVPResponseToGoalAgent(senderAgent);
			if (!test1) {
				//println("Je suis pas le goal agent");
				test1 = true;
			}
		}else if(isGoalAgent && (allAgentVerifiedGoal(currentGoal) || (goalVerifiedLocally && dynamicCase))) { // goal verified by all agents
			constructSolution();
			if (!test2) {
				//println("Je construit la solution");
				test2 = true;
			}
		}
	}
	
	private boolean verifyGoalLocally(Node goalNode) {
		for (Node node : openList) {
			if (goalNode.getCost() > node.getCost())
				//println("Ici alors1");
				return false;
		}
		if (arcList.size() > 0) {
			//println("Ici alors2");
			return false;
		}
		if (arcPendingVerification.size() > 0){
			//println("Ici alors3");
			return false;
		}
		for (Map.Entry<Integer, Arc> entry : pendingList.entrySet()) {
			Arc arc = entry.getValue();
			if ((System.currentTimeMillis() - arc.getRequestTime()) < PENDING_TIME_LIMIT) {
				//println("Ici alors4");
				return false;
			}/*else {Subscribe remote successors completed
				println("---------> Request too old. Removed. <----------");
				// pendingList.removeAll(expiredActions);
				// expiredRequest += expiredActions.size();
			}*/
		}
		//println("Ici finalement");
		return true;
	}
	
	private void sendGVPResponseToGoalAgent(String senderAgent) {
		//println("GVP response sent");
		PlatformUtility.sendMessage(this, ACLMessage.INFORM, senderAgent,
				new GVPResponse(currentGoal.getGoalNode(), true), ConversationIDs.PROPAGATE_GVP_RESPONSE);
	}
	
	private boolean allAgentVerifiedGoal(GoalUnderVerification goal) {
		return goal.getNumberOfVerificationResponse() >= numVerificationRequired;
	}
	
	private void constructSolution() {
		endTime = System.currentTimeMillis(); // Experiments
		solution = new Solution(currentGoal.getGoalNode(), currentGoal.getDiscoveredTime());
		resetGVP();
		collectSearchStatusInfo(); // Experiments

		println("Total latency (all agents) : " + LatencyHolder.getTotalLatency());
		LatencyHolder.printLatencyName();
	}
	
	private void resetGVP() {
		inWaitingMode = true; // pause local search process
		currentGoal = null;
		goalVerifiedLocally = false;
	}
	
	private void collectSearchStatusInfo() {
		// Experiments
		totalExpands += expands;
		totalRequests += requests;
		
		expectedStatisticsResponse =  PlatformUtility.getNumSearchAgentsFromAMS(this) - 1; // Get number of search agents in the platform
		if (expectedStatisticsResponse == 0) { // If there is only 1 search agent, then no need to wait for others
			showExecutionDetails(); // Experiments
		} else {
			PlatformUtility.sendMessage(this, ACLMessage.PROPAGATE, parentAgentName,
					new SearchStatusInfoRequest(getName(), getName()),
					ConversationIDs.PROPAGATE_SEARCH_STATUS_INFO_REQUEST); // request search info from other search agents
		}
	}
	
	private void showExecutionDetails() {
		long startTime = Main.startTime;
		double execTime = (endTime - startTime) / 1000d;
		double discoveryTime = (solution.getDiscoveredTime() - startTime) / 1000d;
		long pathCost = solution.getGoalNode().getG();
		println(getName() + " [Stats] - Cost: (" + pathCost + "), Time: (" + execTime
				+ "), Discovery time: (" + discoveryTime
				+ "), Expands: (" + totalExpands
				+ "), Requests: (" + totalRequests
				+ ")");
		println(solution);
		
		// Trigger dynamic handling
		initiateDynamicHandling();
	}
	

//-------------------------------------------------------------- Dynamic block: Setup
	private void initiateDynamicHandling() {
		nodesInPath = solution.getPathNodes();
		setupForDynamicsHandling();
	}
	
	private void setupForDynamicsHandling() {
		println("------------------------------- Setting up for dynamic -----------------------------------");
		// Take charge of nodes in the path
		takeChargeNodesInPath();
		// Subscribe for changes inSubscribe remote successors completed the path
		subscribeForUpdatesNodesInPath();
		// Subscribe for changes in the successor nodes
		subscribeForUpdatesSuccessorNodes();
	}
		
	private void takeChargeNodesInPath() {
		for (Node nodeInPath : nodesInPath) {
			if (!isLocalNode(nodeInPath.getURI())) {
				sendTakeChargeNodeInPathRequest(nodeInPath.getURI());
				takeChargeNodesPendingResponse++;
			}
		}
		System.err.println("Pending take charge node in path: " + takeChargeNodesPendingResponse);
	}

	private boolean isLocalNode(String node) {
		return responsibledNodes.contains(node);
	}

	private void sendTakeChargeNodeInPathRequest(String node) {
		List<String> ancestorList = GraphUtility.getAncestorList(node);
		TakeChargeNodeInPathRequest request = new TakeChargeNodeInPathRequest(node, getName(), ancestorList);
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, parentAgentName, request, ConversationIDs.REQUEST_TAKE_CHARGE_NODE_IN_PATH);
	}
	
	private void subscribeForUpdatesNodesInPath() {
		for (int i = 1; i < nodesInPath.size(); i++) {
			Node nodeInPath = nodesInPath.get(i);
			sendSubscribeNodeUpdatesMessage(nodeInPath.getURI(), nodeInPath.getParent().getURI(),
					nodeInPath.getConnectingResource(), getName());
		}
		println("Subscribe for updates of the nodes in the path completed.");
	}

	private void subscribeForUpdatesSuccessorNodes() {
		System.out.println(arcPendingVerification.size());
		for (int i = 0; i < nodesInPath.size()-1; i++) {
			Node nodeInPath = nodesInPath.get(i);
			List<Arc> arcList = GraphUtility.getArcs(nodeInPath);
			for (Arc arc : arcList)
			{
				if (!ListUtility.getURIListFromNodeList(nodesInPath).contains(arc.getchildIRI()) && mapNodeAndAgentInResponsible.get(arc.getchildIRI())!=null)
				{
					SubscribeSuccessorNodeUpdatesRequest request = new SubscribeSuccessorNodeUpdatesRequest(arc.getchildIRI(), nodeInPath.getURI(), getName(), GraphUtility.getAncestorList(arc.getchildIRI()));
					PlatformUtility.sendMessage(this, ACLMessage.REQUEST, mapNodeAndAgentInResponsible.get(arc.getchildIRI()), request, ConversationIDs.REQUEST_SUBSCRIBE_SUCCESSOR_NODE_UPDATES);
					//System.out.println(arc);
				}
			}
			//System.out.println("Break : " + i);
		}
		println("Subscribe for updates of the nodes in other successors of nodes in the path");
		//TODO
	}

	private void sendSubscribeNodeUpdatesMessage(String nodeURI, String parentNodeURI, String connectingResource, String agentRequester) {
		SubscribeForNodeUpdateRequest request = new SubscribeForNodeUpdateRequest(nodeURI,
				parentNodeURI, connectingResource, agentRequester);
		PlatformUtility.sendMessage(this, ACLMessage.REQUEST, resourceAgentNames.get(0), request, ConversationIDs.REQUEST_SUBSCRIBE_NODE_UPDATES);
	}


		
	private Node getNodeFromClosedList(String nodeURI) {
		for (Node node : closedList) {
			if (node.getURI().equals(nodeURI))
				return node;
		}
		return null;
	}

	private Node getNextNodeInPath(Node node)
	{
		for (Node node_i : nodesInPath)
		{
			if(node_i.getParent()!= null && node_i.getParent().getURI().equals(node.getURI()))
			{
				//println("returned Node : "+node_i);
				return node_i;
			}
		}
		return null;
	}

	private void removeClosedlistAffectedNodesList(List<String> rootAffectedNodesList)
	{
		for (String concernedRootNode : rootAffectedNodesList)
		{
			removeClosedlistAffectedNodes(concernedRootNode);
		}
	}

	private void removeClosedlistAffectedNodes(String concernedRootNodeURI){
		List<Node> concernedNodesFromRoot = new ArrayList<Node>();
		List<Node> subPath = new ArrayList<Node>();
		for (Node node : closedList)
		{
			subPath.clear();
			do {
				if (node.getURI().equals(concernedRootNodeURI)) {
					concernedNodesFromRoot.addAll(subPath);
					break;
				}
				subPath.add(node);
				node = node.getParent();
			} while (node != null);
		}
		closedList.removeAll(concernedNodesFromRoot);
	}

	private void reinitiateMetrics()
	{
		expands = 0;
		requests = 0;
		totalExpands = 0;
		totalRequests = 0;
		LatencyHolder.resetLatency();
	}
//-------------------------------------------------------------- Dynamic block

}

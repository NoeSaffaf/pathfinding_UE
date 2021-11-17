package csm.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import csm.Evaluation.LatencyHolder;
import csm.SimulatedValue.SimulatedValues;
import csm.SimulatedValue.SimulatedValuesHolder;
import csm.communication.ConversationIDs;
import csm.communication.arccost.ArcCostRequest;
import csm.communication.arccost.ArcCostResponse;
import csm.communication.dynamics.NodeUpdateNotification;
import csm.communication.dynamics.SubscribeForNodeUpdateRequest;
import csm.model.dynamics.MonitoredNode;
import csm.utility.PlatformUtility;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

public class ResourceAgent extends CSMAgent {
	private static final long serialVersionUID = 1L;

	// Latency
	private int resourceAccessTime[];
	private int indexResourceAccessTime = 0;

	// Cost - currently simulated cost fixed to 1 (for uniform-cost)
	private int cost = 1;

	// For dynamic hanlding
	private List<MonitoredNode> monitoredNodes;
	private Thread changeDetector;

	// Stop simulatedchangedValue
	private boolean stopWaiting = false;

//------------------------------------------------------------ Setup block
	protected void setup() {
		// Get latency values
		Object[] arguments = getArguments();
		resourceAccessTime = (int[]) arguments[0];

		//subscribre Latency
		LatencyHolder.subscribeToLatencyList(getName());

		// Dynamic
		monitoredNodes = new ArrayList<>();
		activiteDynamics();

		// Behaviours
		addBehaviour(new ProcessMessageBehaviour());
		addBehaviour(new ChangeArbritaryValueBehavior());
	}

// BehaviorResponsible for reading values change
	class ChangeArbritaryValueBehavior extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			if (!stopWaiting) {
				if(!SimulatedValuesHolder.listSimulatedvalues.isEmpty()){
					stopWaiting = true;
					for(SimulatedValues sv : SimulatedValuesHolder.listSimulatedvalues){
						notifyChangeToSearchAgent(sv.getParentNodeUri(), sv.getNodeUri(), sv.getNewArc());
					}
				}
			}
		}
	}


// ------------------------------------------------------------ Behaviour block
	// Behaviour responsible for handling received messages
	class ProcessMessageBehaviour extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			while (myAgent.getCurQueueSize() > 0) { // process all messages at once
				ACLMessage message = myAgent.receive();
				if (message != null) {
					if (message.getConversationId().equals(ConversationIDs.REQUEST_ARC_COST)) {
						processArcCostRequestMessage(message);
					} else if (message.getConversationId().equals(ConversationIDs.REQUEST_SUBSCRIBE_NODE_UPDATES)) {
						processSubscribeNodeUpdatesRequest(message);
					}
				}
			}
		}
	}

	// Behaviour for simulating latency -
	class SimulatedResourceAccessBehaviour extends WakerBehaviour {
		private static final long serialVersionUID = 1L;

		private ACLMessage message;
		private int cost;
		private Long latency;

		public SimulatedResourceAccessBehaviour(ACLMessage message, int cost, Agent agent, long latency) {
			super(agent, latency);
			this.message = message;
			this.cost = cost;
			this.latency = latency;
		}

		@Override
		public void onWake() {
			ArcCostRequest requestObject;
			try {
				requestObject = (ArcCostRequest) message.getContentObject();
				ArcCostResponse replyObject = new ArcCostResponse(requestObject.getArcID(), cost);
				PlatformUtility.sendMessage(myAgent, ACLMessage.INFORM, message.getSender().getName(), replyObject,
						ConversationIDs.REQUEST_ARC_COST);
				LatencyHolder.addLatency(latency);
				LatencyHolder.addLatencyName(latency,getName());
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
	}

//------------------------------------------------------------ Arc cost block		
	/**
	 * Simulate latency by using the WakeBehaviour
	 * @param message
	 */
	private void processArcCostRequestMessage(ACLMessage message) {
		indexResourceAccessTime++;
		if (indexResourceAccessTime >= resourceAccessTime.length) {
			indexResourceAccessTime = 0;
		}
		addBehaviour(
				new SimulatedResourceAccessBehaviour(message, cost, this, resourceAccessTime[indexResourceAccessTime]));
	}

//------------------------------------------------------------ Dynamics block	
	/**
	 * Handle the request from a search agent to minitor future changes to a node
	 * @param message
	 */
	private void processSubscribeNodeUpdatesRequest(ACLMessage message) {
		try {
			SubscribeForNodeUpdateRequest request = (SubscribeForNodeUpdateRequest) message.getContentObject();
			monitoredNodes.add(new MonitoredNode(request.getNodeURI(), request.getParentURI(),
					request.getConnectingResource(), message.getSender().getName()));
			//println("Monitored Node: "+request.getParentURI() +" : "+request.getNodeURI());

			//For chosing an arc to simulate a modification
			SimulatedValuesHolder.addMonitoredArc(request.getParentURI(),request.getNodeURI());

		} catch (UnreadableException e) {
			e.printStackTrace();
		}
	}

	// Call this method to allow manually adding dynamic changes
	private void activiteDynamics() {
		detectChanges();
	}

	// Simulating dynmaic changes by entering the cost change via the console
	private void detectChanges() {
		changeDetector = new Thread(new Runnable() {
			@Override
			public void run() {
				String parentNodeUri;
				String childNodeUri;
				int newArcCost;
				Scanner scanner = new Scanner(System.in);

				int maxchange = 1;
				int numberchange = 0;
				do {
					// Read the change
					print("Parent node URI: ");
					scanner.next();
					parentNodeUri = scanner.next();
					println("you said  : " + parentNodeUri);
					//parentNodeUri = "http://127.0.0.1/Graph/location/location_3";

					if (parentNodeUri.equals(".")) {
						println("hey");
						showMonitoredNodes();
						continue;
					}

					print("Child node URI: ");
					//childNodeUri = scanner.next();
					childNodeUri = "http://127.0.0.1/Graph/location/location_5";

					print("New arc cost: ");
					//newArcCost = Integer.parseInt(scanner.next());
					newArcCost = 100;

					// Notify the agent
					notifyChangeToSearchAgent(parentNodeUri, childNodeUri, newArcCost);

					numberchange++;

				} while (!parentNodeUri.equals("stop") && numberchange<maxchange);

				scanner.close();
			}
		});
		changeDetector.start();
	}

	// Notify the search agent responsible for the node of the change
	private void notifyChangeToSearchAgent(String parentNodeURI, String childNodeURI, int newArcCost) {
		MonitoredNode monitoredNode = null;
		//println("hello : " + monitoredNodes.size());
		println(parentNodeURI + "   " + childNodeURI + "   " +newArcCost);
		for (MonitoredNode node : monitoredNodes) {
			// System.out.println("Parent : "+node.getParentNodeURI()+", Child : "+node.getNodeURI());

			if (node.getParentNodeURI().equals(parentNodeURI) && node.getNodeURI().equals(childNodeURI)) {
				monitoredNode = node;
				break;
			} else {
				//System.out.println("ca correspond pas");
			}
		}

		if (monitoredNode == null) {
			println("Node non monitoré");
			return;
		}
		NodeUpdateNotification notification = new NodeUpdateNotification(monitoredNode.getNodeURI(),
				monitoredNode.getParentNodeURI(), monitoredNode.getConnectingResource(), newArcCost);
		PlatformUtility.sendMessage(this, ACLMessage.INFORM, monitoredNode.getRequesterAgent(), notification,
				ConversationIDs.NOTIFICATION_NODE_UPDATES);

	}

	// Show all the nodes under the monitoring of this resource agent
	private void showMonitoredNodes() {
		for (MonitoredNode node : monitoredNodes) {
			System.out.println(node);
		}
	}
}
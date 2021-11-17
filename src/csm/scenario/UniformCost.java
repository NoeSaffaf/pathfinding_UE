package csm.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import csm.model.search.Arc;
import csm.model.search.Node;
import csm.model.solution.Solution;
import csm.utility.GraphUtility;

// Implementation of the Uniform-cost algorithm for comparison
public class UniformCost {
	private int expands;
	private long requests;
	private long startTime;
	private long totalLatency;
	
	private  Queue<Node> openList;
	private ArrayList<Node> closedList;
	private Node startNode;
	private String goalURI;

	protected int RESOURCE_ACCESS_TIME[];
	protected int resourceAccessTimeIndex = 0;

	public UniformCost(String startURI, String goalURI, int RESOURCE_ACCESS_TIME[]) {
		totalLatency = 0;
		openList = new PriorityQueue<>();
		closedList = new ArrayList<>();
		startNode = new Node(startURI, null, null, 0, 0, 0);
		
		this.goalURI = goalURI;
		
		this.RESOURCE_ACCESS_TIME = RESOURCE_ACCESS_TIME;
		
		openList.offer(startNode);
		
	}
	
	public void search() {
		startTime = System.currentTimeMillis();
		
		while (true) {
			if (openList.size() == 0)
				return;

			Node current = openList.poll();
			expands++;
			if (current.getURI().equals(goalURI)){
				exit(current, startTime);
				break;
			}
				

			closedList.add(current);
			List<Arc> arcs = GraphUtility.getArcs(current);
			if (arcs != null) {
				for (Arc arc : arcs) {
					
					int arcCost = getActionCost(arc);
					long latency = getLatency(arc);
					totalLatency += latency;


					requests++;	// stats
					
					int h = 0; // heuristic function = 0 for uniform-cost

					//an if just to change the cost of one specific arc

					/*
					Node child;
					if (arc.getParentNode().getURI().equals("http://127.0.0.1/Graph/location/location_3") && arc.getchildIRI().equals("http://127.0.0.1/Graph/location/location_5"))
					{
						child = new Node(arc.getchildIRI(), arc.getParentNode(), arc.getConnectingResourceURI(),
								arc.getParentNode().getG(), 10, h);
					} else if (arc.getParentNode().getURI().equals("http://127.0.0.1/Graph/location/location_1") && arc.getchildIRI().equals("http://127.0.0.1/Graph/location/location_2")) {
						child = new Node(arc.getchildIRI(), arc.getParentNode(), arc.getConnectingResourceURI(),
									arc.getParentNode().getG(), 10, h);
					} else {
						child = new Node(arc.getchildIRI(), arc.getParentNode(), arc.getConnectingResourceURI(),
								arc.getParentNode().getG(), arcCost, h);
					}*/

					Node child = new Node(arc.getchildIRI(), arc.getParentNode(), arc.getConnectingResourceURI(),
							arc.getParentNode().getG(), arcCost, h);

					Node inOL = child.existsIn(openList);
					Node inCL = child.existsIn(closedList);
					if (inOL == null && inCL == null) {
						openList.offer(child);
					} else if (inOL != null && inOL.getG() > child.getG()) {
						openList.remove(inOL);
						openList.offer(child);
					}
				}
			}
		}
	}

	//Same as get action, but we dont use threads to create real time latency, just consider latency as a value
	private long getLatency(Arc arc) {
		resourceAccessTimeIndex++;
		if (resourceAccessTimeIndex >= RESOURCE_ACCESS_TIME.length) {
			resourceAccessTimeIndex = 0;
		}
		return RESOURCE_ACCESS_TIME[resourceAccessTimeIndex];
	}

	protected int getActionCost(Arc action) {
		resourceAccessTimeIndex++;
		if (resourceAccessTimeIndex >= RESOURCE_ACCESS_TIME.length) {
			resourceAccessTimeIndex = 0;
		}
		int accessTime = RESOURCE_ACCESS_TIME[resourceAccessTimeIndex];
		try {
			Thread.sleep(accessTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return 1;
	}
	
	protected void exit(Node goal, long startTime) {
		long endTime = System.currentTimeMillis();
		Solution solution = new Solution(goal, endTime);
		double execTime = (endTime - startTime) / 1000d;
		
		System.out.println("[Stats] - Cost: (" + goal.getCost() + "), Time: (" + execTime
				+ "), Discovery time: (" + execTime
				+ "), Expands: (" + expands
				+ "), Requests: (" + requests
				+ "), Total Latency: (" + totalLatency
				+ ")");
		
		System.out.println(solution);

	}
	
	public static void main(String[] args) {
		String ORIGIN_LOCATION = "http://127.0.0.1/Graph/location/location_10";
		String DESTINATION_LOCATION = "http://127.0.0.1/Graph/location/location_25";
		
		int latency = 3;
		int RESOURCE_ACCESS_TIME[];
		switch (latency) {
		case 1:
			RESOURCE_ACCESS_TIME = ExperimentValues.RESOURCE_ACCESS_TIME_1;
			break;
		case 3:
			RESOURCE_ACCESS_TIME = ExperimentValues.RESOURCE_ACCESS_TIME_3;
			break;
		case 5:
			RESOURCE_ACCESS_TIME = ExperimentValues.RESOURCE_ACCESS_TIME_5;
			break;
		case 7:
			RESOURCE_ACCESS_TIME = ExperimentValues.RESOURCE_ACCESS_TIME_7;
			break;
		case 9:
			RESOURCE_ACCESS_TIME = ExperimentValues.RESOURCE_ACCESS_TIME_9;
			break;
		default:
			RESOURCE_ACCESS_TIME = ExperimentValues.RESOURCE_ACCESS_TIME_0;
			break;
		}
		
		UniformCost uni = new UniformCost(ORIGIN_LOCATION, DESTINATION_LOCATION, RESOURCE_ACCESS_TIME);
		uni.search();
	}
}

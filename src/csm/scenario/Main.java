package csm.scenario;

import java.util.List;

import csm.CSM;
import csm.SimulatedValue.SimulatedValuesThread;
import csm.model.search.PathFindingProblem;
import csm.utility.PlatformUtility;
import jade.wrapper.ContainerController;

public class Main {
	public static long startTime; // stats

	public static void main(String[] args) {
		// Main container
		ContainerController mainContainer = PlatformUtility.launchMainContainer(ExperimentValues.HOST,
				ExperimentValues.PORT, ExperimentValues.PLATFORM_ID, ExperimentValues.GUI);
		// Other containers
		List<String> containers = null; // provide other containers for a distributed versionl; null for centralised

		// Get start and goal URI
		String start = "http://127.0.0.1/Graph/location/location_10";
		String goal = "http://127.0.0.1/Graph/location/location_63";
		PathFindingProblem problem = new PathFindingProblem(start, goal); // create a pathfinding problem instance

		// Get latency values
		int resourceAccessTime[] = ExperimentValues.getResourceAccessTimeValues(9); // in millsecs

		// Bootstraping
		CSM csm = new CSM(problem, resourceAccessTime, ExperimentValues.NA_MAX_OE,
				ExperimentValues.SA_MAX_RESPONSIBLE_NODES, mainContainer, containers);
		csm.bootStrap(); // start the multi-agent search

		startTime = System.currentTimeMillis(); // record the start time

		SimulatedValuesThread simulatedValuesThread = new SimulatedValuesThread();
		simulatedValuesThread.start();
	}
}

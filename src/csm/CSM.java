package csm;

import java.util.ArrayList;
import java.util.List;

import csm.model.search.PathFindingProblem;
import csm.utility.GraphUtility;
import csm.utility.PlatformUtility;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class CSM {
	private PathFindingProblem problem; // problem to solve
	private int[] resourceAccessTime; // simulated latency
	private int NA_MAX_OE, SA_MAX_RESPONSIBLE_NODES;
	private ContainerController container;
	private List<String> containers;

	public CSM(PathFindingProblem problem, int[] resourceAccessTime, int NA_MAX_OE, int SA_MAX_RESPONSIBLE_NODES,
			ContainerController container, List<String> containers) {
		this.resourceAccessTime = resourceAccessTime;
		this.NA_MAX_OE = NA_MAX_OE;
		this.SA_MAX_RESPONSIBLE_NODES = SA_MAX_RESPONSIBLE_NODES;
		this.problem = problem;
		this.container = container;
		this.containers = containers;
	}

	// Lunch the search process
	public void bootStrap() {
		try {
			// Create and start a resource agent of type API resources
			AgentController resourceAgentAPI = PlatformUtility.createAndStartResourceAgent(container, "Ra_API",
					resourceAccessTime, containers);

			// resourceAgentNames passed as knowledge to other agents
			List<String> resourceAgentNames = new ArrayList<>();
			resourceAgentNames.add(resourceAgentAPI.getName()); // resource agents can be created more if needed

			// form name of the intial network agent using OE of the start location
			String organisationalEntityIRI = GraphUtility.getParentEntityFromURI(problem.getOriginURI());
			String networkAgentLocalName = "Na_"
					+ GraphUtility.getOrganisationalEntitysNameFromIRI(organisationalEntityIRI);

			// Create the intial search agent
			AgentController searchAgent0 = PlatformUtility.createAndStartSearchAgent(container, "Sa_0", problem,
					resourceAgentNames, networkAgentLocalName + "@" + container.getPlatformName(), null, null,
					containers);

			// Create the initial network agent
			List<String> childSearchAgentResponsibledNodes = new ArrayList<>();
			childSearchAgentResponsibledNodes.add(problem.getOriginURI());
			AgentController networkAgent0 = PlatformUtility.createAndStartNetworkAgent(container, networkAgentLocalName,
					organisationalEntityIRI, null, searchAgent0.getName(), childSearchAgentResponsibledNodes, null,
					new ArrayList<>(), problem.getGoal(), resourceAgentNames, containers, NA_MAX_OE,
					SA_MAX_RESPONSIBLE_NODES);

			// Start all agent
			searchAgent0.start();
			resourceAgentAPI.start();
			networkAgent0.start();

		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

}

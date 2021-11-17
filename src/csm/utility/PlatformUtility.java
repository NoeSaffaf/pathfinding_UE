package csm.utility;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import csm.model.search.Arc;
import csm.model.search.Goal;
import csm.model.search.PathFindingProblem;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.domain.AMSService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class PlatformUtility {
	
	public static ContainerController launchMainContainer(String host, String port, String platformID, String gui) {
		Runtime runtime = Runtime.instance(); // Retrieve the singleton instance of the JADE Runtime

		// Lauch the container
		Profile p = new ProfileImpl();
		p.setParameter(Profile.MAIN_HOST, host);
		p.setParameter(Profile.MAIN_PORT, port);
		p.setParameter(Profile.PLATFORM_ID, platformID);
		p.setParameter(Profile.GUI, gui);
		ContainerController containerController = runtime.createMainContainer(p);
		
		return containerController;
	}

	public static AgentController createAndStartResourceAgent(ContainerController containerController, String agentLocalName, int [] RESOURCE_ACCESS_TIME, List<String> containers) {
		return createAndStartAgent(containerController, agentLocalName, "csm.agent.ResourceAgent", new Object[] { RESOURCE_ACCESS_TIME, containers });
	}
	
	public static AgentController createAndStartSearchAgent(ContainerController containerController, String agentLocalName, PathFindingProblem problem, List<String> resourceAgentNames, String parentNetworkAgentName, Arc arc, Goal goal, List<String> containers) {
		return createAndStartAgent(containerController, agentLocalName, "csm.agent.SearchAgent", new Object[] { problem, resourceAgentNames,
				parentNetworkAgentName, arc, goal, containers });
	}
	
	public static AgentController createAndStartNetworkAgent(ContainerController containerController,
			String agentLocalName, String organisationalEntityIRI, String parentAgentName, String childSearchAgentName,
			List<String> childSearchAgentResponsibledNodes, String childNetworkAgentName,
			List<String> childNetworkAgentResponsibledEntities, Goal goal, List<String> resourceAgentName, List<String> containers, int NA_MAX_OE, int SA_MAX_RESPONSIBLE_NODES) {
	
		Object[] arguments = new Object[] { organisationalEntityIRI, parentAgentName, childSearchAgentName,
				childSearchAgentResponsibledNodes, childNetworkAgentName, childNetworkAgentResponsibledEntities, goal,
				resourceAgentName, containers, NA_MAX_OE, SA_MAX_RESPONSIBLE_NODES};
		
		return createAndStartAgent(containerController, agentLocalName, "csm.agent.NetworkAgent", arguments);
	}
	

	private static AgentController createAndStartAgent(ContainerController containerController, String agentLocalName, String className, Object[] arguments) {
		AgentController agent = null;
		try {
			agent = containerController.createNewAgent(agentLocalName, className, arguments);
			agent.start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
		
		return agent;
	}
	
	public static void sendMessage(Agent sender, int messageType, String receiverName, Serializable contentObject, String conversationID) {
		ACLMessage message = new ACLMessage(messageType);
		message.addReceiver(new AID(receiverName, AID.ISGUID));
		try {
			message.setContentObject(contentObject);
			message.setConversationId(conversationID);
			sender.send(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static int getNumSearchAgentsFromAMS(Agent agent) {
		int numSearchAgent = 0;
		AMSAgentDescription[] agents = null;
		SearchConstraints c = new SearchConstraints();
		c.setMaxResults(new Long(-1));
		try {
			agents = AMSService.search(agent, new AMSAgentDescription(), c);
			for (int i=0; i<agents.length;i++){
			     AID agentID = agents[i].getName();
			     if (agentID.getName().startsWith("Sa")) {
			    	 numSearchAgent++;
			     }
			}
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		
		return numSearchAgent;
	}
	
	public static List<String> getAllSearchAgentsFromAMS(Agent agent) {
		List<String> searchAgents = new ArrayList<>();
		AMSAgentDescription[] agents = null;
		SearchConstraints c = new SearchConstraints();
		c.setMaxResults(new Long(-1));
		try {
			agents = AMSService.search(agent, new AMSAgentDescription(), c);
			for (int i=0; i<agents.length;i++){
			     AID agentID = agents[i].getName();
			     if (agentID.getName().startsWith("Sa")) {
			    	 searchAgents.add(agentID.getName());
			     }
			}
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		
		return searchAgents;
	}
	
	public static void shutDownPlatform(Agent myAgent) {
		 ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
         Codec codec = new SLCodec();
         myAgent.getContentManager().registerLanguage(codec);
         myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
         shutdownMessage.addReceiver(myAgent.getAMS());
         shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
         shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
         try {
             myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
             myAgent.send(shutdownMessage);
         }
         catch (Exception e) {
             e.printStackTrace();
         }
	}

	public static List<String> readAvaibleContainers() {
		List<String> containers = new ArrayList<>();
		String ID;
		Scanner s;
		do {
			s = new Scanner(System.in);
			System.out.print("Container's ID: ");
			ID = s.next();
			if(ID.equals(".")) {
				s.close();
				break;
			}
			containers.add(ID);
		}while(true);
		
		return containers;
	}
}

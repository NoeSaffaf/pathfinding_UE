package csm.utility;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import csm.model.search.Arc;
import csm.model.search.Node;

public class GraphUtility {
	
	public static String getParentEntityFromURI(String URI) {
		Model model = null;
		try {
			model = readModelFromURI(URI);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return getParentEntity(model, URI);
	}
	
	private static String getParentEntity(Model model, String IRI) throws NullPointerException{
		Resource entity = model.getResource(IRI);
		Statement statement = entity.getProperty(model.createProperty(UEAOntology.IS_UNDER_ORGANISATIONAL_ENTITY));
		if (statement == null)
			statement = entity.getProperty(model.createProperty(UEAOntology.IS_CHILD_ENTITY_OF));
		if (statement == null)
			return null;
		
		return statement.getResource().getURI();
	}
	
	
	public static List<String> getAncestorList(String nodeIRI) {
		List<String> ancestorList = new ArrayList<>();
		String ancestor;
		ancestor = getParentEntityFromURI(nodeIRI);
		if (ancestor == null)
			return ancestorList;

		ancestorList.add(ancestor);
		ancestorList.addAll(getAncestorList(ancestor));
		return ancestorList;
	}
	
	public static List<Arc> getArcs(Node parentNode) {
		String locationURI = parentNode.getURI();
		List<Arc> arcs = new ArrayList<Arc>();
		Map<String, List<String>> links;
		try {
			links = GraphUtility.getConnectionsFromURI(locationURI);
			for (Map.Entry<String, List<String>> link : links.entrySet()) {
				String childNodeURI = link.getKey();
				List<String> connectingResources = link.getValue();
				for (String connectingResource : connectingResources) {
					arcs.add(new Arc(parentNode, childNodeURI, connectingResource));
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return arcs;
	}
	
	
	public static List<String> getSuccessors(String pathNodeUri) {
		List<String> successors = new ArrayList<>();
		Map<String, List<String>> links;
		try {
			links = getConnectionsFromURI(pathNodeUri);
			for (Map.Entry<String, List<String>> link : links.entrySet()) {
				String childNodeURI = link.getKey();
				successors.add(childNodeURI);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return successors;
	}
	
	private static Map<String, List<String>> getConnectionsFromURI(String IRI) throws IOException{
		Model model = readModelFromURI(IRI);
		Map<String, List<String>> connections = getConnections(IRI, model);
		return connections;
	}
	
	private static Model readModelFromURI(String URI) throws IOException {
		Model model = ModelFactory.createDefaultModel();
		model.read(new URL(URI).openStream(), null, "TTL");
		return model;
	}
	
	// Get connection with connecting resources
	private static Map<String, List<String>> getConnections(String IRI, Model model) throws IOException{
		Map<String, List<String>> connections = new HashMap<String, List<String>>();
		Resource location = model.getResource(IRI);
		StmtIterator iterator = location.listProperties(model.createProperty(UEAOntology.HAS_CONNECTION));
		while (iterator.hasNext()) {
			String connectionURI = iterator.next().getObject().toString();
			Model modelConnection = readModelFromURI(connectionURI);
			
			Resource connection = modelConnection.getResource(connectionURI);
			Resource destination = (Resource) modelConnection.listObjectsOfProperty(connection, model.createProperty(UEAOntology.HAS_DESTINATION)).next();
			
			StmtIterator iteratorConnection = connection.listProperties(model.createProperty(UEAOntology.HAS_CONNECTING_RESOURCE));
			List<String> connectingResources = new ArrayList<String>();
			while (iteratorConnection.hasNext()) {
				Resource connectingResource = (Resource) iteratorConnection.next().getObject();
				connectingResources.add(connectingResource.toString());
			}
			connections.put(destination.toString(), connectingResources);
		}
		return connections;
	}
	
	
	public static String getOrganisationalEntitysNameFromIRI(String hierarchyIRI) {
		String segments[] = hierarchyIRI.split("/");
		return segments[segments.length - 1];
	}

	public static boolean isNodeInList(Collection<Node> list, String uri)
	{
		for (Node n : list)
		{
			if (n.getURI().equals(uri)){
				return true;
			}
		}
		return false;
	}

	public static Node getNodeFromList(Collection<Node> list, String uri)
	{
		for (Node n : list)
		{
			if (n.getURI().equals(uri)){

				return n;
			}
		}
		return null;
	}
}

package csm.utility;

public class UEAOntology {

	public static final String ONTOLOGY_IRI = "http://www.semanticweb.org/kod/ontologies/2017/1/ueao#";

	// Classes
	public static final String RESOURCE = ONTOLOGY_IRI + "Resource";
	public static final String CONNECTION = ONTOLOGY_IRI + "Connection";
	public static final String CONNECTING_POINT = ONTOLOGY_IRI + "ConnectingPoint";
	public static final String LOCATION = ONTOLOGY_IRI + "Location";
	public static final String ORGANISATIONAL_ENTITY = ONTOLOGY_IRI + "OrganisationalEntity";
	public static final String CPS_ENTITY = ONTOLOGY_IRI + "CPSEntity";
	public static final String PHYSICAL_ENTITY = ONTOLOGY_IRI + "PhysicalEntity";
	public static final String SOCIAL_ENTITY = ONTOLOGY_IRI + "SocialEntity";
	public static final String CYBER_PHYSICAL_ENTITY = ONTOLOGY_IRI + "CyberPhysicalEntity";
	public static final String CYBER_PHYSICAL_SOCIAL_ENTITY = ONTOLOGY_IRI + "CyberPhysicalSocialEntity";
	public static final String POTENTIAL_ACTIVITY = ONTOLOGY_IRI + "PotentialActivity";

	// Object properties
	public static final String HAS_CONNECTING_RESOURCE = ONTOLOGY_IRI + "hasConnectingResource";
	public static final String HAS_CONNECTION = ONTOLOGY_IRI + "hasConnection";
	public static final String HAS_ORIGIN = ONTOLOGY_IRI + "hasOrigin";
	public static final String HAS_DESTINATION = ONTOLOGY_IRI + "hasDestination";
	public static final String HAS_ORIGIN_ENTITY = ONTOLOGY_IRI + "hasOriginEntity";
	public static final String HAS_DESTINATION_ENTITY = ONTOLOGY_IRI + "hasDestinationEntity";
	public static final String HAS_CONNECTING_POINT = ONTOLOGY_IRI + "hasConnectingPoint";
	public static final String CONTAINS_LOCATION = ONTOLOGY_IRI + "containsLocation";
	public static final String IS_UNDER_ORGANISATIONAL_ENTITY = ONTOLOGY_IRI + "isUnderOrganisationalEntity";
	public static final String IS_PARENT_ENTITY_OF = ONTOLOGY_IRI + "isParentEntityOf";
	public static final String IS_CHILD_ENTITY_OF = ONTOLOGY_IRI + "isChildEntityOf";
	public static final String CONTAINS_ENTITY = ONTOLOGY_IRI + "containsEntity";
	public static final String HAS_ENTITY_RESOURCE = ONTOLOGY_IRI + "hasEntityResource";
	public static final String SUPPORTS_ACTIVITY = ONTOLOGY_IRI + "supportsActivity";
	public static final String IS_OF_RESOURCE_TYPE = ONTOLOGY_IRI + "isOfResourceType";
}

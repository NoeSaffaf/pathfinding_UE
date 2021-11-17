package csm.communication;

// Interface the ID of different types of communications among agents
public interface ConversationIDs {
	// Arc cost
	public static final String REQUEST_ARC_COST = "request-arc-cost-conversation-id";
	public static final String INFORM_UPDATED_ARC_COST = "inform-update-cost";
	
	// Routing protocol
	public static final String FIND_AGENT_RESPONSIBLE = "request-find-agent-responsible-conversation-id";
	public static final String INFORM_AGENT_RESPONSIBLE_TO_AGENT_REQUESTER = "inform-agent-responsible-to-agent-requester";
	public static final String ASSIGN_ORGANISATIONAL_ENTITY = "request-assign-organisational_entity-conversation-id";
	public static final String REQUEST_SEARCH_AGENT_TO_HANDLE_ACTION = "request-handle-action";
	public static final String ASSIGN_NEW_NODE_VIA_ACTION = "request-assign-new-node-via-action";
	
	// GVP
	public static final String PROPAGATE_GVP_REQUEST = "propagate-gvp-request";
	public static final String PROPAGATE_GVP_RESPONSE = "propagate-gvp-response";	
	
	// Search status
	public static final String PROPAGATE_SEARCH_STATUS_INFO_REQUEST = "propagate-search-status-info-request";
	public static final String PROPAGATE_SEARCH_STATUS_INFO_RESPONSE = "propagate-search-status-info-response";

	// Dynamic
	public static final String REQUEST_TAKE_CHARGE_NODE_IN_PATH = "request-take-charge-node-in-path";
	public static final String RESPONSE_TAKE_CHARGE_NODE_IN_PATH = "response-take-charge-node-in-path";
	public static final String REQUEST_FORWARD_NODE_INFO_TO_GOAL_AGENT = "request-forward-node-info-to-goal-agent";
	
	public static final String REQUEST_SUBSCRIBE_NODE_UPDATES = "request-subscribe-node-updates";
	public static final String NOTIFICATION_NODE_UPDATES = "notification-node-updates";
	public static final String REQUEST_SUBSCRIBE_SUCCESSOR_NODE_UPDATES = "request-subscribe-successor-node-updates";
	public static final String REQUEST_SUCCESSOR_NODE_INFO = "request-successor-node-info";
	public static final String INFORM_REMOTE_SUCCESSOR_UPDATES = "inform-remote-successor-updates";
	public static final String INFORM_REMOTE_SUCCESSOR_INFO = "inform-remote-sucessor-info";

	public static final String REQUEST_REEXPAND_SUCCESSOR_NODE = "request-reexpand-successor-node";
	public static final String REQUEST_RESUME_SEARCH = "request-resume-search";
	
}

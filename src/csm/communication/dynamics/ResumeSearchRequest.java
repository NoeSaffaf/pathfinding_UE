package csm.communication.dynamics;

import java.io.Serializable;
import java.util.List;

public class ResumeSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String goalAgent;
    private String senderAgent;
    private List<String> rootAffectedNodeList;

    public ResumeSearchRequest(String goalAgent, String senderAgent, List<String> rootAffectedNodeList) {
        this.goalAgent = goalAgent;
        this.senderAgent = senderAgent;
        this.rootAffectedNodeList = rootAffectedNodeList;
    }

    public String getGoalAgent() {
        return goalAgent;
    }

    public List<String> getRootAffectedNodeList() { return rootAffectedNodeList; }

    public String getSenderAgent() {
        return senderAgent;
    }

}

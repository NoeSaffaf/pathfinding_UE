package csm.communication.dynamics;

import java.io.Serializable;
import java.util.List;

public class ReexpandSuccessorNodeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nodeURI;
    private String parentURI;
    private List<String> ancestorsList;

    public ReexpandSuccessorNodeRequest(String nodeURI, String parentURI, List<String> ancestorsList) {
        this.nodeURI = nodeURI;
        this.parentURI = parentURI;
        this.ancestorsList = ancestorsList;
    }

    public String getNodeURI() {
        return nodeURI;
    }

    public String getParentURI() {
        return parentURI;
    }

    public List<String> getAncestorsList() {return ancestorsList;}
}

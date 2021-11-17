package csm.SimulatedValue;

public class SimulatedValues {
    private String nodeUri;
    private String parentNodeUri;
    private int newArc;

    public SimulatedValues( String parentNodeUri, String nodeUri, int newArc) {
        this.nodeUri = nodeUri;
        this.parentNodeUri = parentNodeUri;
        this.newArc = newArc;
    }

    public String getNodeUri() {
        return nodeUri;
    }

    public String getParentNodeUri() {
        return parentNodeUri;
    }

    public int getNewArc() {
        return newArc;
    }
}

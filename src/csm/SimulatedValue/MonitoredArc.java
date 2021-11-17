package csm.SimulatedValue;

public class MonitoredArc {
    private String parent;
    private String child;

    public MonitoredArc(String parent, String child) {
        this.parent = parent;
        this.child = child;
    }

    public String getParent() {
        return parent;
    }

    public String getChild() {
        return child;
    }
}

package csm.SimulatedValue;

import java.util.ArrayList;
import java.util.Random;

public class SimulatedValuesHolder {
    public static ArrayList<MonitoredArc> monitoredArcs = new ArrayList<>();
    public static ArrayList<SimulatedValues> listSimulatedvalues = new ArrayList<SimulatedValues>();

    public static void addMonitoredArc(String parent, String child) {
        monitoredArcs.add(new MonitoredArc(parent, child));
    }

    public static MonitoredArc pullRandomMonitoredArc()
    {
        Random rand = new Random();
        return monitoredArcs.get(rand.nextInt(monitoredArcs.size()));
    }

    public static void fillList(){
        //listSimulatedvalues.add(new SimulatedValues("http://127.0.0.1/Graph/location/location_3","http://127.0.0.1/Graph/location/location_5",10));
    }

    public static void fillListWithValue(String parent, String child, int newcost)
    {
        listSimulatedvalues.add(new SimulatedValues(parent,child,newcost));
    }
}

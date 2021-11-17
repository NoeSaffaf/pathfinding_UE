package csm.Evaluation;

import jade.lang.acl.UnreadableException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LatencyHolder {

    private static long totalLatency = 0;
    private static HashMap<String, Long> latencyByResourceAgent = new HashMap();

    public static void addLatency(long latency){
        totalLatency += latency;
    }

    public static void addLatencyName(Long latency, String name)
    {
        latencyByResourceAgent.put(name, latencyByResourceAgent.get(name)+latency);
    }

    public static void resetLatency()
    {
        totalLatency = 0;
        Iterator it = latencyByResourceAgent.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            latencyByResourceAgent.put((String) pair.getKey(), 0L);

        }
    }

    public static void subscribeToLatencyList(String nameResourceAgent){
        try {
            latencyByResourceAgent.put(nameResourceAgent, 0L);
        } catch (UnknownError e){
            System.out.println(e);
        }
    }

    public static long getTotalLatency(){return totalLatency;}

    public static void printLatencyName(){
        Iterator it = latencyByResourceAgent.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println("Agent Name : " + pair.getKey() + ", Total latency : " + pair.getValue());
        }
    }
}

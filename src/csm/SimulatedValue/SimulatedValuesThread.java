package csm.SimulatedValue;

import java.util.Scanner;

public class SimulatedValuesThread {

    public void start(){
        ThreadInitiator a = new ThreadInitiator();
        a.launchThread();
    }

    public class ThreadInitiator {
        Thread changeDetector;

        public void launchThread() {
            changeDetector = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000);
                        //SimulatedValuesHolder.fillList();

                        MonitoredArc monitoredArc = SimulatedValuesHolder.pullRandomMonitoredArc();
                        int newCost = 10;
                        SimulatedValuesHolder.fillListWithValue(monitoredArc.getParent(),monitoredArc.getChild(), newCost);

                        System.out.println("Values changed!!!");
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            changeDetector.start();
        }
    }
}

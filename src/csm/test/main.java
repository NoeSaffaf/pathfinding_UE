package csm.test;

import csm.SimulatedValue.SimulatedValuesThread;

public class main {
    public static void main(String[] args) {
        SimulatedValuesThread simulatedValuesThread = new SimulatedValuesThread();
        simulatedValuesThread.start();
    }
}

package csm.agent;

import jade.core.Agent;

// Astract class of representing all types of agents in CSM
public abstract class CSMAgent extends Agent{
	private static final long serialVersionUID = 1L;

	protected void print(Object object) {
		System.out.print("[" + getName() + "]: " + object);
	}
	
	protected void println(Object object) {
		print(object);
		System.out.println();
	}
}

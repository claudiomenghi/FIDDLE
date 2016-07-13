package ltsa.lts.csp;

import ltsa.lts.lts.StateMachine;


public abstract class Declaration {
	public static final int TAU = 0;
	public static final int TAU_MAYBE = 1;
	public static final int ERROR = -1;
	public static final int STOP = 0;
	public static final int SUCCESS = 1;

	public void explicitStates(StateMachine m) {
	};

	/**
	 * makes sure aliases refer to the same state
	 * 
	 * @param m
	 *            the state machine to be considered
	 */
	public void crunch(StateMachine m) {
	};

	public void transition(StateMachine m) {
	};
}

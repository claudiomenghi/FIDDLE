package ltsa.lts.csp;

import ltsa.lts.automata.automaton.StateMachine;

public abstract class Declaration {
	public static final int TAU = 0;
	public static final int TAU_MAYBE = 1;
	public static final int ERROR = -1;
	public static final int STOP = 0;
	public static final int SUCCESS = 1;
	
	/**
	 * adds the states of the declaration to the state machine
	 * 
	 * @param stateMachine
	 *            the state machine to be modified
	 */
	public void explicitStates(StateMachine stateMachine) {
	};

	/**
	 * makes sure aliases refer to the same state
	 * 
	 * @param stateMachine
	 *            the state machine to be considered
	 */
	protected void crunch(StateMachine stateMachine) {
	};

	/**
	 * adds the transitions of the declaration to the state machine
	 * 
	 * @param stateMachine
	 *            the state machine to be modified
	 */
	public void transition(StateMachine stateMachine) {
	};
}

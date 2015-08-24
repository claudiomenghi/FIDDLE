package controller.game.model;

import java.util.Set;

public interface Game<State> {

	public abstract boolean isUncontrollable(State state);

	public abstract Set<State> getUncontrollableSuccessors(State state);

	public abstract Set<State> getControllableSuccessors(State state);
	
	public abstract Set<State> getSuccessors(State state);

	public abstract Set<State> getPredecessors(State state);

	public abstract Set<State> getStates();

	public abstract void addControllableSuccessor(State state1, State state2);

	public abstract void addUncontrollableSuccessor(State predecessor, State successor);
	
	public abstract Set<State> getInitialStates();

}
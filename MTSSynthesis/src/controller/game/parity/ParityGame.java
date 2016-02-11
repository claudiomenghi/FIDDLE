package controller.game.parity;

import java.util.Set;

import controller.game.model.StateBasedGame;

public class ParityGame<State> extends StateBasedGame<State> {
	
	private ParityGoal<State> goal;
	
	
	public ParityGame(Set<State> initialStates, Set<State> states) {
		super(initialStates, states);
	}
	
	public Integer getPriority(State state){
		return this.getGoal().getPriority(state);
	}
	
	@Override
	public ParityGoal<State> getGoal() {
		return this.goal;
	}
}

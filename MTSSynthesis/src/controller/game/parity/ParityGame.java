package controller.game.parity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import controller.game.model.StateBasedGame;

public class ParityGame<State> extends StateBasedGame<State> {
	
	private Map<State, Integer> priorities;
	
	public ParityGame(Set<State> initialStates, Set<State> states) {
		super(initialStates, states);
	}
	
	public Integer getPriority(State state){
		return this.priorities.get(state);
	}
	
	protected Set<State> getOddPriorityStates() {
		Set<State> odds = new HashSet<State>();
		for (Map.Entry<State, Integer> entry : this.priorities.entrySet()) {
			if (entry.getValue()%2==1) {
				odds.add(entry.getKey());
			}
		};
		return odds;
	}
}

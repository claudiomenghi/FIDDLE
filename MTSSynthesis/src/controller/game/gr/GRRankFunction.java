package controller.game.gr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import controller.game.model.AbstractRankFunction;
import controller.game.model.Rank;
import controller.game.model.RankFunction;

public class GRRankFunction<State> extends AbstractRankFunction<State> implements RankFunction<State> {

	@Override
	public GRRank getMinimum(Set<State> states) {
		return (GRRank) super.getMinimum(states);
	}

	@Override
	public GRRank getMaximum(Set<State> states) {
		return (GRRank) super.getMaximum(states);
	}

	private Map<State,GRRank> function; // This is a ranking function. It maps every state to a Rank
	private GRRankContext context;
	
	public GRRankFunction(Set<State> allStates,int height,int width) {
		this.init(height, width);
		this.setInitialRank(allStates);
	}

	protected void init(int height, int width) {
		this.context = new GRRankContext(height,width);
		this.function = new HashMap<State, GRRank>();
	}

	protected void setInitialRank(Set<State> allStates) {
		for (State state : allStates) {
			GRRank initialRank = new GRRank(this.context);
			function.put(state, initialRank);
		}
	}
	
	@Override
	public String toString() {
		return "Context: " + this.context.toString() + "\n"
		+ "Rank Function: " + this.function.toString();
	}

	@Override
	public GRRank getRank(State state) {
		return this.function.get(state);
	}

	@Override
	public GRRankContext getContext() {
		return this.context;
	}

	@Override
	public Rank getNewRank() {
		return new GRRank(this.getContext());
	}
}

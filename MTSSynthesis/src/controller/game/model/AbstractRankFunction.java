package controller.game.model;

import java.util.Set;


public abstract class AbstractRankFunction<State> implements RankFunction<State> {

	@Override
	public abstract Rank getRank(State state);

	@Override
	public abstract RankContext getContext();

	public abstract Rank getNewRank();

	@Override
	public void increase(State state) {
		this.getRank(state).increase();
	}

	@Override
	public boolean isInfinity(State state) {
		return this.getRank(state).isInfinity();
	}

	@Override
	public void setRank(State state, Rank rank) {
		this.getRank(state).set(rank);
	}

	@Override
	public Rank getMinimum(Set<State> states) {
		Rank minimum = this.getNewRank();
		minimum.setToInfinity();
		
		for (State state : states) {
			Rank rank = this.getRank(state);
			if (rank.compareTo(minimum)<0)
				minimum.set(rank);
		}
		return minimum;
	}

	@Override
	public Rank getMaximum(Set<State> states) {
		Rank maximum = this.getNewRank();
			
		for (State state : states) {
			Rank rank = this.getRank(state);
			if (rank.compareTo(maximum)>0)
				maximum.set(rank);
		}
		return maximum;
	}

}
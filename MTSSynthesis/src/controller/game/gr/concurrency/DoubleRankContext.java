package controller.game.gr.concurrency;

import controller.game.model.RankContext;

public class DoubleRankContext implements RankContext{

	private int max;
	
	public DoubleRankContext(int max) {
		this.max = max;
	}
	
	@Override
	public boolean inRange(int value, int assume) {
		return value >= 0 && value <= max;
	}
	
	
	@Override
	public String toString() {
		return "[max:" + this.max + "]";
	}
	
	public int getMax() {
		return max;
	}

}

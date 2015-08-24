package controller.model;

import ac.ic.doc.mtstools.model.LTS;
import controller.game.gr.StrategyState;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.GRGame;

public abstract class GRGameControlProblem<S,A,M> extends GRControlProblem<S, A, M> {
	
	protected GRGame<S> g;
	
	public GRGameControlProblem(LTS<S, A> environment, GRControllerGoal<A> grControllerGoal) {
		super(environment, grControllerGoal);
	}

	public GRGame<S> getGRGame(){
		return g;
	}

	//TODO: Temporal solution for Transition System Dispatcher need of a LTS<StrategyState..
	public abstract LTS<StrategyState<S, Integer>, A> rawSolve();

}

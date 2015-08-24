package controller;

import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.MTS;
import controller.game.gr.StrategyState;
import controller.model.ControlProblem;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.GRGame;
import controller.synchronous.SynchronousControlProblem;

public class ControllerSynthesisFacade<S, A, M> {
	//TODO Dependency Injection
	private LTSControllerSynthesiserImpl<S,A> synthesiser = new LTSControllerSynthesiserImpl<S, A>();
	
	public MTS<StrategyState<S, Integer>, A> synthesiseController(MTS<S, A> mts, GRControllerGoal<A> goal) {
		MTS<StrategyState<S, Integer>, A> synthesised = synthesiser.synthesiseGR(mts, goal);
		return synthesised;
	}	

	public boolean checkAssumptionsCompatibility(MTS<S, A> mts, GRControllerGoal<A> goal) {
		return synthesiser.checkGRAssumptionsCompatibility(mts, goal);
	}

	public LTS<S, A> synthesiseController(ControlProblem<S, A> cp) {
		return cp.solve();
	}

	public LTS<StrategyState<S, Integer>, A> synthesiseController(SynchronousControlProblem<S, A> cp) {
		return cp.solve();
	}

	public GRGame<S> getGame()
	{
	  return synthesiser.getGame();
	}
}

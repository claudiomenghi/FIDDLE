package MTSSynthesis.controller;

import MTSTools.ac.ic.doc.mtstools.model.LTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSSynthesis.controller.game.gr.StrategyState;
import MTSSynthesis.controller.model.ControlProblem;
import MTSSynthesis.controller.model.gr.GRControllerGoal;
import MTSSynthesis.controller.model.gr.GRGame;
import MTSSynthesis.controller.synchronous.SynchronousControlProblem;

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

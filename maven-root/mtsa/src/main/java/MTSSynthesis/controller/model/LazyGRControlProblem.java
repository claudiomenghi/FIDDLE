package MTSSynthesis.controller.model;

import MTSTools.ac.ic.doc.mtstools.model.LTS;
import MTSTools.ac.ic.doc.mtstools.model.impl.MTSAdapter;
import MTSSynthesis.controller.game.gr.GRRankSystem;
import MTSSynthesis.controller.game.gr.StrategyState;
import MTSSynthesis.controller.game.gr.lazy.LazyGRGameSolver;
import MTSSynthesis.controller.game.util.GRGameBuilder;
import MTSSynthesis.controller.game.util.GameStrategyToLTSBuilder;
import MTSSynthesis.controller.game.util.GenericLTSStrategyStateToStateConverter;
import MTSSynthesis.controller.model.gr.GRControllerGoal;
import MTSSynthesis.controller.model.gr.GRGame;


public class LazyGRControlProblem<S,A> extends GRControlProblem<S, A, Integer>{

	protected LazyGRGameSolver<S> lazyGRGameSolver;
	
	public LazyGRControlProblem(LTS<S,A> originalEnvironment, GRControllerGoal<A> grControllerGoal) {
		super(originalEnvironment, grControllerGoal);
	}
		
	@Override
	protected LTS<S,A> primitiveSolve() {
		GRGame<S> perfectInfoGRGame = new GRGameBuilder<S,A>().buildGRGameFrom(new MTSAdapter<S,A>(environment), grControllerGoal);
		GRRankSystem<S> grRankSystem = new GRRankSystem<S>(perfectInfoGRGame.getStates(), perfectInfoGRGame.getGoal().getGuarantees(),perfectInfoGRGame.getGoal().getAssumptions(), perfectInfoGRGame.getGoal().getFailures());
		lazyGRGameSolver = new LazyGRGameSolver<S>(perfectInfoGRGame, grRankSystem, grControllerGoal.getLazyness());
		lazyGRGameSolver.solveGame();
		LTS<StrategyState<S, Integer>, A> result = GameStrategyToLTSBuilder.getInstance().buildLTSFrom(environment,lazyGRGameSolver.buildStrategy());
		return new GenericLTSStrategyStateToStateConverter<S, A, Integer>().transform(result);		
	}	
	
}





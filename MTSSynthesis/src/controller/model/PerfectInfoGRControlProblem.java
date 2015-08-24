package controller.model;

import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import controller.game.gr.GRRankSystem;
import controller.game.gr.StrategyState;
import controller.game.gr.perfect.PerfectInfoGRGameSolver;
import controller.game.util.GRGameBuilder;
import controller.game.util.GameStrategyToLTSBuilder;
import controller.game.util.GenericLTSStrategyStateToStateConverter;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.GRGame;


public class PerfectInfoGRControlProblem<S,A> extends GRControlProblem<S, A, Integer>{

	protected PerfectInfoGRGameSolver<S> perfectInfoGRGameSolver;
	private GRGame<S> g;

	public PerfectInfoGRControlProblem(LTS<S,A> originalEnvironment, GRControllerGoal<A> grControllerGoal) {
		super(originalEnvironment, grControllerGoal);
	}
		
	@Override
	protected LTS<S,A> primitiveSolve() {
		GRGame<S> perfectInfoGRGame = new GRGameBuilder<S,A>().buildGRGameFrom(new MTSAdapter<S,A>(environment), grControllerGoal);
		g = perfectInfoGRGame;
		GRRankSystem<S> grRankSystem = new GRRankSystem<S>(
				perfectInfoGRGame.getStates(), perfectInfoGRGame.getGoal().getGuarantees(),
				perfectInfoGRGame.getGoal().getAssumptions(), perfectInfoGRGame.getGoal().getFailures());
		perfectInfoGRGameSolver = new PerfectInfoGRGameSolver<S>(perfectInfoGRGame, grRankSystem);
		perfectInfoGRGameSolver.solveGame();
		LTS<StrategyState<S, Integer>, A> result = GameStrategyToLTSBuilder
				.getInstance().buildLTSFrom(environment,
						perfectInfoGRGameSolver.buildStrategy());
		result.removeUnreachableStates();
		return new GenericLTSStrategyStateToStateConverter<S, A, Integer>()
				.transform(result);		
	}	
	
	public GRGame<S> getGRGame() {
		return g;
	}
	
}
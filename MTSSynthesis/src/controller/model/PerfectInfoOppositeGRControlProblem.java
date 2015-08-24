package controller.model;

import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import controller.game.gr.GRRankSystem;
import controller.game.gr.StrategyState;
import controller.game.gr.perfect.PerfectInfoOppositeGRGameSolver;
import controller.game.model.GameSolver;
import controller.game.util.GRGameBuilder;
import controller.game.util.GameStrategyToLTSBuilder;
import controller.game.util.GenericLTSStrategyStateToStateConverter;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.concurrency.GRCGame;


public class PerfectInfoOppositeGRControlProblem<S,A> extends
	StateSpaceCuttingControlProblem<S, A>{

	public PerfectInfoOppositeGRControlProblem(LTS<S,A> originalEnvironment, GRControllerGoal<A> grControllerGoal) {
		super(originalEnvironment, grControllerGoal);
	}	
	
	@Override
	protected LTS<S,A> primitiveSolve() {
		//cut according to all the predefined game solvers

		gameSolver.solveGame();
		LTS<StrategyState<S, Integer>, A> result = GameStrategyToLTSBuilder
				.getInstance().buildLTSFrom(environment,
						gameSolver.buildStrategy());
		return new GenericLTSStrategyStateToStateConverter<S, A, Integer>()
				.transform(result);		
	}	
	
	@Override
	protected GameSolver<S, Integer> buildGameSolver() {
		// TODO Auto-generated method stub

		GRCGame<S> perfectInfoGRGame = new GRGameBuilder<S,A>().buildGRCGameFrom(new MTSAdapter<S,A>(environment), grControllerGoal.cloneWithAssumptionsAsGoals());
		GRRankSystem<S> grRankSystem = new GRRankSystem<S>(
				perfectInfoGRGame.getStates(), perfectInfoGRGame.getGoal().getGuarantees(),
				perfectInfoGRGame.getGoal().getAssumptions(), perfectInfoGRGame.getGoal().getFailures());
		
		PerfectInfoOppositeGRGameSolver<S> perfectInfoOppositeGRGameSolver = new PerfectInfoOppositeGRGameSolver<S>(perfectInfoGRGame,  grRankSystem);
		return perfectInfoOppositeGRGameSolver;
	}
}





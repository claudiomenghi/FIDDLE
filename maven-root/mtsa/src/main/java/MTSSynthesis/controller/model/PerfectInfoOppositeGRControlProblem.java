package MTSSynthesis.controller.model;

import MTSTools.ac.ic.doc.mtstools.model.LTS;
import MTSTools.ac.ic.doc.mtstools.model.impl.MTSAdapter;
import MTSSynthesis.controller.game.gr.GRRankSystem;
import MTSSynthesis.controller.game.gr.StrategyState;
import MTSSynthesis.controller.game.gr.perfect.PerfectInfoOppositeGRGameSolver;
import MTSSynthesis.controller.game.model.GameSolver;
import MTSSynthesis.controller.game.util.GRGameBuilder;
import MTSSynthesis.controller.game.util.GameStrategyToLTSBuilder;
import MTSSynthesis.controller.game.util.GenericLTSStrategyStateToStateConverter;
import MTSSynthesis.controller.model.gr.GRControllerGoal;
import MTSSynthesis.controller.model.gr.concurrency.GRCGame;


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





package controller.model.gr;

import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import controller.game.gr.StrategyState;
import controller.game.gr.concurrency.ConcurrencyFunction;
import controller.game.gr.concurrency.ConcurrencyGameSolver;
import controller.game.gr.concurrency.DoubleRankFunction;
import controller.game.util.GRGameBuilder;
import controller.game.util.GameStrategyToLTSBuilder;
import controller.game.util.GenericLTSStrategyStateToStateConverter;
import controller.model.GRGameControlProblem;
import controller.model.gr.concurrency.GRCGame;

public class ConcurrencyControlProblem<S,A,M> extends GRGameControlProblem<S, A, M> {
	
	GRGame<S> g;

	public ConcurrencyControlProblem(LTS<S, A> environment, GRControllerGoal<A> grControllerGoal){
		super(environment, grControllerGoal);
	}
	
	@Override
	protected LTS<S, A> primitiveSolve() {
		return new GenericLTSStrategyStateToStateConverter<S, A, Integer>().transform(rawSolve()); 
	}
	
	@Override
	public	LTS<StrategyState<S, Integer>, A>  rawSolve(){
		GRCGame<S> cgame = new GRGameBuilder<S, A>().buildGRCGameFrom(new MTSAdapter<S,A>(environment), grControllerGoal);
		g = cgame;
		DoubleRankFunction<S> function = new ConcurrencyFunction<S>(environment.getStates(), cgame.getGoal().getConcurrencyInformation());
		ConcurrencyGameSolver<S,A> solver = new ConcurrencyGameSolver<S,A>(cgame, environment, function,cgame.getGoal().getGuarantee(cgame.getGoal().getGuarantees().size()).getStateSet());
		LTS<StrategyState<S, Integer>, A> result = GameStrategyToLTSBuilder.getInstance().buildLTSFrom(environment,solver.buildStrategy());
		result.removeUnreachableStates();
		return result;
	}
	
	@Override
	public GRGame<S> getGRGame() {
		return g;
	}
}

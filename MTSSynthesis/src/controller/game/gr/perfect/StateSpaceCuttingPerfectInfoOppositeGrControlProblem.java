package controller.game.gr.perfect;

import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import controller.game.gr.GRRankSystem;
import controller.game.gr.StrategyState;
import controller.game.model.GameSolver;
import controller.game.util.GRGameBuilder;
import controller.game.util.GameStrategyToLTSBuilder;
import controller.game.util.GenericLTSStrategyStateToStateConverter;
import controller.model.OppositeSafeControlProblem;
import controller.model.PerfectInfoOppositeGRControlProblem;
import controller.model.StateSpaceCuttingControlProblem;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.GRGame;
import controller.model.gr.concurrency.GRCGame;

public class StateSpaceCuttingPerfectInfoOppositeGrControlProblem<S,A> extends
		StateSpaceCuttingControlProblem<S, A> {
	

	public StateSpaceCuttingPerfectInfoOppositeGrControlProblem(LTS<S, A> env,
			GRControllerGoal<A> grControlGoal) {
		super(env, grControlGoal);
	}

	protected Set<S> solveOppositeSafe(LTS<S,A> currentEnvironment){
		OppositeSafeControlProblem<S, A> safeControlProblem = new OppositeSafeControlProblem<S, A>(currentEnvironment, grControllerGoal);
		return safeControlProblem.getWinningStates();		
	}
	
	protected Set<S> solveOppositeGR(LTS<S,A> currentEnvironment){
		PerfectInfoOppositeGRControlProblem<S, A> oppositeGRControlProblem = new PerfectInfoOppositeGRControlProblem<S, A>(currentEnvironment, grControllerGoal);
		return oppositeGRControlProblem.getWinningStates();		
	}
	
	@Override
	protected LTS<S, A> primitiveSolve() {
		Set<S> winningNoG = solveOppositeSafe(environment);
		Set<S> winningAssumptions = solveOppositeGR(environment);
		
		SetView<S> losingStates = Sets.difference(winningNoG, winningAssumptions);
		removelosingStates(losingStates);
		gameSolver.solveGame();
		return environment;
	}	
	
	public LTS<S, A> buildStrategy(){
		LTS<StrategyState<S, Integer>, A> result = GameStrategyToLTSBuilder.getInstance().buildLTSFrom(environment, gameSolver.buildStrategy());		
		return new GenericLTSStrategyStateToStateConverter<S, A, Integer>().transform(result);		
	}
	
	@Override
	protected GameSolver<S, Integer> buildGameSolver() {
		// TODO Auto-generated method stub
		GRGame<S> game = new GRGameBuilder<S, A>().buildGRGameFrom(new MTSAdapter<S,A>(this.environment), grControllerGoal);
		GRRankSystem<S> system = new GRRankSystem<S>(game.getStates(), game.getGoal().getGuarantees(), game.getGoal().getAssumptions(), game.getGoal().getFailures());		
		return new PerfectInfoGRGameSolver<S>(game, system);
	}
	
	protected LTS<S, A> removelosingStates(Set<S> losingStates) {

		// remove transitions to winningstates
		for (S s : environment.getStates()) {
			for (Pair<A, S> p : environment.getTransitions(s)) {
				if (losingStates.contains(p.getSecond()))
					environment.removeTransition(s, p.getFirst(),
							p.getSecond());
			}
		}
		// remove transitions from winningstates
		for (S s : losingStates) {
			if(environment.getTransitions(s) != null){
				for (Pair<A, S> p : environment.getTransitions(s)) {
					environment.removeTransition(s, p.getFirst(),
							p.getSecond());
				}
			}
		}
		environment.removeUnreachableStates();
		return environment;
	}
	
	
}

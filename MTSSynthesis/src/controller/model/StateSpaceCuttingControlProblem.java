package controller.model;

import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.LTS;
import controller.game.model.GameSolver;
import controller.model.gr.GRControllerGoal;

public abstract class StateSpaceCuttingControlProblem<S, A> extends
	GRControlProblem<S, A, Integer> {

	protected GameSolver<S, Integer> gameSolver;


	protected abstract GameSolver<S, Integer> buildGameSolver();	
	
	public StateSpaceCuttingControlProblem(LTS<S, A> env,
			GRControllerGoal<A> grControlGoal) {
		super(env, grControlGoal);
		this.gameSolver = buildGameSolver();		
	}

	@Override
	protected LTS<S, A> primitiveSolve() {
		gameSolver.solveGame();		
		return environment;
	}
	
	public Set<S> getWinningStates(){
		gameSolver.solveGame();
		return gameSolver.getWinningStates();
	}

	public LTS<S, A> cutOriginalStateSpace() {
		SetView<S> winningStates = Sets.difference(gameSolver.getGame().getStates(), gameSolver.getWinningStates());
		// remove transitions to winningstates
		for (S s : environment.getStates()) {
			for (Pair<A, S> p : environment.getTransitions(s)) {
				if (winningStates.contains(p.getSecond()))
					environment.removeTransition(s, p.getFirst(),
							p.getSecond());
			}
		}
		// remove transitions from winningstates
		for (S s : winningStates) {
			for (Pair<A, S> p : environment.getTransitions(s)) {
				environment.removeTransition(s, p.getFirst(),
						p.getSecond());
			}
		}
		environment.removeUnreachableStates();
		return environment;
	}
}

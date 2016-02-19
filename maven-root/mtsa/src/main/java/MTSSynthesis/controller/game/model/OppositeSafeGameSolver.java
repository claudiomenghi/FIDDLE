package MTSSynthesis.controller.game.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import MTSSynthesis.controller.game.gr.StrategyState;
import MTSSynthesis.controller.game.oppositeSafe.OppositeSafeGame;

import MTSTools.ac.ic.doc.mtstools.model.LTS;

public class OppositeSafeGameSolver<S, A> extends
		StateSpaceCuttingGameSolver<S, A> {

	protected static int DUMMY_GOAL = 1;

	public OppositeSafeGameSolver(LTS<S, A> env, Set<A> controllable,
			Set<Set<S>> goalStates) {
		super(new OppositeSafeGame<S, A>(env, controllable),goalStates);
	}

	public void solveGame() {
		// Initialize
		Queue<S> losing = new LinkedList<S>();
		for (Set<S> actualGoalSet : this.goalStates) {
			this.initialise(losing, actualGoalSet);

			// Handle the pending states
			while (!losing.isEmpty()) {
				S state = losing.poll();

				if (losingStates.contains(state)) {
					continue;
				}

				losingStates.add(state);
				// TODO: review, I would like to understand better the use I am
				// giving to both losing and losingStates
				// even the name is misleading

				// a state will be losing if it has uncontrollable predecessors
				// but none leads to a non-losing state
				// or if it has only controllable predecessors since the system
				// can take the game to a env-losing state

				Set<S> predecessors = this.game.getPredecessors(state);
				for (S pred : predecessors) {
					if (!game.isUncontrollable(pred)) {
						losing.add(pred);
					} else {
						boolean atLeastOneUncontrollableWinning = false;
						for (S succ : this.game
								.getUncontrollableSuccessors(pred)) {
							if (!losing.contains(succ)
									&& !losingStates.contains(succ)
									&& succ != pred) {
								atLeastOneUncontrollableWinning = true;// what
																		// happens
																		// if
																		// succ
																		// ==
																		// pred?
								break;
							}
						}
						if (!atLeastOneUncontrollableWinning)
							losing.add(pred);
					}
				}
			}
		}
		this.gameSolved = true;
	}

	private void initialise(Collection<S> pending, Set<S> actualGoalStates) {
		for (S state : actualGoalStates) {
			pending.add(state);
		}
	}

	public Set<S> getWinningStates() {
		Set<S> winning = new HashSet<S>();
		if (!gameSolved) {
			this.solveGame();
		}
		for (S state : this.game.getStates()) {
			if (!this.losingStates.contains(state)) {
				winning.add(state);
			}
		}
		return winning;
	}

	public boolean isWinning(S state) {
		if (!gameSolved) {
			this.solveGame();
		}
		return !this.losingStates.contains(state);
	}

	// remove any transition to a winning state
	public Strategy<S, Integer> buildStrategy() {
		Strategy<S, Integer> result = new Strategy<S, Integer>();

		Set<S> winningStates = this.getWinningStates();

		for (S state : losingStates) {
			StrategyState<S, Integer> source = new StrategyState<S, Integer>(
					state, DUMMY_GOAL);
			Set<StrategyState<S, Integer>> successors = new HashSet<StrategyState<S, Integer>>();
			// if its uncontrollable and winning it must have winning succesors
			for (S succ : this.game.getSuccessors(state)) {
				if (!winningStates.contains(succ)) {
					StrategyState<S, Integer> target = new StrategyState<S, Integer>(
							succ, DUMMY_GOAL);
					successors.add(target);
				}
			}
			result.addSuccessors(source, successors);
		}
		return result;
	}

	public Strategy<S, Integer> buildEnvironmentStrategy() {
		Strategy<S, Integer> result = new Strategy<S, Integer>();

		Set<S> winningStates = this.getWinningStates();
		for (S state : winningStates) {
			StrategyState<S, Integer> source = new StrategyState<S, Integer>(
					state, DUMMY_GOAL);
			Set<StrategyState<S, Integer>> successors = new HashSet<StrategyState<S, Integer>>();
			// if its uncontrollable and winning it must have winning succesors
			if (this.game.isUncontrollable(state)) {
				for (S succ : this.game.getUncontrollableSuccessors(state)) {
					if (!this.losingStates.contains(succ)) {
						StrategyState<S, Integer> target = new StrategyState<S, Integer>(
								succ, DUMMY_GOAL);
						successors.add(target);
					}
				}
			} else { // Controllable State
				for (S succ : this.game.getControllableSuccessors(state)) {
					StrategyState<S, Integer> target = new StrategyState<S, Integer>(
							succ, DUMMY_GOAL);
					successors.add(target);
				}
			}
			result.addSuccessors(source, successors);
		}
		return result;
	}


}

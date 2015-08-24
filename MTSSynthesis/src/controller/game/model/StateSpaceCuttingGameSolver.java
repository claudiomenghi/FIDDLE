package controller.game.model;

import java.util.HashSet;
import java.util.Set;

import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.LTS;


public abstract class StateSpaceCuttingGameSolver<S, A> implements GameSolver<S,Integer> {

	protected LTS<S, A> originalEnvironment;
	protected Game<S> game;
	protected Set<S> losingStates;
	protected boolean gameSolved;
	protected Set<Set<S>> goalStates;

	
	public StateSpaceCuttingGameSolver(Game<S> game, Set<Set<S>> goalStates){
		this.game = game;
		losingStates = new HashSet<S>();
		gameSolved = false;
		this.goalStates = goalStates;
	}
	
	public Game<S> getGame(){
		return game;
	}
	

}


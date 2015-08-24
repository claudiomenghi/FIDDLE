package controller.model.gr;

import ac.ic.doc.mtstools.model.LTS;
import controller.game.gr.StrategyState;
import controller.game.util.GenericLTSStrategyStateToStateConverter;
import controller.model.GRGameControlProblem;
import controller.model.LazyGRControlProblem;

public class TransientLazyGRControlProblem<S,A, M> extends GRGameControlProblem<S, A, M>{
	
	LazyGRControlProblem<S, A> lazyGRControlProblem;
	
	public TransientLazyGRControlProblem(LTS<S, A> environment, GRControllerGoal<A> grControllerGoal){
		super(environment, grControllerGoal);
	}
	
	@Override
	protected LTS<S, A> primitiveSolve() {
		return new GenericLTSStrategyStateToStateConverter<S, A, Integer>().transform(rawSolve()); 
	}
	
	@Override
	public	LTS<StrategyState<S, Integer>, A>  rawSolve(){
		lazyGRControlProblem = new LazyGRControlProblem<S,A>(environment, grControllerGoal);
		LTS<S,A> env = lazyGRControlProblem.solve();
		TransientControlProblem<S,A,M> tcp = new TransientControlProblem<S,A,M>(env, grControllerGoal);
		LTS<StrategyState<S, Integer>, A> result = tcp.rawSolve();
		g = tcp.getGRGame();
		return result;
	}
}

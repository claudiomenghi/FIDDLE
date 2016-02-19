package MTSSynthesis.controller.model.gr;

import MTSTools.ac.ic.doc.mtstools.model.LTS;
import MTSSynthesis.controller.game.gr.StrategyState;
import MTSSynthesis.controller.game.util.GenericLTSStrategyStateToStateConverter;
import MTSSynthesis.controller.model.GRGameControlProblem;
import MTSSynthesis.controller.model.PerfectInfoGRControlProblem;

public class ConcurrencyGRControlProblem<S,A, M> extends GRGameControlProblem<S, A, M>{
	
	PerfectInfoGRControlProblem<S, A> perfectInfoGRControlProblem;

	public ConcurrencyGRControlProblem(LTS<S, A> environment, GRControllerGoal<A> grControllerGoal){
		super(environment, grControllerGoal);
	}
	
	@Override
	protected LTS<S, A> primitiveSolve() {
		return new GenericLTSStrategyStateToStateConverter<S, A, Integer>().transform(rawSolve()); 
	}
	
	@Override
	public	LTS<StrategyState<S, Integer>, A>  rawSolve(){
		perfectInfoGRControlProblem = new PerfectInfoGRControlProblem<S,A>(environment, grControllerGoal);
		LTS<S,A> env = perfectInfoGRControlProblem.solve();
		ConcurrencyControlProblem<S,A,M> ccp = new ConcurrencyControlProblem<S,A,M>(env, grControllerGoal);
		LTS<StrategyState<S, Integer>, A> result = ccp.rawSolve();
		g = ccp.getGRGame();
		return result;
	}
	
}

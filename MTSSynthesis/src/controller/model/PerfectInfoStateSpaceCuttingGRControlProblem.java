package controller.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import controller.game.gr.GRRankSystem;
import controller.game.gr.StrategyState;
import controller.game.gr.perfect.PerfectInfoGRGameSolver;
import controller.game.util.GRGameBuilder;
import controller.game.util.GameStrategyToLTSBuilder;
import controller.game.util.GenericLTSStrategyStateToStateConverter;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.concurrency.GRCGame;


public class PerfectInfoStateSpaceCuttingGRControlProblem<S,A> extends
	GRControlProblem<S, A, Integer>{

	protected List<String> stateSpaceCuttingControlProblems;
	protected PerfectInfoGRGameSolver<S> perfectInfoGRControlProblem;
	
	
	public PerfectInfoStateSpaceCuttingGRControlProblem(LTS<S,A> originalEnvironment, GRControllerGoal<A> grControllerGoal) {
		super(originalEnvironment, grControllerGoal);
		stateSpaceCuttingControlProblems = new ArrayList<String>();
	}
		
	public boolean containsStateSpaceCuttingControlProblem(String stateSpaceCuttingControlProblem){
		return stateSpaceCuttingControlProblems.contains(stateSpaceCuttingControlProblem);
	}
	
	public void addStateSpaceCuttingControlProblem(String stateSpaceCuttingControlProblem){
		if(!containsStateSpaceCuttingControlProblem(stateSpaceCuttingControlProblem))
			stateSpaceCuttingControlProblems.add(stateSpaceCuttingControlProblem);
	}
	
	public void removeStateSpaceCuttingControlProblem(String sateSpaceCuttingControlProblem){
		if(containsStateSpaceCuttingControlProblem(sateSpaceCuttingControlProblem))
			stateSpaceCuttingControlProblems.remove(sateSpaceCuttingControlProblem);
	}
	
	protected StateSpaceCuttingControlProblem<S, A> createStateSpaceCuttingControlProblem(
			String gameSolverName) throws NoSuchMethodException,
			SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		Class<?> clazz = Class.forName(gameSolverName);
		Constructor<?> constructor = clazz.getConstructor(LTS.class, GRControllerGoal.class);
		Set<S> failures = new HashSet<S>();
		StateSpaceCuttingControlProblem<S,A> instance = (StateSpaceCuttingControlProblem<S, A>) constructor.newInstance(environment, grControllerGoal);
		return instance;
	}
	
	@Override
	protected LTS<S,A> primitiveSolve() {
		//cut according to all the predefined game solvers
		for(String s: stateSpaceCuttingControlProblems){
			try {
				StateSpaceCuttingControlProblem<S, A> cuttingControlProblem = createStateSpaceCuttingControlProblem(s);
				cuttingControlProblem.solve();
				environment = cuttingControlProblem.cutOriginalStateSpace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

		GRCGame<S> perfectInfoGRGame = new GRGameBuilder<S,A>().buildGRCGameFrom(new MTSAdapter<S,A>(environment), grControllerGoal);
		GRRankSystem<S> grRankSystem = new GRRankSystem<S>(
				perfectInfoGRGame.getStates(), perfectInfoGRGame.getGoal().getGuarantees(),
				perfectInfoGRGame.getGoal().getAssumptions(), perfectInfoGRGame.getGoal().getFailures());
		perfectInfoGRControlProblem = new PerfectInfoGRGameSolver<S>(perfectInfoGRGame, grRankSystem);
		perfectInfoGRControlProblem.solveGame();
		LTS<StrategyState<S, Integer>, A> result = GameStrategyToLTSBuilder
				.getInstance().buildLTSFrom(environment,
						perfectInfoGRControlProblem.buildStrategy());
		return new GenericLTSStrategyStateToStateConverter<S, A, Integer>()
				.transform(result);		
	}	
	
}





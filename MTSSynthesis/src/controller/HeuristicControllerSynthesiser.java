package controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.MTS.TransitionType;
import ac.ic.doc.mtstools.model.impl.LTSAdapter;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import ac.ic.doc.mtstools.model.operations.ParallelComposer;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.language.Symbol;
import controller.game.gr.GRGameSolver;
import controller.game.gr.GRRankSystem;
import controller.game.gr.StrategyState;
import controller.game.gr.lazy.LazyGRGameSolver;
import controller.game.model.GameSolver;
import controller.game.model.Strategy;
import controller.game.util.GRGameBuilder;
import controller.game.util.GameStrategyToMTSBuilder;
import controller.gr.time.GR1toReachability;
import controller.gr.time.GenericChooser;
import controller.gr.time.LatencyNotPresetEvaluator;
import controller.gr.time.SchedulerGenerator;
import controller.gr.time.Translator;
import controller.gr.time.TranslatorPair;
import controller.gr.time.model.Activity;
import controller.gr.time.model.ActivityDefinitions;
import controller.model.GRGameControlProblem;
import controller.model.PerfectInfoGRControlProblem;
import controller.model.gr.ConcurrencyControlProblem;
import controller.model.gr.ConcurrencyGRControlProblem;
import controller.model.gr.ConcurrencyLazyGRControlProblem;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.GRGame;
import controller.model.gr.TransientControlProblem;
import controller.model.gr.TransientGRControlProblem;
import controller.model.gr.TransientLazyGRControlProblem;
import controller.model.gr.concurrency.GRCGame;

public class HeuristicControllerSynthesiser<S,A>{
	
	public MTS<StrategyState<S, Integer>, A> synthesiseGR(MTS<S, A> plant, GRControllerGoal<A> goal, MTS<S, A> env) {

		boolean CONCURRENCY_DEFINED = !goal.getConcurrencyFluents().isEmpty();
		GameSolver<S, Integer> solver;
		GRGameSolver<S> gSolver;
		
		int maxLazyness = goal.getLazyness();
		
		if(CONCURRENCY_DEFINED || goal.isNonTransient()){ 
			MTS<StrategyState<S, Integer>, A> result;
			LTS<S,A> safeEnvironment = new LTSAdapter<S,A>(plant,TransitionType.REQUIRED);
			LTS<S,A> realEnvironment = new LTSAdapter<S,A>(env,TransitionType.REQUIRED);
			
			GRGameControlProblem<S,A,Integer> controlProblem = null;
			
			if(CONCURRENCY_DEFINED){
				if(maxLazyness>0){
					controlProblem = new ConcurrencyLazyGRControlProblem<S,A,Integer>(safeEnvironment, goal);
					result = new MTSAdapter<StrategyState<S, Integer>, A>(controlProblem.rawSolve());
					return result;
				}else{
					controlProblem = new ConcurrencyGRControlProblem<S,A,Integer>(safeEnvironment, goal);
					result = new MTSAdapter<StrategyState<S, Integer>, A>(controlProblem.rawSolve());
				}
			}else{
				if(maxLazyness>0){
					controlProblem = new TransientLazyGRControlProblem<S,A,Integer>(safeEnvironment, goal);
					result = new MTSAdapter<StrategyState<S, Integer>, A>(controlProblem.rawSolve());
					return result;
				}else{
					controlProblem = new TransientGRControlProblem<S,A,Integer>(safeEnvironment, goal);
					result = new MTSAdapter<StrategyState<S, Integer>, A>(controlProblem.rawSolve());
				}
			}
			
			doTest(goal, safeEnvironment, realEnvironment, controlProblem);
			return result;
		}
		
		GRGame<S> nGame = new GRGameBuilder<S, A>().buildGRCGameFrom(plant, goal);
		GRRankSystem<S> nSystem = new GRRankSystem<S>(nGame.getStates(), nGame.getGoal().getGuarantees(), nGame.getGoal().getAssumptions(), nGame.getGoal().getFailures());
		gSolver = new LazyGRGameSolver<S>(nGame, nSystem, maxLazyness);
		solver = gSolver;
		
		solver.solveGame();
		
		if (solver.isWinning(plant.getInitialState())) {

			MTS<StrategyState<S, Integer>, A> maximalControllerUsingGR1 = getResult(plant, gSolver, gSolver);
			return maximalControllerUsingGR1;
			
		} else {
			return null;
		}
	}
	
	private void doTest(GRControllerGoal<A> goal, LTS<S, A> safeEnvironment, LTS<S, A> realEnvironment, GRGameControlProblem<S, A, Integer> controlProblem) {
		PerfectInfoGRControlProblem<S, A> perfectControlProblem = new PerfectInfoGRControlProblem<S, A>(safeEnvironment, goal);
		
		LTS<S,A> heuristicSolution =  controlProblem.solve();
		LTS<S,A> perfectSolution = perfectControlProblem.solve();
		
		Set<S> heuristicFinalStates = controlProblem.getGRGame().getGoal().getGuarantee(1).getStateSet();
		Set<S> perfectFinalStates = perfectControlProblem.getGRGame().getGoal().getGuarantee(1).getStateSet();
		
		compareControllers(goal, realEnvironment, heuristicSolution, perfectSolution, heuristicFinalStates, perfectFinalStates);
	}


	public MTS<S, A> applyHeuristics(MTS<S, A> controller, MTS<S, A> env, GRControllerGoal<A> goal) {
		LTS<S,A> realEnvironment = new LTSAdapter<S,A>(env,TransitionType.REQUIRED);
		LTS<S,A> perfectSolution =  new LTSAdapter<S,A>(controller, TransitionType.REQUIRED);
		LTS<S,A> heuristicSolution = null;
		GRGameControlProblem<S,A,Integer> cp ;
		
		if(goal.isNonTransient()){
			cp = new TransientControlProblem<S, A, Integer>(perfectSolution, goal);
		}else{
			cp = new ConcurrencyControlProblem<S, A, Integer>(perfectSolution, goal);
		}
		
		heuristicSolution =  cp.solve();
		
		if(goal.isTestLatency()){
			compareControllers(goal, realEnvironment, heuristicSolution, perfectSolution);
		}
		if(goal.isReachability()){
			GR1toReachability.transform(heuristicSolution, cp.getGRGame().getGoal().getGuarantee(1).getStateSet());
		}
		
		return new MTSAdapter<S,A>(heuristicSolution);
	}
	
	public MTS<S, A> applyReachabilityPrunning(MTS<S, A> controller, GRControllerGoal<A> goal) {
		GRCGame<S> cgame = new GRGameBuilder<S, A>().buildGRCGameFrom(controller, goal);
		GR1toReachability.transform(new LTSAdapter<S,A>(controller, TransitionType.REQUIRED), cgame.getGoal().getGuarantee(1).getStateSet());
		return controller;
	}

	
	
	private void compareControllers(GRControllerGoal<A> goal,
			LTS<S, A> realEnvironment, LTS<S, A> heuristicSolution,
			LTS<S, A> perfectSolution){
		Set<S> perfectFinalStates = getFinalStates(goal, perfectSolution);
		Set<S> heuristicFinalStates = getFinalStates(goal, heuristicSolution);
		compareControllers(goal, realEnvironment, heuristicSolution, perfectSolution, heuristicFinalStates, perfectFinalStates);
	}


	private Set<S> getFinalStates(GRControllerGoal<A> goal, LTS<S, A> heuristicSolution) {
		GRGame<S> heuristicGRGame = new GRGameBuilder<S,A>().buildGRGameFrom(new MTSAdapter<S,A>(heuristicSolution), goal);
		Set<S> heuristicFinalStates = heuristicGRGame.getGoal().getGuarantee(1).getStateSet();
		return heuristicFinalStates;
	}

	private void compareControllers(GRControllerGoal<A> goal,
			LTS<S, A> realEnvironment, LTS<S, A> heuristicSolution,
			LTS<S, A> perfectSolution, Set<S> heuristicFinalStates,
			Set<S> perfectFinalStates) {
		
		LTS<Pair<S, S>, A> heuristicComposition = transformToReachability1(realEnvironment, heuristicSolution, heuristicFinalStates);
		LTS<Pair<S, S>, A> perfectComposition = transformToReachability1(realEnvironment, perfectSolution, perfectFinalStates);
		
		Set<A> controllableActions = goal.getControllableActions();
		
		ActivityDefinitions<A> activityDefinitions = getActivityDefinition(goal.getActivityFluents());
		
		
		Translator<S,Pair<S,S>> translator = new TranslatorPair<S,S>();
		
		Set<Pair<S, S>> heuristicComposedFinalStates = compositionFinalStates(heuristicFinalStates, heuristicComposition.getStates());
		Set<Pair<S, S>> perfectComposedFinalStates = compositionFinalStates(perfectFinalStates,perfectComposition.getStates());
		
		pruneRealEnvironment(perfectComposition,realEnvironment);

//		Set<GenericChooser<S, A, Pair<S, S>>> schedulers = generateSchedulers(goal.getMaxSchedulers()-1,realEnvironment,controllableActions,activityDefinitions);
//		LatencyPresetEvaluator<Pair<S,S>,A,S> evaluator = new LatencyPresetEvaluator<Pair<S,S>,A,S>(
//								new MTSAdapter<Pair<S,S>,A>(heuristicComposition),
//								new MTSAdapter<Pair<S,S>,A>(perfectComposition),
//								heuristicComposedFinalStates, perfectComposedFinalStates,
//								activityDefinitions, translator,
//						        goal.getControllableActions(), 
//						        schedulers, goal.getMaxSchedulers()-1);
		LatencyNotPresetEvaluator<S,A> evaluator = new LatencyNotPresetEvaluator<S,A>(
				new MTSAdapter<Pair<S,S>,A>(heuristicComposition),
				new MTSAdapter<Pair<S,S>,A>(perfectComposition),
				heuristicComposedFinalStates, perfectComposedFinalStates,
				activityDefinitions, translator,
		        controllableActions, 
		        realEnvironment,goal.getMaxSchedulers()-1);
		evaluator.evaluateLatency(goal.getMaxControllers()-1);
	}
	
	private void pruneRealEnvironment(LTS<Pair<S, S>, A> perfectComposition, LTS<S, A> realEnvironment) {
		Map<S,Set<Pair<A,S>>> transitionsToRemove = new HashMap<S,Set<Pair<A,S>>>();
		Set<S> finalStates = new HashSet<S>();
		Set<S> notFinalStates = new HashSet<S>();
		for (S state : realEnvironment.getStates()) {
			Set<A> enabled = new HashSet<A>();
			for (Pair<S,S> p : perfectComposition.getStates()) {
				if(p.getFirst().equals(state)){
					if(perfectComposition.getTransitions(p).isEmpty()){
						finalStates.add(state);
					}else{
						notFinalStates.add(state);
						for(Pair<A,Pair<S,S>> t :perfectComposition.getTransitions(p)){
							enabled.add(t.getFirst());
						}
					}
				}
			}
			Set<Pair<A,S>> toRemove = new HashSet<Pair<A,S>>();
			for(Pair<A,S> t : realEnvironment.getTransitions(state)){
				if(!enabled.contains(t.getFirst())){
					toRemove.add(t);
				}
			}
			transitionsToRemove.put(state, toRemove);
		}
		for (S s : transitionsToRemove.keySet()) {
			for (Pair<A,S> t : transitionsToRemove.get(s)) {
				realEnvironment.removeTransition(s, t.getFirst(), t.getSecond());
			}
		}
		for (S s : finalStates) {
			if(notFinalStates.contains(s)){
//				System.out.println("End and something more");
			}else{
//				System.out.println("Prunning something..");
				for(Pair<A,S> transition: realEnvironment.getTransitions(s)){
					realEnvironment.removeTransition(s, transition.getFirst(), transition.getSecond());
				}
			}
		}
		realEnvironment.removeUnreachableStates();
	}

	private Set<GenericChooser<S, A, Pair<S, S>>> generateSchedulers(Integer maxSchedulers, LTS<S, A> realEnvironment, Set<A> controllableActions, ActivityDefinitions<A> activityDefinitions) {
		SchedulerGenerator<S, A> schedulerGenerator = new SchedulerGenerator<S,A>(realEnvironment, controllableActions, activityDefinitions);
		System.out.println("Estimation: " + schedulerGenerator.getEstimation());
		
		GenericChooser<S, A, Pair<S,S>> scheduler = schedulerGenerator.next();
		int i = 0;
		while(scheduler!=null && i< maxSchedulers){
			scheduler = schedulerGenerator.next();
			i++;
		}
		return schedulerGenerator.getGenerated();
	}


	private LTS<Pair<S, S>, A> transformToReachability1(
			LTS<S, A> realEnvironment, LTS<S, A> perfectSolution,
			Set<S> perfectFinalStates) {
		GR1toReachability.transform(perfectSolution,perfectFinalStates);
		ParallelComposer<S, A, S> perfectComposer = new ParallelComposer<S, A, S>(realEnvironment, perfectSolution);
		LTS<Pair<S,S>,A> perfectComposition = perfectComposer.compose();
		return perfectComposition;
	}
	
	
	
	private Set<Pair<S, S>> compositionFinalStates(Set<S> originalFinalStates, Set<Pair<S, S>> compositionStates) {
		Set<Pair<S,S>> heuristicComposedFinalStates = new HashSet<Pair<S,S>>();
		for (S s : originalFinalStates) {
			for (Pair<S,S> p : compositionStates) {
				if(p.getSecond().equals(s)){
					heuristicComposedFinalStates.add(p);
				}
			}
		}
		return heuristicComposedFinalStates;
	}

	private MTS<StrategyState<S, Integer>, A> getResult(MTS<S, A> plant,
			GameSolver<S, Integer> gameSolver,
			GRGameSolver<S> grSolver) {
		Strategy<S,Integer> strategy = gameSolver.buildStrategy();
		
		Set<Pair<StrategyState<S, Integer>, StrategyState<S, Integer>>> worseRank = grSolver.getWorseRank();
		
		int lazyness = 0;
		try {
			lazyness = ((LazyGRGameSolver<S>) grSolver).getMaxLazyness();
		} catch (Exception e) {
			System.out.println("This model doesn't have lazyness.");
		}
		
		MTS<StrategyState<S, Integer>, A> result = GameStrategyToMTSBuilder.getInstance().buildMTSFrom(plant, strategy, worseRank, lazyness);
		return result;
	}

	@SuppressWarnings("unchecked")
	private ActivityDefinitions<A> getActivityDefinition(Set<Fluent> activityFluents) {
		Set<Activity<A>> activities = new HashSet<Activity<A>>();
		for (Fluent fluent : activityFluents) {
			Set<A> initiatingActions = new HashSet<A>();
			for (Symbol a : fluent.getInitiatingActions()) {
				initiatingActions.add((A) a.toString());
			}
			Set<A> terminatingActions = new HashSet<A>();
			for (Symbol a : fluent.getTerminatingActions()) {
				terminatingActions.add((A) a.toString());
			}
			activities.add(new Activity<A>(fluent.getName(),initiatingActions,terminatingActions)); 
		}
		return new ActivityDefinitions<A>(activities);
	}
	
}

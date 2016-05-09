package MTSSynthesis.controller;

import MTSSynthesis.ar.dc.uba.model.condition.Fluent;
import MTSSynthesis.ar.dc.uba.model.language.Symbol;
import MTSSynthesis.controller.game.gr.GRGameSolver;
import MTSSynthesis.controller.game.gr.StrategyState;
import MTSSynthesis.controller.game.gr.lazy.LazyGRGameSolver;
import MTSSynthesis.controller.game.model.GameSolver;
import MTSSynthesis.controller.game.model.Strategy;
import MTSSynthesis.controller.game.util.GRGameBuilder;
import MTSSynthesis.controller.game.util.GameStrategyToMTSBuilder;
import MTSSynthesis.controller.gr.time.*;
import MTSSynthesis.controller.gr.time.model.Activity;
import MTSSynthesis.controller.gr.time.model.ActivityDefinitions;
import MTSSynthesis.controller.model.GRGameControlProblem;
import MTSSynthesis.controller.model.gr.ConcurrencyControlProblem;
import MTSSynthesis.controller.model.gr.GRControllerGoal;
import MTSSynthesis.controller.model.gr.GRGame;
import MTSSynthesis.controller.model.gr.TransientControlProblem;
import MTSSynthesis.controller.model.gr.concurrency.GRCGame;
import MTSTools.ac.ic.doc.commons.relations.Pair;
import MTSTools.ac.ic.doc.mtstools.model.LTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS.TransitionType;
import MTSTools.ac.ic.doc.mtstools.model.impl.LTSAdapter;
import MTSTools.ac.ic.doc.mtstools.model.impl.MTSAdapter;
import MTSTools.ac.ic.doc.mtstools.model.operations.ParallelComposer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HeuristicControllerSynthesiser<S,A>{

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

		//@ezecastellano: Uncomment this if you want to use the same set of schedulers from every pair of controllers.
		//Set<GenericChooser<S, A, Pair<S, S>>> schedulers = generateSchedulers(goal.getMaxSchedulers()-1,realEnvironment,controllableActions,activityDefinitions);
		//LatencyPresetEvaluator<Pair<S,S>,A,S> evaluator = new LatencyPresetEvaluator<Pair<S,S>,A,S>(
		//new MTSAdapter<Pair<S,S>,A>(heuristicComposition),
		//new MTSAdapter<Pair<S,S>,A>(perfectComposition),
		//heuristicComposedFinalStates, perfectComposedFinalStates,
		//activityDefinitions, translator,
		//goal.getControllableActions(),
		//schedulers, goal.getMaxSchedulers()-1);
		LatencyNotPresetEvaluator<S,A> evaluator = new LatencyNotPresetEvaluator<S,A>(
				new MTSAdapter<Pair<S,S>,A>(heuristicComposition),
				new MTSAdapter<Pair<S,S>,A>(perfectComposition),
				heuristicComposedFinalStates, perfectComposedFinalStates,
				activityDefinitions, translator,
		        controllableActions, 
		        realEnvironment,goal.getMaxSchedulers()-1);
		evaluator.evaluateLatency(goal.getMaxControllers()-1);
	}

//	private Set<GenericChooser<S, A, Pair<S, S>>> generateSchedulers(Integer maxSchedulers, LTS<S, A> realEnvironment, Set<A> controllableActions, ActivityDefinitions<A> activityDefinitions) {
//		SchedulerGenerator<S, A> schedulerGenerator = new SchedulerGenerator<S,A>(realEnvironment, controllableActions, activityDefinitions);
//		System.out.println("Estimation: " + schedulerGenerator.getEstimation());
//
//		GenericChooser<S, A, Pair<S,S>> scheduler = schedulerGenerator.next();
//		int i = 0;
//		while(scheduler!=null && i< maxSchedulers){
//			scheduler = schedulerGenerator.next();
//			i++;
//		}
//		return schedulerGenerator.getGenerated();
//	}

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
		realEnvironment.removeUnreachableStates();}




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

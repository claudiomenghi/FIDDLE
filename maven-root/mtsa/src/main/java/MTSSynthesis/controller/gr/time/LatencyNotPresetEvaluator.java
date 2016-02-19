package MTSSynthesis.controller.gr.time;

import java.util.HashSet;
import java.util.Set;

import MTSTools.ac.ic.doc.commons.relations.Pair;
import MTSTools.ac.ic.doc.mtstools.model.LTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS.TransitionType;
import MTSTools.ac.ic.doc.mtstools.model.impl.LTSAdapter;
import MTSTools.ac.ic.doc.mtstools.model.impl.LTSImpl;

import com.microsoft.z3.Z3Exception;

import MTSSynthesis.controller.gr.time.comparator.ControllerComparator;
import MTSSynthesis.controller.gr.time.comparator.ControllerPairsComparator;
import MTSSynthesis.controller.gr.time.model.ActivityDefinitions;
import MTSSynthesis.controller.gr.time.model.ComparatorPool;

public class LatencyNotPresetEvaluator<S,A> extends LatencyEvaluator<Pair<S,S>,A,S> {
	
	LTS<S,A> realEnvironment;
	LTS<S,A> prunedEnvironment;
	SchedulerGenerator<S, A> schedulerGenerator;
	public LatencyNotPresetEvaluator(MTS<Pair<S,S>, A> heuristic, MTS<Pair<S,S>, A> maximalControllerUsingGR1,Set<Pair<S,S>> heuristicFinalStates, Set<Pair<S,S>> perfectFinalStates, ActivityDefinitions<A> activityDefinitions, Translator<S, Pair<S,S>> translator, Set<A> controllableAction, LTS<S,A> realEnvironment, Integer maxSchedulers) {
		super(heuristic, maximalControllerUsingGR1, heuristicFinalStates, perfectFinalStates, activityDefinitions, translator, controllableAction, maxSchedulers, realEnvironment);
	}

	protected GenericChooser<S, A, Pair<S,S>> getScheduler(){
		return schedulerGenerator.next();
	}
	
	
	private Set<GenericChooser<S, A, Pair<S, S>>> generateSchedulers(Integer maxSchedulers, LTS<S, A> environment, Set<A> controllableActions, ActivityDefinitions<A> activityDefinitions) {
		SchedulerGenerator<S, A> schedulerGenerator = new SchedulerGenerator<S,A>(environment, controllableActions, activityDefinitions);
		System.out.println("Estimation: " + schedulerGenerator.getEstimation());
		GenericChooser<S, A, Pair<S,S>> scheduler = schedulerGenerator.next();
		int i = 0;
		while(scheduler!=null && i< maxSchedulers){
			scheduler = schedulerGenerator.next();
			i++;
		}
		return schedulerGenerator.getGenerated();
	}

	@Override
	protected Set<GenericChooser<S, A, Pair<S, S>>> getSchedulers(MTS<Pair<S, S>, A> result1, MTS<Pair<S, S>, A> result2) {
		LTS<S,A> prunedEnvironment = pruneRealEnvironment(new LTSAdapter<Pair<S,S>,A>(result1,TransitionType.REQUIRED), new LTSAdapter<Pair<S,S>,A>(result2,TransitionType.REQUIRED), realEnvironment);
		return generateSchedulers(maxSchedulers,prunedEnvironment,controllableActions,activityDefinitions);
	}

	@Override
	protected void preCalculateGamaForHeuristic() throws Z3Exception {
		
	}
	
	private LTS<S,A> pruneRealEnvironment(LTS<Pair<S, S>, A> result1, LTS<Pair<S, S>, A> result2, LTS<S, A> realEnvironment) {
		LTS<S,A> lts = new LTSImpl<S, A>(realEnvironment.getInitialState());
		lts.addStates(realEnvironment.getStates());
		lts.addActions(realEnvironment.getActions());
		for (S state : realEnvironment.getStates()) {
			Set<A> enabled = new HashSet<A>();
			enabledFrom(result1, state, enabled);
			enabledFrom(result2, state, enabled);
			for(Pair<A,S> t : realEnvironment.getTransitions(state)){
				if(enabled.contains(t.getFirst())){
					lts.addTransition(state, t.getFirst(), t.getSecond());
				}
			}
		}
		lts.removeUnreachableStates();
		return lts;
	}

	private void enabledFrom(LTS<Pair<S, S>, A> result2, S state, Set<A> enabled) {
		for (Pair<S,S> p : result2.getStates()) {
			if(p.getFirst().equals(state)){
				for(Pair<A,Pair<S,S>> t :result2.getTransitions(p)){
					enabled.add(t.getFirst());
				}
			}
		}
	}

	@Override
	protected ControllerComparator<S, A, Pair<S, S>> getControllerComparator(int i,Set<ControllerChooser<Pair<S, S>, A>> controllerChooser, ResultCounter stats,
			MTS<Pair<S, S>, A> maximalController, Set<Pair<S, S>> perfectFinalStates,
			MTS<Pair<S, S>, A> result2, Set<Pair<S, S>> heuristicFinalStates,
			Integer maxSchedulers, Set<A> controllableActions,
			ActivityDefinitions<A> activityDefinitions,
			LTS<S, A> realEnvironment, Translator<S, Pair<S, S>> translator, ComparatorPool<A, Pair<S,S>> comparator, Integer maxThreads) {
		return  new ControllerPairsComparator<S,A>(i, stats, controllerChooser, maximalController, perfectFinalStates, result2, heuristicFinalStates,maxSchedulers, controllableActions, activityDefinitions,realEnvironment, translator, comparator, maxThreads);
	}

}
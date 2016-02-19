package MTSSynthesis.controller.gr.time.comparator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import MTSTools.ac.ic.doc.commons.relations.Pair;
import MTSTools.ac.ic.doc.mtstools.model.LTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS.TransitionType;
import MTSTools.ac.ic.doc.mtstools.model.impl.LTSAdapter;
import MTSTools.ac.ic.doc.mtstools.model.impl.LTSImpl;

import com.microsoft.z3.Z3Exception;

import MTSSynthesis.controller.gr.time.ControllerChooser;
import MTSSynthesis.controller.gr.time.GenericChooser;
import MTSSynthesis.controller.gr.time.ResultCounter;
import MTSSynthesis.controller.gr.time.SchedulerGeneratorLight;
import MTSSynthesis.controller.gr.time.SkeletonBuilder;
import MTSSynthesis.controller.gr.time.Translator;
import MTSSynthesis.controller.gr.time.model.ActivityDefinitions;
import MTSSynthesis.controller.gr.time.model.ComparatorPool;
import MTSSynthesis.controller.gr.time.model.Result;

public class ControllerPairsComparator<S,A> extends ControllerComparator<S, A, Pair<S,S>> {
	SchedulerGeneratorLight<S, A> schedulerGenerator;
	
	public ControllerPairsComparator(final Integer id, ResultCounter stats,Set<ControllerChooser<Pair<S, S>, A>> controllerChooser, final MTS<Pair<S, S>, A> maximalController,
			final Set<Pair<S, S>> perfectFinalStates, final MTS<Pair<S, S>, A> result2,
			final Set<Pair<S, S>> heuristicFinalStates, final Integer maxSchedulers,
			final Set<A> controllableActions,
			final ActivityDefinitions<A> activityDefinitions,
			final LTS<S, A> realEnvironment, final Translator<S, Pair<S, S>> translator, ComparatorPool<A, Pair<S,S>> comparatorPool, Integer maxThreads) {
		super(id, stats, controllerChooser, maximalController, perfectFinalStates, result2, heuristicFinalStates,
				maxSchedulers, controllableActions, activityDefinitions,
				realEnvironment, translator, comparatorPool, maxThreads);
	}
	
	@Override
	protected Result compare() throws Z3Exception {
		LTS<S,A> prunedEnvironment = pruneRealEnvironment(new LTSAdapter<Pair<S, S>,A>(result1,TransitionType.REQUIRED), new LTSAdapter<Pair<S, S>,A>(result2,TransitionType.REQUIRED), realEnvironment);
		this.schedulerGenerator = new SchedulerGeneratorLight<S,A>(prunedEnvironment, controllableActions, activityDefinitions);
		return super.compare();
	}
	
	protected LTS<S,A> pruneRealEnvironment(LTS<Pair<S, S>, A> result1, LTS<Pair<S, S>, A> result2, LTS<S, A> realEnvironment) {
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
	protected GenericChooser<S, A, Pair<S, S>> getScheduler() 	
	{
		Map<S,Integer> tmp = schedulerGenerator.next();
		if(tmp != null)
			return 	schedulerGenerator.build(tmp);
		return null;
	}

	@Override
	protected Set<Map<S,Integer>> getSkeletons(int cant) {
		return schedulerGenerator.next(cant);
	}

	@Override
	protected Set<Map<S, Integer>> getLastSkeletons() {
		return schedulerGenerator.getLasts();
	}

	@Override
	protected SkeletonBuilder<S, A, Pair<S, S>> getSkeletonBuilder() {
		return schedulerGenerator;
	}


}

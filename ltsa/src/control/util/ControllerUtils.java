package control.util;

import static ac.ic.doc.mtstools.model.MTS.TransitionType.MAYBE;
import static ac.ic.doc.mtstools.model.MTS.TransitionType.POSSIBLE;
import static ac.ic.doc.mtstools.model.MTS.TransitionType.REQUIRED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import lts.CompactState;
import lts.CompositeState;
import lts.LTSOutput;
import ui.StandardOutput;
import ac.ic.doc.commons.collections.PowerSet;
import ac.ic.doc.commons.relations.BinaryRelation;
import ac.ic.doc.commons.relations.BinaryRelationUtils;
import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.impl.LTSImpl;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import ac.ic.doc.mtstools.model.impl.MTSImpl;
import ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.language.Symbol;

import com.google.common.collect.Sets;

import controller.model.ControllerGoal;

//TODO Move to MTSSynthesis and make it generic
public class ControllerUtils {

	public static MTS<Long, String> getSynchronous(MTS<Long, String> mts, Set<String> controlledActions,
			Long envState, Long contState, String envYields, String contYields) {
		
		LTS<Long, String> sync = new LTSImpl<Long, String>(envState);
		sync.addActions(mts.getActions());
		sync.addState(contState);
		sync.addAction(contYields);
		sync.addAction(envYields);
		sync.addTransition(envState, envYields, contState);
		sync.addTransition(contState, contYields, envState);
		for (String action : mts.getActions()) {
			if (controlledActions.contains(action)) {
				sync.addTransition(contState, action, contState);
			} else {
				sync.addTransition(envState, action, envState);
			}
		}
//this is how this method must be once the composition is made generic. 
	//	public static <State, Action> void makeSynchronous(MTS<State, Action> mts, Set<Action> controlledActions,
//			State envState, State contState, Action envYields, Action contYields) {
//		
//		LTS<State, Action> sync = new LTSImpl<State, Action>(envState, "Sync");
//		sync.addState(contState);
//		sync.addAction(contYields);
//		sync.addAction(envYields);
//		sync.addTransition(envState, envYields, contState);
//		sync.addTransition(contState, contYields, envState);
//		for (Action action : mts.getActions()) {
//			if (controlledActions.contains(action)) {
//				sync.addTransition(contState, action, contState);
//			} else {
//				sync.addTransition(envState, action, envState);
//			}
//		}

		MTSToAutomataConverter instance = MTSToAutomataConverter.getInstance();
		Vector<CompactState> machinesToCompose = new Vector<CompactState>();
		
		machinesToCompose.add(instance.convert(mts, " "));
		machinesToCompose.add(instance.convert(new MTSAdapter<Long, String>(sync), " "));
		
		CompositeState c = new CompositeState(machinesToCompose);
		c.compose(new StandardOutput());
		
		return AutomataToMTSConverter.getInstance().convert(c.composition);
		
		
	}
	
	public static MTS<Long, String> removeTopStates(MTS<Long, String> mts, Collection<Fluent> fluents) {
		
		MTSToAutomataConverter instance = MTSToAutomataConverter.getInstance();
		Vector<CompactState> machinesToCompose = new Vector<CompactState>();
		machinesToCompose.add(instance.convert(mts, " "));
		Set<String> mtsActions = mts.getActions();
		
		List<MTS<Long, String>> toCompose = new ArrayList<MTS<Long, String>>();
		toCompose.add(mts);
		
		
		for (Fluent fluent : fluents) {
			MTS<Long, String> modelFrom = getModelFrom(fluent);
			Set<String> fluentActions = modelFrom.getActions();
			if (!mtsActions.containsAll(fluentActions)){
				for (String actionInFluent : fluentActions) {
					if (!mtsActions.contains(actionInFluent)){
						Logger.getAnonymousLogger().warning("While doing 'removeTopStates'. Action: " + actionInFluent + " is not in MTS alphabet" );
					}
				}
			}
			machinesToCompose.add(instance.convert(modelFrom, fluent.getName()));
		}
		
		CompositeState c = new CompositeState(machinesToCompose);
		c.compose(new StandardOutput());
		return AutomataToMTSConverter.getInstance().convert(c.composition);
		
//		CompositionRuleApplier compositionRuleApplier = new CompositionRuleApplier();
//		MTS<Long, String> result = new MTSMultipleComposer<Long, String>(compositionRuleApplier).compose(toCompose);
		
//		return result;
		
	}

	public static MTS<Long, String> getModelFrom(Fluent fluent) {
		Long trueState, falseState;
		trueState = 0L;
		falseState = 1L;
		MTS<Long, String> mtsFromFluent;
		if (fluent.isInitialValue()) {
			mtsFromFluent = new MTSImpl<Long, String>(trueState);
			mtsFromFluent.addState(falseState);
		} else {
			mtsFromFluent = new MTSImpl<Long, String>(falseState);
			mtsFromFluent.addState(trueState);
		}

		
		for (Symbol symbol : fluent.getInitiatingActions()) {
			String symbolStr = symbol.toString();
			mtsFromFluent.addAction(symbolStr);
			mtsFromFluent.addRequired(falseState, symbolStr, trueState);
			mtsFromFluent.addRequired(trueState, symbolStr, trueState);
		}	
		for (Symbol symbol : fluent.getTerminatingActions()) {
			String symbolStr = symbol.toString();
			mtsFromFluent.addAction(symbolStr);
			mtsFromFluent.addRequired(trueState, symbolStr, falseState);
			mtsFromFluent.addRequired(falseState, symbolStr, falseState);
		}
		return mtsFromFluent;
	}

	/**
	 * Generates, a domain model as described in [FSE2010].
	 * Uses <i>output</i> to log the process. 
	 * 
	 * @param mts This parameter should have no maybe transitions.
	 * @param goal
	 * @param output
	 * @return
	 */
	//TODO make this method work for MTS as well. 
	public static MTS<Long, String> embedFluents(MTS<Long, String> mts, ControllerGoal<String> goal, LTSOutput output) {
		output.outln("Environment states: " + mts.getStates().size());
		MTS<Long, String> result = ControllerUtils.removeTopStates(mts, goal.getFluents());
		output.outln("Game states: " + result.getStates().size());
		return result;
	}
	
	//to be refactored. It needs to be generalised as a strategy together with the subset construction for MTS Control. 
	public static MTS<Pair<Long, Set<String>>, String> getSubSetConstructionFor(MTS<Long, String> mts, Set<String> controllable, LTSOutput output) {
		MTS<Pair<Long, Set<String>>, String> result = new MTSImpl<Pair<Long, Set<String>>, String>(Pair.create(mts.getInitialState(), (Set<String>)null));
		result.addActions(mts.getActions());
		
		Set<String> uncontrollable = new HashSet<String>(Sets.difference(mts.getActions(), controllable));
		
		for (Long state : mts.getStates()) {
			result.addState(Pair.create(state, (Set<String>)null));
		}
		
//		Adds transitions
		for (Long next : mts.getStates()) {
			if(!mts.getTransitions(next, POSSIBLE).isEmpty()){
				Pair<Long, Set<String>> state = Pair.create(next, (Set<String>)null);
				
				PowerSet<String> ps = new PowerSet<String>(controllable);
				for (Set<String> enabled : ps) {
					String action = enabled.toString();
					result.addAction(action);
					Pair<Long, Set<String>> to = Pair.create(next, enabled);
					result.addState(to);
					result.addRequired(state, action, to);

					for (Pair<String, Long> tr : mts.getTransitions(next, POSSIBLE)) {
						if (enabled.contains(tr.getFirst()) || uncontrollable.contains(tr.getFirst())) {
							result.addRequired(to, tr.getFirst(), Pair.create(tr.getSecond(), (Set<String>)null));
						}
					}
				}
			}
		}
		result.removeUnreachableStates();
		output.outln("Game Structure: " + result.getStates().size());
		output.outln("Subset construction states: " + result.getStates().size());
		return result;
	}
	
	public static MTS<Pair<Long, Set<String>>, String> generateStarredEnvModel(MTS<Long, String> mts) {
		
		MTS<Pair<Long, Set<String>>, String> result = new MTSImpl<Pair<Long, Set<String>>, String>(Pair.create(mts.getInitialState(), (Set<String>)null));
		result.addActions(mts.getActions());
		
		for (Long next : mts.getStates()) {
			result.addState(Pair.create(next, (Set<String>)null));
		}
		
//		Adds transitions
		for (Long next : mts.getStates()) {
			if(!mts.getTransitions(next, POSSIBLE).isEmpty()){
				Pair<Long, Set<String>> state = Pair.create(next, (Set<String>)null);
				
				BinaryRelation<String, Long> maybes = mts.getTransitions(next, MAYBE);
				BinaryRelation<String, Long> reqs = mts.getTransitions(next, REQUIRED);
				
				PowerSet<String> ps = new PowerSet<String>(BinaryRelationUtils.getDomain(maybes));
				for (Set<String> set : ps) {
					if (reqs.isEmpty() && set.isEmpty()) continue;
					String action = set.toString();
//					if (action.contains("[")) {
//						action = action.replace("[", "_");
//					}
//					if (action.contains("]")) {
//						action = action.replace("]", "");
//					}
					
					result.addAction(action);
					Pair<Long, Set<String>> to = Pair.create(next, set);
					result.addState(to);
					result.addRequired(state, action, to);

					for (Pair<String, Long> tr : reqs) {
						result.addRequired(to, tr.getFirst(), Pair.create(tr.getSecond(), (Set<String>)null));
					}
					for (Pair<String, Long> tr : maybes) {
						if (set.contains(tr.getFirst())) {
							result.addRequired(to, tr.getFirst(), Pair.create(tr.getSecond(), (Set<String>)null));
						}
					}
				}
			}
		}
		result.removeUnreachableStates();
		return result;
	}
	
	//to be refactored. It needs to be generalised as a strategy together with the subset construction for MTS Control. 
		public static MTS<Pair<Set<Long>, String>, String> getControllableDeterminisationFor(MTS<Long, String> mts, Set<String> controllable, LTSOutput output) {
			Pair<Set<Long>, String> initialSet = Pair.create(Collections.singleton(mts.getInitialState()), (String)null);
			MTS<Pair<Set<Long>, String>, String> result = new MTSImpl<Pair<Set<Long>, String>, String>(initialSet);
			
			result.addActions(mts.getActions());
			result.addAction("-1");
			
			Set<String> uncontrollable = new HashSet<String>(Sets.difference(mts.getActions(), controllable));
			
			Set<Pair<Set<Long>, String>> visited = new HashSet<Pair<Set<Long>, String>>();
			Queue<Pair<Set<Long>, String>> pending = new LinkedList<Pair<Set<Long>, String>>();
			pending.add(initialSet);
			
			while (!pending.isEmpty()) {
				Pair<Set<Long>, String> current = pending.poll();
				visited.add(current);
				
				Set<Long> mixedStates = new HashSet<Long>();
				
				for (Long smallState : current.getFirst()) { 
					for (String action : uncontrollable) {
						Set<Long> successors = mts.getTransitions(smallState, POSSIBLE).getImage(action);
						if (!successors.isEmpty()) {
							mixedStates.add(smallState);
						}
					}
				}
				
				
				for (String action : uncontrollable) {
					Set<Long> successors = new HashSet<Long>();
					boolean enabled = false;
					for (Long smallState : mixedStates) {
						Set<Long> image = mts.getTransitions(smallState, POSSIBLE).getImage(action);
						if (!image.isEmpty()) {
							enabled = true;
						}
						successors.addAll(image);
					}
					if (enabled) {
						Pair<Set<Long>, String> successor = Pair.create(successors, (String)null);
						result.addState(successor);
						result.addRequired(current, action, successor);
						if (!visited.contains(successor)) {
							pending.add(successor);
						}
					}
				}
				
				Pair<Set<Long>, String> newActual = current;
				if (current.getFirst().size()!=mixedStates.size()) {
					//is either pure controllable or deadlock
					Pair<Set<Long>, String> actualBis = Pair.create(current.getFirst(), "-1");
					result.addState(actualBis);
					result.addRequired(current, "-1", actualBis);
					newActual = actualBis;
				}
				
				for (String action : controllable) {
					boolean enabled = false;
					Set<Long> successors = new HashSet<Long>();
					for (Long smallState : current.getFirst()) {
						Set<Long> image = mts.getTransitions(smallState, POSSIBLE).getImage(action);
						if (image.isEmpty()) {
							enabled = false;
							break;
						}
						enabled = true;
						successors.addAll(image);
					}
					
					if (enabled) {
						Pair<Set<Long>, String> successor = Pair.create(successors, (String) null);
						result.addState(successor);
						result.addRequired(newActual, action, successor);
						if (!visited.contains(successor)) {
							pending.add(successor);
						}
					}
				}
			}
			result.removeUnreachableStates();
			return result;
		}
		
		public static void addExceptionTransitions(MTS<Long, String> c, ControllerGoal<String> goal) {
			Set<Long> uncontrolledStates = new HashSet<Long>();
			Set<String> uncontrolledActions = new HashSet<String>(c.getActions());
			uncontrolledActions.removeAll(goal.getControllableActions());
			
			initUncontrolledStates(c, uncontrolledStates, uncontrolledActions);
			Long EXCEPTION_STATE = new Long(c.getStates().size()+2);
			c.addAction("exception");
			c.addState(EXCEPTION_STATE);
			c.addRequired(EXCEPTION_STATE, "exception", EXCEPTION_STATE);
			
			for (Long state : uncontrolledStates) {
				for (String action : uncontrolledActions) {
					if (c.getTransitions(state, POSSIBLE).getImage(action).isEmpty()
							&& !action.startsWith("@") 
							&& !action.equals("tau")) {
						c.addRequired(state, action, EXCEPTION_STATE);
					}
					
				}
			}
		}

		private static  void initUncontrolledStates(MTS<Long, String> c,
				Set<Long> uncontrolledStates, Set<String> uncontrolledActions) {
			Set<Long> states = c.getStates();
			for (Long state : states) {
				for (String action : uncontrolledActions) {
					if (!c.getTransitions(state, POSSIBLE).getImage(action).isEmpty()) {
						uncontrolledStates.add(state);
						break;
					}
				}
			}
		}

		
}

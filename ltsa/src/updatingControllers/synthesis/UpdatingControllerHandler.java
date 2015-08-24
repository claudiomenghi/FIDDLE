package updatingControllers.synthesis;

import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.MTS.TransitionType;
import ac.ic.doc.mtstools.model.impl.MTSImpl;
import ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.FluentImpl;
import ar.dc.uba.model.language.SingleSymbol;
import ar.dc.uba.model.language.Symbol;
import lts.CompactState;
import lts.CompositeState;
import updatingControllers.UpdateConstants;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class UpdatingControllerHandler {

	public static Fluent createOnlyTurnOnFluent(String initAction) {
		HashSet<Symbol> initiatingActions = new HashSet<Symbol>();
		initiatingActions.add(new SingleSymbol(initAction));
				
		Fluent onlyTurnOnFluent = new FluentImpl(new String(initAction), initiatingActions, new HashSet<Symbol>(), false);
		return onlyTurnOnFluent;
	}
	
	///////////////////////////////// FOR DEBUGING //////////////////////////////////////////
	//// For debugging I used updating environment without minimization
/*
	public void checkMappingValue(ArrayList<String> actionsInTrace, LTSOutput ltsOutput) {
		
		ltsOutput.clearOutput();
		MTS<Long, String> updEnvironment = updEnv.getEnvironment();
		Long actualState = updEnvironment.getInitialState(); 
		boolean nonDeterministic = false;
		HashMap<String, Set<Long>> nextStep = new HashMap<String, Set<Long>>();
		
		ltsOutput.outln("Executing:");
		for (String action : actionsInTrace) {
			boolean hasAction = false;
			
			if (action.equals(UpdateConstants.START_NEW_SPEC) || nonDeterministic){
				if (!nonDeterministic) {
					printFluentValuation(ltsOutput, actualState);
					ltsOutput.outln("");
					HashSet<Long> firstStates = new HashSet<Long>();
					firstStates.add(actualState);
					nextStep = continueWithNoDeterministic(action, updEnvironment, ltsOutput, firstStates);
				} else {
					Set<Long> nextStates = nextStep.get(action);
					if (nextStates == null){
						ltsOutput.outln("The action: '"+action+"' can't be executed");
						return;
					}
					nextStep = continueWithNoDeterministic(action, updEnvironment, ltsOutput, nextStates);
				}
				
				nonDeterministic = true;
			} else {
				ltsOutput.outln("->"+action);
				for (Pair<String, Long> action_toState : updEnvironment.getTransitions(actualState, TransitionType.REQUIRED)) {
					
					String actionInMTS = action_toState.getFirst();
					Long toState = action_toState.getSecond();
					if (action.equals(actionInMTS) || new String(action+UpdateConstants.OLD_LABEL).equals
							(actionInMTS)) {
						hasAction = true;
						actualState = toState;
						break;
					}
				}
				if (!hasAction) {
					ltsOutput.outln("The trace given is imposible to execute");
					return;				
				}
			}
		}
		if (!nonDeterministic) {
			printFluentValuation(ltsOutput, actualState);
		} 
	}

	private void printFluentValuation(LTSOutput ltsOutput, Long actualState) {
		ltsOutput.outln("Fluents Valuation Before " + UpdateConstants.START_NEW_SPEC);
		Map<Long, Set<Fluent>> states_toFluent = mapping.getValuationOld().getStatesFromFluents();
		Set<Fluent> result = states_toFluent.get(actualState);
		if (result.isEmpty()) ltsOutput.outln("All Fluents are off");
		for (Fluent fluent : result) {
			ltsOutput.outln(fluent.toString());
		}
	}

	private HashMap<String, Set<Long>> continueWithNoDeterministic(String action, MTS<Long, String> updEnvironment, LTSOutput ltsOutput, Set<Long> set) {

		HashMap<String, Set<Long>> nextActions = new HashMap<String, Set<Long>>();

		for (Long nextStepState : set) {

			ltsOutput.outln("From state: ["+nextStepState.toString()+"]");
			for (Pair<String, Long> action_toState : updEnvironment.getTransitions(nextStepState, TransitionType.REQUIRED)) {

				String actionInMTS = action_toState.getFirst();
				Long toState = action_toState.getSecond();

				if (actionInMTS.equals(action)) {
					ltsOutput.outln("------"+actionInMTS+"----->"+"["+toState+"] *");
					for (Pair<String, Long> nextIteration  : updEnvironment.getTransitions(toState, TransitionType.REQUIRED)) {
						putInNextActions(nextIteration.getFirst(), toState, nextActions);
					}

				} else {
					ltsOutput.outln("------"+actionInMTS+"----->"+"["+toState+"]");
				}

			}
		}
		return nextActions;
	}

	private void putInNextActions(String action, Long fromState,
			HashMap<String, Set<Long>> nextActions) {

		Set<Long> value = null;
		if (nextActions.containsKey(action))
			value = nextActions.get(action);
		else
			value = new HashSet<Long>();

		value.add(fromState);
		nextActions.put(action, value);
	}
*/

	/////////////////////////////THIS CODE IS FOR RELABELING ACTIONS ///////////////////////////////
	public static void removeOldTransitions(CompositeState cs) {
		
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(cs.composition);
		MTS<Long, String> resultMts = new MTSImpl<Long, String>(mts.getInitialState());
		for (String action : mts.getActions()) {
			if (!isOld(action)){
				resultMts.addAction(action);
			}
		}
		
		for (Long state : mts.getStates()) {
			
			resultMts.addState(state);
			
			for (Pair<String, Long> action_toState : mts.getTransitions(state, MTS.TransitionType.REQUIRED)) { 
				
				if (!isOld(action_toState.getFirst())){
					resultMts.addState(action_toState.getSecond());
					resultMts.addRequired(state, action_toState.getFirst(), action_toState.getSecond());

				} else {
					resultMts.addState(action_toState.getSecond());
					resultMts.addRequired(state, withoutOld(action_toState.getFirst()), action_toState.getSecond());
				}
			}
		}
		cs.composition = MTSToAutomataConverter.getInstance().convert(resultMts, cs.composition.getName(), false);
		
	}

	public Vector<CompactState> addOldTransitionsToSafetyMachines(Vector<CompactState> machines, Set<String> toRelabelActions) {
		
		Vector<CompactState> withOldActionsMachines = new Vector<CompactState>();
		
		for (CompactState safetyGoal : machines) {
			withOldActionsMachines.add(addOldTransitions(safetyGoal, toRelabelActions));
		}
		
		return withOldActionsMachines;
	}
	
	private CompactState addOldTransitions(CompactState cs, Set<String> toRelabelActions) {
		
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(cs);
	
		for (Long state : mts.getStates()) {
			
			for (Pair<String, Long> action_toState : mts.getTransitions(state, TransitionType.REQUIRED)) {
				
				String action = action_toState.getFirst();
				Long toState = action_toState.getSecond();
				if (UpdatingControllersUtils.isNotUpdateAction(action) && toRelabelActions.contains(action)) {
					String actionWithOld = action + UpdateConstants.OLD_LABEL;
					mts.addAction(actionWithOld);
					mts.addRequired(state, actionWithOld, toState);

				}
			}
		}
		return MTSToAutomataConverter.getInstance().convert(mts, cs.getName(), false);
	}

	private static String withoutOld(String action) {

		return action.substring(0, action.length() - UpdateConstants.OLD_LABEL.length());
	}

	private static boolean isOld(String action) {
		
		return action.contains(UpdateConstants.OLD_LABEL);
	}
}
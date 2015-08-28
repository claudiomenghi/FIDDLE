package updatingControllers.synthesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import lts.CompactState;
import lts.LTSOutput;

import org.apache.commons.collections15.map.HashedMap;

import updatingControllers.UpdateConstants;
import updatingControllers.structures.MappingStructure;
import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.MTS.TransitionType;
import ac.ic.doc.mtstools.model.impl.MTSImpl;
import ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.FluentUtils;
import control.util.ControllerUtils;
import controller.game.util.FluentStateValuation;
import dispatcher.TransitionSystemDispatcher;

public class UpdatingEnvironmentGenerator {

	private MTS<Pair<Long, Long>, String> oldPart;
	private MTS<Long, String> oldController;
	private MTS<Long, String> oldEnvironment;
	private MTS<Long, String> hatEnvironment;
	private MTS<Long, String> newEnvironment;
	private List<Fluent> propositions;
	private MTS<Long, String> updatingEnvironment;
	private Long lastState;
	private Set<Long> eParallelCStates; // used for relabeling actions
	private Set<Long> oldEnvironmentStates;
	private MappingStructure mapping;
	private HashMap<Long, Long> newEnvToThis;

	public UpdatingEnvironmentGenerator(MTS<Long, String> oldController, MTS<Long, String> oldEnvironment, MTS<Long,
		String> hatEnvironment, MTS<Long, String> newEnvironment, List<Fluent> propositions) {

		this.oldController = oldController;
		this.oldEnvironment = oldEnvironment;
		this.hatEnvironment = hatEnvironment;
		this.newEnvironment = newEnvironment;
		this.propositions = propositions;
		this.mapping = null;
		this.newEnvToThis = null;

		updatingEnvironment = new MTSImpl<Long, String>(new Long(0));
		Pair<Long, Long> initialState = new Pair<Long, Long>(new Long(0), new Long(0));
		oldPart = new MTSImpl<Pair<Long, Long>, String>(initialState);

		lastState = new Long(0);

		eParallelCStates = new HashSet<Long>();
		oldEnvironmentStates = new HashSet<Long>();
	}

	public void generateEnvironment(Set<String> controllableActions, LTSOutput output) {
		
		this.removeTopStates();
		this.generateOldPart();
		this.completeWithHatEnvironment();
		this.changePairsToLong();
		mapping = new MappingStructure(updatingEnvironment, newEnvironment, propositions);
		this.newEnvToThis = this.linkStatesWithSameFluentValues(mapping);
		this.completeWithNewEnvironment(newEnvToThis);
	}

	private void removeTopStates() {
		
		hatEnvironment.addActions(newEnvironment.getActions());
		newEnvironment.addActions(hatEnvironment.getActions());
		
//		List<Fluent> newProposition = new ArrayList<Fluent>(); 
//		for (Fluent prop : propositions) {
//			if (!prop.getName().contains("action")){
//				newProposition.add(prop);
//			}
//		}
		
		hatEnvironment = ControllerUtils.removeTopStates(hatEnvironment, propositions);
		newEnvironment = ControllerUtils.removeTopStates(newEnvironment, propositions);
		
	}

	private void generateOldPart() {

		oldPart.addAction(UpdateConstants.BEGIN_UPDATE);

		// add beginUpdate transition and the new state from (0,0)
		addBeginUpdateTransition(oldPart.getInitialState());

		// BFS
		Queue<Pair<Long, Long>> toVisit = new LinkedList<Pair<Long, Long>>();
		Pair<Long, Long> firstState = new Pair<Long, Long>(oldController.getInitialState(), hatEnvironment
			.getInitialState());
		toVisit.add(firstState);
		ArrayList<Pair<Long, Long>> discovered = new ArrayList<Pair<Long, Long>>();

		while (!toVisit.isEmpty()) {
			Pair<Long, Long> actual = toVisit.remove();
			if (!discovered.contains(actual)) {
				discovered.add(actual);
				for (Pair<String, Long> action_toState : oldController.getTransitions(actual.getFirst(), MTS
					.TransitionType.REQUIRED)) {
					toVisit.addAll(nextToVisitInParallelComposition(actual, action_toState));
				}
			}
		}
	}

	private ArrayList<Pair<Long, Long>> nextToVisitInParallelComposition(Pair<Long, Long> actual, Pair<String, Long>
		transition) {

		ArrayList<Pair<Long, Long>> toVisit = new ArrayList<Pair<Long, Long>>();

		for (Pair<String, Long> action_toStateEnvironment : hatEnvironment.getTransitions(actual.getSecond(), MTS
			.TransitionType.REQUIRED)) {

			String action = action_toStateEnvironment.getFirst();
			Long toState = action_toStateEnvironment.getSecond();

			if (transition.getFirst().equals(action)) {

				//action = action.concat(UpdateControllerSolver.label); // rename the actions so as to
				// distinguish from the controllable in the new problem controller
				oldPart.addAction(action); // actions is a Set. it Avoids duplicated actions
				Pair<Long, Long> newState = new Pair<Long, Long>(transition.getSecond(), toState);
				oldPart.addState(newState);

				addBeginUpdateTransition(newState);
				oldPart.addRequired(new Pair<Long, Long>(actual.getFirst(), actual.getSecond()), action, newState);
				toVisit.add(new Pair<Long, Long>(transition.getSecond(), toState));
			}
		}

		return toVisit;
	}

	private void addBeginUpdateTransition(Pair<Long, Long> newState) {

		oldPart.addState(new Pair<Long, Long>(new Long(-1), newState.getSecond()));
		oldPart.addRequired(newState, UpdateConstants.BEGIN_UPDATE, new Pair<Long, Long>(new Long(-1), newState
			.getSecond()));
	}

	private void completeWithHatEnvironment() {

		for (Long state : hatEnvironment.getStates()) {

			for (Pair<String, Long> action_toState : hatEnvironment.getTransitions(state, TransitionType.REQUIRED)) {

				String action = action_toState.getFirst();
				Long toState = action_toState.getSecond();
				Pair<Long, Long> newState = new Pair<Long, Long>(new Long(-1), toState);
				oldPart.addState(newState);
				oldPart.addAction(action);
				Pair<Long, Long> currentState = new Pair<Long, Long>(new Long(-1), state);
				oldPart.addState(currentState); // caution with this
				oldPart.addRequired(currentState, action, newState);
			}
		}
	}

	private void changePairsToLong() {

		Map<Pair<Long, Long>, Long> visited = new HashMap<Pair<Long, Long>, Long>();

		// set first the state (0,0) to 0
		Pair<Long, Long> initialPair = new Pair<Long, Long>(new Long(0), new Long(0));
		Long initialState = this.getState(visited, initialPair);
		this.copyActions(visited, initialState, initialPair);

		for (Pair<Long, Long> pairState : oldPart.getStates()) {

			Long from = this.getState(visited, pairState);
			this.copyActions(visited, from, pairState);
		}
	}

	private Long getState(Map<Pair<Long, Long>, Long> visited, Pair<Long, Long> pairState) {
		Long state = null;
		if (visited.containsKey(pairState)) {
			state = visited.get(pairState);
		} else {
			visited.put(pairState, lastState);
			state = lastState;
			updatingEnvironment.addState(state);
			lastState++;
		}
		return state;
	}

	private void copyActions(Map<Pair<Long, Long>, Long> visited, Long longState, Pair<Long, Long> pairState) {

		for (Pair<String, Pair<Long, Long>> action_toState : oldPart.getTransitions(pairState, TransitionType
			.REQUIRED)) {

			String action = action_toState.getFirst();
			Pair<Long, Long> toPairState = action_toState.getSecond();
			updatingEnvironment.addAction(action);
			
			Long to = getState(visited, toPairState);
			updatingEnvironment.addRequired(longState, action, to);
		}
	}

	private HashMap<Long, Long> linkStatesWithSameFluentValues(MappingStructure mapping) {

		setStatesForEachPart();

		updatingEnvironment.addAction(UpdateConstants.RECONFIGURE);

		HashMap<Long, Long> newEnvToUpdEnv = new HashMap<Long, Long>();

		for (ArrayList<Boolean> oldValuation : mapping.valuationsOld()) {
			for (Long oldEnvState : mapping.getOldStates(oldValuation)) {
				//Build a no deterministic MTS
				if (mapping.containsNewValuation(oldValuation) && oldEnvironmentStates.contains(oldEnvState)) {
					for (Long newEnvironmentState : mapping.getNewStates(oldValuation)) {

						if (newEnvToUpdEnv.containsKey(newEnvironmentState)) {
							Long toState = newEnvToUpdEnv.get(newEnvironmentState);
							updatingEnvironment.addRequired(oldEnvState, UpdateConstants.RECONFIGURE, toState);
						} else {
							Long newState = new Long(lastState);
							updatingEnvironment.addState(newState);
							updatingEnvironment.addRequired(oldEnvState, UpdateConstants.RECONFIGURE, newState);

							newEnvToUpdEnv.put(newEnvironmentState, newState);
							lastState++;
						}
					}
				}
			}
		}
		return newEnvToUpdEnv;
	}

	private void setStatesForEachPart() {

		Fluent afterBeginUpdateFluent = UpdatingControllerHandler.createOnlyTurnOnFluent(UpdateConstants.BEGIN_UPDATE);
		Set<Fluent> fluentSet = new HashSet<Fluent>();
		fluentSet.add(afterBeginUpdateFluent);

		FluentStateValuation<Long> valuationAfterBeginUpdate = FluentUtils.getInstance().buildValuation(updatingEnvironment,
			fluentSet);

		for (Long state : updatingEnvironment.getStates()) {
			if (valuationAfterBeginUpdate.isTrue(state, afterBeginUpdateFluent)) {
				addStopOldAndStartNewSpecActions(state);
				oldEnvironmentStates.add(state);
			} else {
				eParallelCStates.add(state);
			}
		}
	}

	private void addStopOldAndStartNewSpecActions(Long state) {
		updatingEnvironment.addAction(UpdateConstants.STOP_OLD_SPEC);
		updatingEnvironment.addAction(UpdateConstants.START_NEW_SPEC);
		updatingEnvironment.addRequired(state, UpdateConstants.STOP_OLD_SPEC, state);
		updatingEnvironment.addRequired(state, UpdateConstants.START_NEW_SPEC, state);
	}

	private void completeWithNewEnvironment(HashMap<Long, Long> newEnvToUpdEnv) {

		for (Long state : newEnvironment.getStates()) {

			for (Pair<String, Long> action_toState : newEnvironment.getTransitions(state, MTS.TransitionType
				.REQUIRED)) {

				if (newEnvToUpdEnv.containsKey(state)) {

					Long updEnvState = newEnvToUpdEnv.get(state);
					addTransitionCreatingNewStates(action_toState, updEnvState, newEnvToUpdEnv);
					addStopOldAndStartNewSpecActions(updEnvState);
				} else {

					Long updEnvState = addState(state, newEnvToUpdEnv);
					addTransitionCreatingNewStates(action_toState, updEnvState, newEnvToUpdEnv);
					addStopOldAndStartNewSpecActions(updEnvState);
				}
			}
		}
	}

	private void addTransitionCreatingNewStates(Pair<String, Long> action_toState, Long state, HashMap<Long, Long>
		newEnvToUpdEnv) {

		updatingEnvironment.addAction(action_toState.getFirst());
		if (!newEnvToUpdEnv.containsKey(action_toState.getSecond())) {

			Long newState = addState(action_toState.getSecond(), newEnvToUpdEnv);
			updatingEnvironment.addRequired(state, action_toState.getFirst(), newState);
		} else {
			updatingEnvironment.addRequired(state, action_toState.getFirst(), newEnvToUpdEnv.get(action_toState.getSecond()));
		}
	}

	private Long addState(Long originalState, HashMap<Long, Long> newEnvToUpdEnv) {

		Long newState = new Long(lastState);
		updatingEnvironment.addState(newState);
		newEnvToUpdEnv.put(originalState, newState);
		lastState++;
		return newState;
	}

	public MTS<Long, String> getUpdEnv(){
		return updatingEnvironment;
	}
	
	public MappingStructure getMapping(){
		return mapping;
	}
	
	public boolean isEParrallelCState(Long state){
		return eParallelCStates.contains(state);
	}
	
	public boolean isHatEnvironmentState(Long state){
		return oldEnvironmentStates.contains(state);
	}

	public ArrayList<Boolean> getOldValuation(Long state) {
		return mapping.getOldValuation(state);
	}
	
	public ArrayList<Boolean> getNewValuation(Long state) {
		return mapping.getNewValuation(state);
	}

	public Long mapStateToValuationState(Long state) {
		for (Entry<Long, Long> entry : newEnvToThis.entrySet()) {
			if (entry.getValue().equals(state)){
				return entry.getKey();
			}
		}
		return null;
	}

	public List<Fluent> getPropositions() {
		
		return propositions;
	}
}
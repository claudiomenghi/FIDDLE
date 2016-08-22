package ltsa.lts.ltl.ltlftoba;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;

/**
 * removes the states from which it is not reachable an accepting state that can
 * be entered infinitely many often.
 * 
 *
 */
public class NoAcceptingRemover implements
		Function<LabelledTransitionSystem, LabelledTransitionSystem> {

	/**
	 * removes the states from which it is not reachable an accepting state that
	 * can be entered infinitely many often.
	 * 
	 * @param s
	 *            the automaton to be considered
	 * @throws NullPointerException
	 *             if the automaton to be considered is null
	 */
	@Override
	public LabelledTransitionSystem apply(LabelledTransitionSystem s) {

		Preconditions.checkNotNull(s,
				"The automaton to be considered cannot be null");
		LabelledTransitionSystem ret = s.myclone();

		Set<Integer> states = this.getStatesFromWichReachableAccepting(ret);

		LTSTransitionList[] transitions = ret.getStates();

		for (int stateIndex = 0; stateIndex < transitions.length; stateIndex++) {
			if (states.contains(stateIndex)) {
				visited = new HashSet<>();
				ret.getStates()[stateIndex] = this
						.keepOnlyNextTransitionBetweenStates(stateIndex,
								ret.getTransitions(stateIndex), states);
			} else {
				ret.getStates()[stateIndex] = null;
			}
		}
		ret = retainStates(ret, states);

		return ret;
	}

	private static Set<LTSTransitionList> visited = new HashSet<>();

	/**
	 * removes from the automaton all the states that is not contained in the
	 * set
	 * 
	 * @param s
	 *            the automaton to be considered
	 * @param states
	 *            the set of the states to be kept in the automaton
	 * @return a modified version of the automaton that contains only the
	 *         specified states
	 * @throws NullPointerException
	 *             if one of the parameters is null
	 */
	private LabelledTransitionSystem retainStates(LabelledTransitionSystem s,
			Set<Integer> states) {
		Preconditions.checkNotNull(s,
				"The automaton to be considered cannot be null");
		Preconditions.checkNotNull(states,
				"The set of the states to be considered cannot be null");
		// if (!states.contains(0)) {
		// throw new IllegalArgumentException(
		// "The initial state must be contained into the set of the states");
		// }

		LabelledTransitionSystem oldState = s.myclone();
		// states.remove(0);
		Map<Integer, Integer> mapOldIndexNewIndex = new HashMap<>();
		// mapOldIndexNewIndex.put(0, 0);
		List<Integer> oldIndexes = new ArrayList<>(states);

		for (int i = 0; i < states.size(); i++) {
			mapOldIndexNewIndex.put(oldIndexes.get(i), i);
		}

		s.setStates(new LTSTransitionList[states.size()]);

		for (Entry<Integer, Integer> entry : mapOldIndexNewIndex.entrySet()) {
			int oldIndex = entry.getKey();
			int newIndex = entry.getValue();
			s.getStates()[newIndex] = relabelList(
					oldState.getStates()[oldIndex], mapOldIndexNewIndex);
		}
		return s;
	}

	private LTSTransitionList relabelList(LTSTransitionList transition,
			Map<Integer, Integer> mapOldIndexNewIndex) {

		LTSTransitionList currentTransition = transition;
		while (currentTransition != null) {
			currentTransition.setNext(mapOldIndexNewIndex.get(currentTransition
					.getNext()));
			currentTransition.setNondet(relabelNonDet(
					currentTransition.getNondet(), mapOldIndexNewIndex));
			currentTransition = currentTransition.getList();
		}
		return transition;
	}

	private LTSTransitionList relabelNonDet(LTSTransitionList transition,
			Map<Integer, Integer> mapOldIndexNewIndex) {

		LTSTransitionList currentTransition = transition;
		while (currentTransition != null) {
			currentTransition.setNext(mapOldIndexNewIndex.get(currentTransition
					.getNext()));
			currentTransition = currentTransition.getNondet();
		}
		return transition;
	}

	private LTSTransitionList keepOnlyNextTransitionBetweenStates(int source,
			LTSTransitionList transitions, Set<Integer> states) {
		visited.add(transitions);
		if (transitions == null) {
			return null;
		}
		if (!states.contains(source)) {
			return null;
		}
		LTSTransitionList nextTransitions;
		if (!visited.contains(transitions)) {
			 nextTransitions = this
					.keepOnlyNextTransitionBetweenStates(source,
							transitions.getList(), states);
		}
		else{
			nextTransitions=null;
		}
		LTSTransitionList retTransition = null;
		LTSTransitionList nonDetTransitions = this.getNonDetTransitions(source,
				transitions.getNondet(), states);

		if (states.contains(transitions.getNext())) {
			retTransition = new LTSTransitionList(transitions.getEvent(),
					transitions.getNext(), transitions.getMachine());
			retTransition.setNondet(nonDetTransitions);
			retTransition.setList(nextTransitions);

		} else {
			if (nonDetTransitions != null) {
				retTransition = nonDetTransitions;
				retTransition.setNondet(nonDetTransitions.getNondet());
				retTransition.setList(nextTransitions);
			} else {
				return nextTransitions;
			}
		}
		return retTransition;

	}

	private LTSTransitionList getNonDetTransitions(int source,
			LTSTransitionList transitions, Set<Integer> states) {

		if (transitions == null) {
			return null;
		}
		if (transitions.getList() != null) {
			throw new InternalError("The transition with source " + source
					+ " and destination " + transitions.getNext()
					+ " and event " + transitions.getEvent()
					+ " must have a null list");
		}
		LTSTransitionList next = this.getNonDetTransitions(source,
				transitions.getNondet(), states);
		if (states.contains(transitions.getNext())) {
			transitions.setNondet(next);
			return transitions;
		} else {
			return next;
		}
	}

	/**
	 * returns the states from which an accepting state is reachable
	 * 
	 * @return the states from which an accepting state is reachable
	 */
	private Set<Integer> getStatesFromWichReachableAccepting(
			LabelledTransitionSystem ret) {
		Map<Integer, Set<Integer>> reversedReachable = this
				.computeInverseTransitionRelation(ret);

		Set<Integer> reachable = new HashSet<>();
		Set<Integer> current = ret.getAccepting();

		while (!current.isEmpty()) {
			Integer evaluated = current.iterator().next();
			reachable.add(evaluated);
			current.remove(evaluated);
			Set<Integer> prev = new HashSet<>(reversedReachable.get(evaluated));
			prev.removeAll(reachable);
			current.addAll(prev);
		}

		return reachable;
	}

	/**
	 * returns a map that contains for each state its predecessors
	 * 
	 * @param ret
	 *            a map that maps each state to its predecessors
	 * @return a map that maps each state to its predecessors.
	 */
	private Map<Integer, Set<Integer>> computeInverseTransitionRelation(
			LabelledTransitionSystem ret) {

		Map<Integer, Set<Integer>> reverseTransitionMap = new HashMap<>();
		LTSTransitionList[] transitions = ret.getStates();

		for (int stateIndex = 0; stateIndex < transitions.length; stateIndex++) {
			reverseTransitionMap.put(stateIndex, new HashSet<>());
		}

		for (int stateIndex = 0; stateIndex < transitions.length; stateIndex++) {

			LTSTransitionList transition = transitions[stateIndex];
			this.updateIndexes(stateIndex, transition, reverseTransitionMap);
		}

		return reverseTransitionMap;
	}

	private void updateIndexes(int sourceState, LTSTransitionList transition,
			Map<Integer, Set<Integer>> reverseTransitionMap) {
		if (transition != null) {
			reverseTransitionMap.get(transition.getNext()).add(sourceState);
			this.updateIndexes(sourceState, transition.getList(),
					reverseTransitionMap);
			this.updateIndexes(sourceState, transition.getNondet(),
					reverseTransitionMap);
		}

	}
}

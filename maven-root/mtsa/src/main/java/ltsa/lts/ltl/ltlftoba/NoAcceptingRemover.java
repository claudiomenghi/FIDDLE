package ltsa.lts.ltl.ltlftoba;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;

import com.google.common.base.Preconditions;

/**
 * removes the states from which it is not reachable an accepting state that can
 * be entered infinitely many often.
 * 
 *
 */
<<<<<<< HEAD
<<<<<<< HEAD
public class NoAcceptingRemover implements
		Function<LabelledTransitionSystem, LabelledTransitionSystem> {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());
	
=======
public class NoAcceptingRemover implements Function<LabelledTransitionSystem, LabelledTransitionSystem> {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
public class NoAcceptingRemover implements Function<LabelledTransitionSystem, LabelledTransitionSystem> {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

>>>>>>> dev
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

<<<<<<< HEAD
<<<<<<< HEAD
		Preconditions.checkNotNull(s,
				"The automaton to be considered cannot be null");
=======
		Preconditions.checkNotNull(s, "The automaton to be considered cannot be null");
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
		Preconditions.checkNotNull(s, "The automaton to be considered cannot be null");
>>>>>>> dev
		LabelledTransitionSystem ret = s.myclone();

		Set<Integer> states = this.getStatesFromWichReachableAccepting(ret);

<<<<<<< HEAD
<<<<<<< HEAD
		logger.debug("States from which an accepting state can be reached: "+states);
		
		Set<Integer> allStates=new HashSet<>();
		for(int i=0; i<ret.getNumberOfStates(); i++){
=======
		logger.debug("States from which an accepting state can be reached: " + states);

		Set<Integer> allStates = new HashSet<>();
		for (int i = 0; i < ret.getNumberOfStates(); i++) {
>>>>>>> dev
			allStates.add(i);
		}
		allStates.removeAll(states);
		logger.debug("States from which an accepting state can not be reached: " + allStates);

<<<<<<< HEAD
=======
		logger.debug("States from which an accepting state can be reached: " + states);

		Set<Integer> allStates = new HashSet<>();
		for (int i = 0; i < ret.getNumberOfStates(); i++) {
			allStates.add(i);
		}
		allStates.removeAll(states);
		logger.debug("States from which an accepting state can not be reached: " + allStates);

		ret.removeStates(allStates);
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
		ret.removeStates(allStates);
>>>>>>> dev

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
<<<<<<< HEAD
<<<<<<< HEAD
	private LabelledTransitionSystem retainStates(LabelledTransitionSystem s,
			Set<Integer> states) {
		Preconditions.checkNotNull(s,
				"The automaton to be considered cannot be null");
		Preconditions.checkNotNull(states,
				"The set of the states to be considered cannot be null");
=======
	private LabelledTransitionSystem retainStates(LabelledTransitionSystem s, Set<Integer> states) {
		Preconditions.checkNotNull(s, "The automaton to be considered cannot be null");
		Preconditions.checkNotNull(states, "The set of the states to be considered cannot be null");
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
	private LabelledTransitionSystem retainStates(LabelledTransitionSystem s, Set<Integer> states) {
		Preconditions.checkNotNull(s, "The automaton to be considered cannot be null");
		Preconditions.checkNotNull(states, "The set of the states to be considered cannot be null");
>>>>>>> dev
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
<<<<<<< HEAD
<<<<<<< HEAD
			s.getStates()[newIndex] = relabelList(
					oldState.getStates()[oldIndex], mapOldIndexNewIndex);
=======
			s.getStates()[newIndex] = relabelList(oldState.getStates()[oldIndex], mapOldIndexNewIndex);
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
			s.getStates()[newIndex] = relabelList(oldState.getStates()[oldIndex], mapOldIndexNewIndex);
>>>>>>> dev
		}
		return s;
	}

<<<<<<< HEAD
<<<<<<< HEAD
	private LTSTransitionList relabelList(LTSTransitionList transition,
			Map<Integer, Integer> mapOldIndexNewIndex) {

		LTSTransitionList currentTransition = transition;
		while (currentTransition != null) {
			currentTransition.setNext(mapOldIndexNewIndex.get(currentTransition
					.getNext()));
			currentTransition.setNondet(relabelNonDet(
					currentTransition.getNondet(), mapOldIndexNewIndex));
=======
	private LTSTransitionList relabelList(LTSTransitionList transition, Map<Integer, Integer> mapOldIndexNewIndex) {

		LTSTransitionList currentTransition = transition;
		while (currentTransition != null) {
			currentTransition.setNext(mapOldIndexNewIndex.get(currentTransition.getNext()));
			currentTransition.setNondet(relabelNonDet(currentTransition.getNondet(), mapOldIndexNewIndex));
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
	private LTSTransitionList relabelList(LTSTransitionList transition, Map<Integer, Integer> mapOldIndexNewIndex) {

		LTSTransitionList currentTransition = transition;
		while (currentTransition != null) {
			currentTransition.setNext(mapOldIndexNewIndex.get(currentTransition.getNext()));
			currentTransition.setNondet(relabelNonDet(currentTransition.getNondet(), mapOldIndexNewIndex));
>>>>>>> dev
			currentTransition = currentTransition.getList();
		}
		return transition;
	}

<<<<<<< HEAD
<<<<<<< HEAD
	private LTSTransitionList relabelNonDet(LTSTransitionList transition,
			Map<Integer, Integer> mapOldIndexNewIndex) {

		LTSTransitionList currentTransition = transition;
		while (currentTransition != null) {
			currentTransition.setNext(mapOldIndexNewIndex.get(currentTransition
					.getNext()));
=======
	private LTSTransitionList relabelNonDet(LTSTransitionList transition, Map<Integer, Integer> mapOldIndexNewIndex) {

		LTSTransitionList currentTransition = transition;
		while (currentTransition != null) {
			currentTransition.setNext(mapOldIndexNewIndex.get(currentTransition.getNext()));
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
	private LTSTransitionList relabelNonDet(LTSTransitionList transition, Map<Integer, Integer> mapOldIndexNewIndex) {

		LTSTransitionList currentTransition = transition;
		while (currentTransition != null) {
			currentTransition.setNext(mapOldIndexNewIndex.get(currentTransition.getNext()));
>>>>>>> dev
			currentTransition = currentTransition.getNondet();
		}
		return transition;
	}

<<<<<<< HEAD
<<<<<<< HEAD
	private LTSTransitionList keepOnlyNextTransitionBetweenStates(int source,
			LTSTransitionList transitions, Set<Integer> states) {
=======
	private LTSTransitionList keepOnlyNextTransitionBetweenStates(int source, LTSTransitionList transitions,
			Set<Integer> states) {
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
	private LTSTransitionList keepOnlyNextTransitionBetweenStates(int source, LTSTransitionList transitions,
			Set<Integer> states) {
>>>>>>> dev
		visited.add(transitions);
		if (transitions == null) {
			return null;
		}
		if (!states.contains(source)) {
			return null;
		}
		LTSTransitionList nextTransitions;
		if (!visited.contains(transitions)) {
<<<<<<< HEAD
<<<<<<< HEAD
			 nextTransitions = this
					.keepOnlyNextTransitionBetweenStates(source,
							transitions.getList(), states);
		}
		else{
			nextTransitions=null;
=======
			nextTransitions = this.keepOnlyNextTransitionBetweenStates(source, transitions.getList(), states);
		} else {
			nextTransitions = null;
>>>>>>> dev
		}
		LTSTransitionList retTransition = null;
		LTSTransitionList nonDetTransitions = this.getNonDetTransitions(source, transitions.getNondet(), states);

		if (states.contains(transitions.getNext())) {
<<<<<<< HEAD
			retTransition = new LTSTransitionList(transitions.getEvent(),
					transitions.getNext(), transitions.getMachine());
=======
			nextTransitions = this.keepOnlyNextTransitionBetweenStates(source, transitions.getList(), states);
		} else {
			nextTransitions = null;
		}
		LTSTransitionList retTransition = null;
		LTSTransitionList nonDetTransitions = this.getNonDetTransitions(source, transitions.getNondet(), states);

		if (states.contains(transitions.getNext())) {
			retTransition = new LTSTransitionList(transitions.getEvent(), transitions.getNext(),
					transitions.getMachine());
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
			retTransition = new LTSTransitionList(transitions.getEvent(), transitions.getNext(),
					transitions.getMachine());
>>>>>>> dev
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

<<<<<<< HEAD
<<<<<<< HEAD
	private LTSTransitionList getNonDetTransitions(int source,
			LTSTransitionList transitions, Set<Integer> states) {
=======
	private LTSTransitionList getNonDetTransitions(int source, LTSTransitionList transitions, Set<Integer> states) {
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
	private LTSTransitionList getNonDetTransitions(int source, LTSTransitionList transitions, Set<Integer> states) {
>>>>>>> dev

		if (transitions == null) {
			return null;
		}
		if (transitions.getList() != null) {
<<<<<<< HEAD
<<<<<<< HEAD
			throw new InternalError("The transition with source " + source
					+ " and destination " + transitions.getNext()
					+ " and event " + transitions.getEvent()
					+ " must have a null list");
		}
		LTSTransitionList next = this.getNonDetTransitions(source,
				transitions.getNondet(), states);
=======
			throw new InternalError("The transition with source " + source + " and destination " + transitions.getNext()
					+ " and event " + transitions.getEvent() + " must have a null list");
		}
		LTSTransitionList next = this.getNonDetTransitions(source, transitions.getNondet(), states);
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
			throw new InternalError("The transition with source " + source + " and destination " + transitions.getNext()
					+ " and event " + transitions.getEvent() + " must have a null list");
		}
		LTSTransitionList next = this.getNonDetTransitions(source, transitions.getNondet(), states);
>>>>>>> dev
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
<<<<<<< HEAD
<<<<<<< HEAD
	private Set<Integer> getStatesFromWichReachableAccepting(
			LabelledTransitionSystem ret) {
		Map<Integer, Set<Integer>> reversedReachable = this
				.computeInverseTransitionRelation(ret);
=======
	private Set<Integer> getStatesFromWichReachableAccepting(LabelledTransitionSystem ret) {
		Map<Integer, Set<Integer>> reversedReachable = this.computeInverseTransitionRelation(ret);
>>>>>>> dev

		Set<Integer> reachable = new HashSet<>();

		logger.debug("Accepting states:  " + ret.getAccepting());
		Set<Integer> current = ret.getAccepting();

		boolean[] visited = new boolean[ret.getStates().length];

		while (!current.isEmpty()) {
			Integer evaluated = current.iterator().next();
			current.remove(evaluated);
<<<<<<< HEAD
			Set<Integer> prev = new HashSet<>(reversedReachable.get(evaluated));
			prev.removeAll(reachable);
			current.addAll(prev);
=======
	private Set<Integer> getStatesFromWichReachableAccepting(LabelledTransitionSystem ret) {
		Map<Integer, Set<Integer>> reversedReachable = this.computeInverseTransitionRelation(ret);

		Set<Integer> reachable = new HashSet<>();

		logger.debug("Accepting states:  " + ret.getAccepting());
		Set<Integer> current = ret.getAccepting();

		boolean[] visited = new boolean[ret.getStates().length];

		while (!current.isEmpty()) {
			Integer evaluated = current.iterator().next();
			current.remove(evaluated);
=======
>>>>>>> dev
			if (!visited[evaluated]) {
				visited[evaluated] = true;
				reachable.add(evaluated);
				Set<Integer> prev = new HashSet<>(reversedReachable.get(evaluated));
				current.addAll(prev);
			}
<<<<<<< HEAD
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
>>>>>>> dev
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
<<<<<<< HEAD
<<<<<<< HEAD
	private Map<Integer, Set<Integer>> computeInverseTransitionRelation(
			LabelledTransitionSystem ret) {
=======
	private Map<Integer, Set<Integer>> computeInverseTransitionRelation(LabelledTransitionSystem ret) {
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
	private Map<Integer, Set<Integer>> computeInverseTransitionRelation(LabelledTransitionSystem ret) {
>>>>>>> dev

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
<<<<<<< HEAD
<<<<<<< HEAD
			this.updateIndexes(sourceState, transition.getList(),
					reverseTransitionMap);
			this.updateIndexes(sourceState, transition.getNondet(),
					reverseTransitionMap);
=======
			this.updateIndexes(sourceState, transition.getList(), reverseTransitionMap);
			this.updateIndexes(sourceState, transition.getNondet(), reverseTransitionMap);
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
			this.updateIndexes(sourceState, transition.getList(), reverseTransitionMap);
			this.updateIndexes(sourceState, transition.getNondet(), reverseTransitionMap);
>>>>>>> dev
		}

	}
}

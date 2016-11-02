package scalabilityAssessment.modelgenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;

public class RandomControllerGenerator {

	public LabelledTransitionSystem getComponent(ModelConfiguration size) {

		LabelledTransitionSystem ltsCopy = new RandomLTSGenerator(
				size.getStatesController(), size.getEventsController(),
				size.getTransitionsPerStateController()).getRandomLTS("CONTROLLER");

		int boxIndex = new Random().nextInt(ltsCopy.getStates().length);

		List<String> events = new ArrayList<>(ltsCopy.getAlphabetEvents());
		List<String> interfaceBox = events.subList(0,
				size.getEventsControllerInterface());
		ltsCopy.addBoxIndex("box", boxIndex);
		ltsCopy.setBoxInterface("box", new HashSet<>(interfaceBox));

		return ltsCopy;
	}

	/*
	 * public LabelledTransitionSystem getComponent(LabelledTransitionSystem
	 * lts) {
	 * 
	 * LabelledTransitionSystem ltsCopy = lts.myclone(); List<Integer> states =
	 * new ArrayList<>();
	 * 
	 * IntStream.range(0, ltsCopy.getNumberOfStates()).forEach(states::add);
	 * 
	 * int boxIndex = ltsCopy.addNewState(); ltsCopy.addBoxIndex("box",
	 * boxIndex); ltsCopy.mapBoxInterface.put("box", new
	 * HashSet<>(ltsCopy.getAlphabetCharacters())); Collections.shuffle(states);
	 * List<Integer> toBeRemoved = states.subList(0, states.size() /
	 * TOBEREMOVEDRATIO);
	 * 
	 * removeTransitionsBetweenInternalStates(ltsCopy, toBeRemoved);
	 * 
	 * addsTheTransitionsOfTheRemovedStatesToTheBox(ltsCopy, boxIndex,
	 * toBeRemoved);
	 * 
	 * changeTheDestinationOfAStateToBeRemovedTransition(lts, boxIndex,
	 * toBeRemoved);
	 * 
	 * ltsCopy.removeStates(toBeRemoved);
	 * 
	 * return ltsCopy; }
	 * 
	 * private void changeTheDestinationOfAStateToBeRemovedTransition(
	 * LabelledTransitionSystem lts, int boxIndex, List<Integer> toBeRemoved) {
	 * for (int index = 0; index < lts.getStates().length; index++) {
	 * LTSTransitionList componentTransitions = lts.getTransitions(index); if
	 * (componentTransitions != null) { Enumeration<LTSTransitionList>
	 * transitions = componentTransitions .elements();
	 * 
	 * while (transitions.hasMoreElements()) { LTSTransitionList transition =
	 * transitions.nextElement(); if
	 * (toBeRemoved.contains(transition.getNext())) {
	 * transition.setNext(boxIndex); } } } } }
	 * 
	 * private void addsTheTransitionsOfTheRemovedStatesToTheBox(
	 * LabelledTransitionSystem ltsCopy, int boxIndex, List<Integer>
	 * toBeRemoved) { LTSTransitionList boxTransitionList =
	 * ltsCopy.getTransitions(boxIndex); for (Integer stateToBeRemoved :
	 * toBeRemoved) { LTSTransitionList transitionOfTheStateToBeRemoved =
	 * ltsCopy .getTransitions(stateToBeRemoved); boxTransitionList =
	 * EventStateUtils.add(boxTransitionList, transitionOfTheStateToBeRemoved);
	 * } ltsCopy.setState(boxIndex, boxTransitionList); }
	 * 
	 * private void removeTransitionsBetweenInternalStates(
	 * LabelledTransitionSystem ltsCopy, List<Integer> toBeRemoved) {
	 * Set<String> events = new HashSet<>(); // removing the transitions between
	 * the states that must be encapsulated // in the box Map<Integer,
	 * Set<LTSTransitionList>> transitionsToBeRemoved = new HashMap<>(); for
	 * (Integer stateToBeRemoved : toBeRemoved) {
	 * transitionsToBeRemoved.put(stateToBeRemoved, new
	 * HashSet<LTSTransitionList>()); LTSTransitionList
	 * transitionOfTheStateToBeRemoved = ltsCopy
	 * .getTransitions(stateToBeRemoved); if (transitionOfTheStateToBeRemoved !=
	 * null) { Enumeration<LTSTransitionList> transitions =
	 * transitionOfTheStateToBeRemoved .elements(); while
	 * (transitions.hasMoreElements()) { LTSTransitionList transition =
	 * transitions.nextElement(); if
	 * (toBeRemoved.contains(transition.getNext())) {
	 * events.add(ltsCopy.getAlphabet()[transition.getEvent()]);
	 * transitionsToBeRemoved.get(stateToBeRemoved).add( transition); } } } }
	 * for (Integer index : transitionsToBeRemoved.keySet()) { for
	 * (LTSTransitionList transition : transitionsToBeRemoved .get(index)) {
	 * ltsCopy.setState(index, LTSTransitionList.remove(
	 * ltsCopy.getStates()[index], transition)); } }
	 * ltsCopy.mapBoxInterface.put("box", new
	 * HashSet<String>(ltsCopy.getAlphabetCharacters())); //
	 * ltsCopy.mapBoxInterface.put("box", events); }
	 */
}

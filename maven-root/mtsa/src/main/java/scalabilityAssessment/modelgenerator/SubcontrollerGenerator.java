package scalabilityAssessment.modelgenerator;

import java.util.Arrays;
import java.util.Enumeration;

import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;

public class SubcontrollerGenerator {

	/**
	 * contains the final number of states of the subcontroller
	 */
	private final int numberOfStates;

	public SubcontrollerGenerator(int numberOfStates) {
		this.numberOfStates = numberOfStates;
	}

<<<<<<< HEAD
<<<<<<< HEAD
	public LabelledTransitionSystem subController(
			LabelledTransitionSystem controller) {
=======
	public LabelledTransitionSystem subController(LabelledTransitionSystem controller) {
>>>>>>> dev

		LabelledTransitionSystem subcontroller = new LabelledTransitionSystem("SUB-CONTROLLER");

		subcontroller.setStates(new LTSTransitionList[numberOfStates + 1]);

		int endStateIndex = numberOfStates;

		int[] newIndex = new int[controller.getAlphabet().length];
		Arrays.fill(newIndex, -1);

		int currentFreeIndex = 0;
		// adding transitions between internal states
		for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
			LTSTransitionList transitions = controller.getTransitions(stateIndex);
			if (transitions != null) {
				Enumeration<LTSTransitionList> transitionList = transitions.elements();
				while (transitionList.hasMoreElements()) {
					LTSTransitionList currentTransition = transitionList.nextElement();
					if (currentTransition.getNext() < numberOfStates) {

						if (newIndex[currentTransition.getEvent()] == -1) {
							newIndex[currentTransition.getEvent()] = currentFreeIndex;
							currentFreeIndex++;
						}
						subcontroller.setState(stateIndex,
								LTSTransitionList.addTransition(subcontroller.getTransitions(stateIndex),
										newIndex[currentTransition.getEvent()], currentTransition.getNext()));
					}
<<<<<<< HEAD
					subcontroller.setState(stateIndex, LTSTransitionList
							.addTransition(
									subcontroller.getTransitions(stateIndex),
									newIndex[currentTransition.getEvent()],
									currentTransition.getNext()));
=======
	public LabelledTransitionSystem subController(LabelledTransitionSystem controller) {

		LabelledTransitionSystem subcontroller = new LabelledTransitionSystem("SUB-CONTROLLER");

		subcontroller.setStates(new LTSTransitionList[numberOfStates + 1]);

		int endStateIndex = numberOfStates;

		int[] newIndex = new int[controller.getAlphabet().length];
		Arrays.fill(newIndex, -1);

		int currentFreeIndex = 0;
		// adding transitions between internal states
		for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
			LTSTransitionList transitions = controller.getTransitions(stateIndex);
			if (transitions != null) {
				Enumeration<LTSTransitionList> transitionList = transitions.elements();
				while (transitionList.hasMoreElements()) {
					LTSTransitionList currentTransition = transitionList.nextElement();
					if (currentTransition.getNext() < numberOfStates) {

						if (newIndex[currentTransition.getEvent()] == -1) {
							newIndex[currentTransition.getEvent()] = currentFreeIndex;
							currentFreeIndex++;
						}
						subcontroller.setState(stateIndex,
								LTSTransitionList.addTransition(subcontroller.getTransitions(stateIndex),
										newIndex[currentTransition.getEvent()], currentTransition.getNext()));
					}
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
>>>>>>> dev
				}
			}
		}
		// initializing The alphabet
<<<<<<< HEAD
<<<<<<< HEAD
		String[] alphabet=new String[currentFreeIndex];
		for(int i=0; i<newIndex.length; i++){
			if(newIndex[i]!=-1){
				alphabet[newIndex[i]]=controller.getAlphabet()[i];
=======
		String[] alphabet = new String[currentFreeIndex];
		for (int i = 0; i < newIndex.length; i++) {
			if (newIndex[i] != -1) {
				alphabet[newIndex[i]] = controller.getAlphabet()[i];
>>>>>>> dev
			}
		}
		subcontroller.setAlphabet(alphabet);

		subcontroller.addFinalStateIndex(endStateIndex);
<<<<<<< HEAD
		int endEventIndex = subcontroller.addEvent(LTLf2LTS.endSymbol
				.getValue());
		subcontroller
				.addTransition(endStateIndex, endEventIndex, endStateIndex);
=======
		String[] alphabet = new String[currentFreeIndex];
		for (int i = 0; i < newIndex.length; i++) {
			if (newIndex[i] != -1) {
				alphabet[newIndex[i]] = controller.getAlphabet()[i];
			}
		}
		subcontroller.setAlphabet(alphabet);

		subcontroller.addFinalStateIndex(endStateIndex);
		int endEventIndex = subcontroller.addEvent(LTLf2LTS.endSymbol.getValue());
		subcontroller.addTransition(endStateIndex, endEventIndex, endStateIndex);
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
		int endEventIndex = subcontroller.addEvent(LTLf2LTS.endSymbol.getValue());
		subcontroller.addTransition(endStateIndex, endEventIndex, endStateIndex);
>>>>>>> dev
		// adding transitions to final states
		for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
			boolean found = false;

<<<<<<< HEAD
<<<<<<< HEAD
			LTSTransitionList transitions = subcontroller
					.getTransitions(stateIndex);
			Enumeration<LTSTransitionList> transitionList = transitions
					.elements();
			while (transitionList.hasMoreElements() && !found) {
				LTSTransitionList currentTransition = transitionList
						.nextElement();
				if (currentTransition.getNext() >= numberOfStates) {
					found = true;
					subcontroller
							.setState(endStateIndex, LTSTransitionList
									.addTransition(subcontroller
											.getTransitions(endStateIndex),
											endEventIndex, endStateIndex));
=======
=======
>>>>>>> dev
			LTSTransitionList transitions = subcontroller.getTransitions(stateIndex);

			if (transitions != null) {
				Enumeration<LTSTransitionList> transitionList = transitions.elements();
				while (transitionList.hasMoreElements() && !found) {
					LTSTransitionList currentTransition = transitionList.nextElement();
					if (currentTransition.getNext() >= numberOfStates) {
						found = true;
						subcontroller.setState(endStateIndex, LTSTransitionList.addTransition(
								subcontroller.getTransitions(endStateIndex), endEventIndex, endStateIndex));
					}
<<<<<<< HEAD
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
>>>>>>> dev
				}
			}
		}

		return subcontroller;
	}
}

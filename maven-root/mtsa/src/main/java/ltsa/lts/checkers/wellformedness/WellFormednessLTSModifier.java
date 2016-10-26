package ltsa.lts.checkers.wellformedness;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.google.common.base.Preconditions;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.automaton.StateMachine;
import ltsa.lts.automata.automaton.transition.Transition;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.operations.composition.integrator.IntegratorEngine;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.Symbol;
import MTSTools.ac.ic.doc.mtstools.model.MTSConstants;

/**
 * Modifies the Labeled Transition System to check well formedness.<br/>
 * For each box that is not the box of interest, it injects the LTS associated
 * with the post condition of the box <br/>
 *
 */
public class WellFormednessLTSModifier {

	/**
	 * The suffix used to rename the machine of interest
	 */
	private static final String POST_CONDITION_SUFFIX = "_WITH_POST";

	/**
	 * The output used to write messages on the screen
	 */
	private LTSOutput output;

	/**
	 * 
	 * @param output
	 *            the output used to print messages
	 * @throws NullPointerException
	 *             if the output is null
	 */
	public WellFormednessLTSModifier(LTSOutput output) {
		Preconditions.checkNotNull(output, "The output cannot be null");
		this.output = output;
	}

	/**
	 * modifies the LTS lts to check its well formedness.<br/>
	 * 
	 * For each box that is not the box of interest, it injects the LTS
	 * associated with the post condition of the box <br/>
	 * 
	 * @param lts
	 *            is the LTS whose well-formedness must be verified
	 * @param mapBoxPostCondition
	 *            map each black box state to its corresponding LTS
	 * @param forPreconditionChecking
	 *            true if the obtained LTS must be used for precondition
	 *            checking
	 * @param boxOfInterest
	 *            the box of interest
	 * @return the LTS modified with post conditions
	 */
	public LabelledTransitionSystem modify(LabelledTransitionSystem lts,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition,
			boolean forPreconditionChecking, String boxOfInterest) {

		LabelledTransitionSystem returnLts = lts;
		boolean first = true;
		for (String box : lts.mapBoxInterface.keySet()) {

			if (forPreconditionChecking && boxOfInterest.equals(box)) {
				returnLts = this.processPre(returnLts, box, first,
						mapBoxPostCondition);
			} else {
				returnLts = this.processPostCondition(returnLts, box, first,
						mapBoxPostCondition);
			}
			first = false;
		}
		return returnLts;
	}

	private LabelledTransitionSystem processPre(
			LabelledTransitionSystem compiledProcess, String box,
			boolean first,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition) {

		LabelledTransitionSystem cs = null;

		if (mapBoxPostCondition.containsKey(box)) {
			Set<String> boxInterface = compiledProcess.mapBoxInterface.get(box);
			LabelledTransitionSystem boxInterfaceAutomaton = this
					.createInterfaceLTS(boxInterface);

			Vector<LabelledTransitionSystem> machines = new Vector<>();
			machines.add(boxInterfaceAutomaton);
			LabelledTransitionSystem machinePostCondition = mapBoxPostCondition
					.get(box);
			String postCondition = machinePostCondition.getName();

			machinePostCondition = processPost(compiledProcess, box,
					boxInterface, postCondition, mapBoxPostCondition);

			int newInitiatilState = machinePostCondition.addInitialState();

			int eventIndex = machinePostCondition.addEvent(LTLf2LTS.endSymbol
					.getValue());
			int endStateIndex = machinePostCondition.addNewState();
			machinePostCondition.addFinalStateIndex(endStateIndex);
			machinePostCondition.addTransition(endStateIndex, eventIndex,
					endStateIndex);

			int tauIndex = machinePostCondition.addEvent(MTSConstants.TAU);
			machinePostCondition.addTransition(newInitiatilState, tauIndex, 1);
			machinePostCondition.addTransition(0, eventIndex, endStateIndex);

			int boxPosition = compiledProcess.getBoxIndexes().get(box);
			cs = new IntegratorEngine().apply(compiledProcess, boxPosition,
					machinePostCondition);
			cs.setName(compiledProcess.getName() + POST_CONDITION_SUFFIX);

		} else {
			cs = this.addInterfaceSelfLoop(compiledProcess, box);
			int eventIndex = cs.addEvent(LTLf2LTS.endSymbol.getValue());
			int addedStateIndex = cs.addNewState();

			cs.addTransition(addedStateIndex, eventIndex, addedStateIndex);

			cs.addFinalStateIndex(addedStateIndex);
			int boxIndex = cs.getBoxIndexes().get(box);
			cs.addTransition(boxIndex, eventIndex, addedStateIndex);
		}

		if (cs != null) {
			compiledProcess = cs;
		}
		return compiledProcess;
	}

	private LabelledTransitionSystem addInterfaceSelfLoop(
			LabelledTransitionSystem compiledProcess, String box) {
		Set<String> boxInterface = compiledProcess.mapBoxInterface.get(box);
		Integer boxIndex = compiledProcess.getBoxIndexes().get(box);

		LTSTransitionList holdTransitions = compiledProcess
				.getTransitions(boxIndex);

		LTSTransitionList newTransitions = holdTransitions;
		for (String event : boxInterface) {
			compiledProcess.addEvent(event);
			int eventIndex = compiledProcess.getEvent(event);
			newTransitions = LTSTransitionList.addTransition(newTransitions,
					eventIndex, boxIndex);
		}

		compiledProcess.setState(boxIndex, newTransitions);
		return compiledProcess;
	}

	private LabelledTransitionSystem processPostCondition(
			LabelledTransitionSystem compiledProcess, String box,
			boolean first,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition) {
		output.outln("\t box: " + box);

		LabelledTransitionSystem cs = null;

		if (mapBoxPostCondition.containsKey(box)) {
			Set<String> boxInterface = compiledProcess.mapBoxInterface.get(box);
			LabelledTransitionSystem boxInterfaceAutomaton = this
					.createInterfaceLTS(boxInterface);

			Vector<LabelledTransitionSystem> machines = new Vector<>();
			machines.add(boxInterfaceAutomaton);
			LabelledTransitionSystem machinePostCondition = mapBoxPostCondition
					.get(box);
			String postCondition = machinePostCondition.getName();

			machinePostCondition = processPost(compiledProcess, box,
					boxInterface, postCondition, mapBoxPostCondition);

			int boxPosition = compiledProcess.getBoxIndexes().get(box);
			cs = new IntegratorEngine().apply(compiledProcess, boxPosition,
					machinePostCondition);
			if (first) {
				cs.setName(compiledProcess.getName() + POST_CONDITION_SUFFIX);
			}

		} else {
			cs = this.addInterfaceSelfLoop(compiledProcess, box);
		}

		if (cs != null) {
			compiledProcess = cs;
		}
		return compiledProcess;
	}

	private LabelledTransitionSystem processPost(
			LabelledTransitionSystem compiledProcess, String box,
			Set<String> boxInterface, String postCondition,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition)
			throws InternalError {
		LabelledTransitionSystem machinePostCondition;

		output.outln("\t \t post condition: " + postCondition);
		System.out.println("processing the post condition: " + postCondition);
		machinePostCondition = mapBoxPostCondition.get(box);

		Set<String> postConditionCharacters = new HashSet<>(
				machinePostCondition.getAlphabetCharacters());
		postConditionCharacters.remove("tau");
		postConditionCharacters.remove("@any");
		if (!boxInterface.containsAll(postConditionCharacters)) {

			postConditionCharacters.removeAll(boxInterface);
			Diagnostics.fatal("The actions " + postConditionCharacters
					+ " of the postcondition " + postCondition
					+ " are not contained in the interface of the box " + box);
		}
		if (!compiledProcess.mapBoxInterface.containsKey(box)) {
			Diagnostics.fatal("No interface provided for the box: " + box);
		}
		return machinePostCondition;
	}

	/**
	 * returns a LTS with a single state and a self-loop labeled with the events
	 * of the interface of the box
	 * 
	 * @param boxInterface
	 *            the set of the events in the interface of the box
	 * @return a LTS with a single state and a self-loop labeled with the events
	 *         of the interface of the box
	 * @throws NullPointerException
	 *             if the interface of the box is null
	 */
	private LabelledTransitionSystem createInterfaceLTS(Set<String> boxInterface) {

		
		StateMachine mc = new StateMachine(LTSCompiler.boxOfInterest
				+ "-interface");
		String initStateName = LTSCompiler.boxOfInterest + "-interface";
		mc.addState(initStateName);

		boxInterface.stream().forEach(mc::addEvent);
		boxInterface.stream().forEach(
				event -> mc.addTransition(new Transition(mc
						.getStateIndex(initStateName), new Symbol(event,
						Symbol.UPPERIDENT), mc.getStateIndex(initStateName))));
		return mc.makeCompactState();

	}
}

package ltsa.lts.checkers.wellformedness;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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

public class WellFormednessLTSModifier {

	private LTSOutput output;

	public WellFormednessLTSModifier(LTSOutput output) {
		this.output = output;
	}

	public LabelledTransitionSystem modify(LabelledTransitionSystem lts,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition, boolean forPreconditionChecking, String boxOfInterest) {

		LabelledTransitionSystem returnLts = lts;
		boolean first = true;
		for (String box : lts.mapBoxInterface.keySet()) {

			if (forPreconditionChecking
					&& boxOfInterest.equals(box)) {
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
					.createInterfaceCompactState(boxInterface);

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
			cs = new IntegratorEngine().apply(boxPosition, compiledProcess,
					machinePostCondition);
			cs.setName(compiledProcess.getName() + "_WITH_POST");

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
					.createInterfaceCompactState(boxInterface);

			Vector<LabelledTransitionSystem> machines = new Vector<>();
			machines.add(boxInterfaceAutomaton);
			LabelledTransitionSystem machinePostCondition = mapBoxPostCondition
					.get(box);
			String postCondition = machinePostCondition.getName();

			machinePostCondition = processPost(compiledProcess, box,
					boxInterface, postCondition, mapBoxPostCondition);
			// return machinePostCondition;
			// TOFIX
			// machines.add(machinePostCondition);

			int boxPosition = compiledProcess.getBoxIndexes().get(box);
			cs = new IntegratorEngine().apply(boxPosition, compiledProcess,
					machinePostCondition);
			cs.setName(compiledProcess.getName() + "_WITH_POST");

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
		System.out.println("processing the post condition: "+postCondition);
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
		// machines.add(machinePostCondition);
		// compiled.put(postCondition, machinePostCondition.myclone());
		return machinePostCondition;
	}

	private LabelledTransitionSystem createInterfaceCompactState(
			Set<String> boxInterface) {

		StateMachine mc = new StateMachine(LTSCompiler.boxOfInterest
				+ "-interface");
		String initStateName = LTSCompiler.boxOfInterest + "-interface";
		mc.addState(initStateName);
		

		boxInterface.stream().forEach(event -> mc.addEvent(event));
		boxInterface.stream().forEach(
				event -> mc.addTransition(new Transition(mc
						.getStateIndex(initStateName), new Symbol(event,
						Symbol.UPPERIDENT), mc.getStateIndex(initStateName))));
		return mc.makeCompactState();

	}
}

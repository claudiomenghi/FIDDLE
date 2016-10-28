package ltsa.lts.checkers.realizability;

import java.util.Vector;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.output.LTSOutput;
import ltsa.ui.EmptyLTSOuput;

/**
 * Contains the algorithm that checks the realizabilty.
 *
 */
public class RealizabilityChecker {

	/**
	 * The system to be considered
	 */
	private final CompositeState system;
	/**
	 * The formula to be considered
	 */
	private final CompositeState property;

	/**
	 * The negation of the formula to be considered
	 */
	private final CompositeState notProperty;

	/**
	 * The output used to print messages
	 */
	private final LTSOutput output;

	public RealizabilityChecker(CompositeState system, CompositeState property,
			CompositeState notProperty, LTSOutput output) {
		this.system = system;
		this.property = property;
		this.notProperty = notProperty;
		this.output = output;
	}

	public void check() {

		this.output
				.outln("**************** REALIZABILITY CHECKER ****************");

		this.output
				.outln("***** STEP 1: Checking whether C^B || E MODELS phi ");

		CompositeState systemCopy = system.clone();
		Vector<LabelledTransitionSystem> machines = systemCopy.getMachines();
		for (int i = 0; i < machines.size(); i++) {
			machines.get(i).removeStates(
					machines.get(i).getBoxIndexes().values());
		}

		boolean satisfied = systemCopy.checkLTL(output, property);
		if (!satisfied) {
			this.output
					.outln("---- REALIZABILITY RESULT: The controller is not realizable");
		} else {
			this.output
					.outln("---- REALIZABILITY RESULT: The controller is realizable");

		}
	}
}

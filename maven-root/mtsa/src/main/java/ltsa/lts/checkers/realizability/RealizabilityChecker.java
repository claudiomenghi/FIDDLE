package ltsa.lts.checkers.realizability;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.output.LTSOutput;

/**
 * Contains the algorithm that checks the realizability.
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

	/**
	 * Creates a new realizability checker
	 * 
	 * @param system
	 *            the system to be considered
	 * @param property
	 *            the property to be considered
	 * @param notProperty
	 *            the negation of the property to be considered
	 * @param output
	 *            the output used to print the result
	 */
	public RealizabilityChecker(CompositeState system, CompositeState property,
			CompositeState notProperty, LTSOutput output) {
		Preconditions.checkNotNull(system,
				"The system to be considered cannot be null");
		Preconditions.checkNotNull(property,
				"The property to be considered cannot be null");
		Preconditions.checkNotNull(notProperty,
				"The negation of the property to be considered cannot be null");
		Preconditions.checkNotNull(output,
				"The output used to print the result cannot be null");

		this.system = system;
		this.property = property;
		this.notProperty = notProperty;
		this.output = output;
	}

	/**
	 * runs the realizability checker
	 */
	public void check() {

		this.output
				.outln("**************** REALIZABILITY CHECKER ****************");

		this.output
				.outln("***** STEP 1: Checking whether C^B || E MODELS phi ");

		CompositeState systemCopy = system.clone();
		List<LabelledTransitionSystem> machines = new ArrayList<>(
				systemCopy.getMachines());
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
					.outln("***** STEP 2: Checking whether C || E NOT MODELS phi ");
			CompositeState secondSystemCopy = modifyController();

			boolean secondCheckSatisfied = secondSystemCopy.checkLTL(output,
					notProperty);
			if (secondCheckSatisfied) {
				this.output
						.outln("---- REALIZABILITY RESULT: The controller is not realizable");
			} else {
				this.output
						.outln("---- REALIZABILITY RESULT: The controller could be realizable");
			}
		}
	}

	private CompositeState modifyController() {
		CompositeState secondSystemCopy = system.clone();

		List<LabelledTransitionSystem> secondMachines = new ArrayList<>(
				secondSystemCopy.getMachines());
		for (int i = 0; i < secondMachines.size(); i++) {

			LabelledTransitionSystem machine = secondMachines.get(i);

			for (String boxName : machine.getBoxes()) {
				for (String event : machine.getBoxInterface(boxName)) {
					machine.addTransition(machine.getBoxIndexes().get(boxName),
							machine.addEvent(event), machine.getBoxIndexes()
									.get(boxName));
				}
			}
		}
		return secondSystemCopy;
	}
}

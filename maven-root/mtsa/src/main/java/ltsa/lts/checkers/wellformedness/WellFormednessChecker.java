package ltsa.lts.checkers.wellformedness;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.output.LTSOutput;
import ltsa.ui.EmptyLTSOuput;

public class WellFormednessChecker {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private final LTSOutput output;

	private final LabelledTransitionSystem controller;

	private LabelledTransitionSystem updatedController;

	private final CompositeState environment;

	private final String boxName;

	private final Formula precondition;
	private final String preconditionName;

	public WellFormednessChecker(LTSOutput output, CompositeState environment, LabelledTransitionSystem controller,
			String boxName, Formula precondition, String preconditionName) {

		Preconditions.checkNotNull(controller, "The controller cannot be null");
		Preconditions.checkNotNull(environment, "The environment cannot be null");

		Preconditions.checkArgument(controller.getBoxes().contains(boxName),
				"The box " + boxName + " must be a box of the controller");
		output.outln("*********************************************************");
		output.outln("Running the well-formedness checker.\n ENVIRONMENT: " + environment.getName() + "\n CONTROLLER: "
				+ controller.getName() + "\n PRECONDITION: " + preconditionName);

		this.environment = environment;
		this.boxName = boxName;
		this.output = output;
		this.controller = controller;
		this.precondition = precondition;
		this.preconditionName = preconditionName;
	}

	public LabelledTransitionSystem getModifiedController() {
		return this.updatedController;
	}

	public CompositeState check() {

		// implements the step 1, 2, 3 of the well-formedness checking
		// algorithm, i.e., it returns the modified controller
		updatedController = new WellFormednessLTSModifier(output).modify(controller, boxName);

		logger.debug(updatedController);
		logger.debug(updatedController.getName() + "\t " + updatedController.getEndOfSequenceIndex());
		// STEP 4
		output.outln("\t APPLYING STEP4. ");
		logger.debug("APPLYING STEP4");
		output.outln("\t \t translating the formula: " + preconditionName);

		Set<String> alphabet = new HashSet<>();
		alphabet.addAll(environment.getAlphabetEvents());
		alphabet.addAll(controller.getAlphabetEvents());

		// modifies the property
		CompositeState property = new LTLf2LTS().toPropertyWithNoInit(precondition, this.output, alphabet,
				preconditionName);

		CompositeState system = new CompositeState("System");
		environment.getMachines().stream().forEach(system::addMachine);

		environment.getMachines().stream().forEach(logger::debug);

		environment.getMachines().stream()
				.forEach(machine -> logger.debug(machine.getName() + "\t " + machine.getEndOfSequenceIndex()));
		system.addMachine(updatedController);

		output.outln("\t \t checking...");
		boolean result = system.checkLTL(new EmptyLTSOuput(), property);

		if (result) {
			output.outln("RESULT: all the valid traces satisfy the precondition " + preconditionName);
		} else {
			output.outln("RESULT: one of the valid traces violates the precondition " + preconditionName);
			output.outln(system.getErrorTrace().toString());
			if (property.getFluentTracer() != null) {
				output.outln("Violating trace fluents:");
				property.getFluentTracer().print(output, system.getErrorTrace(), true);
			}
		}
		return system;
	}

}

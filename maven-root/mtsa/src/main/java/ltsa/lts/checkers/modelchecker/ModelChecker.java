package ltsa.lts.checkers.modelchecker;

import javax.annotation.Nonnull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.output.LTSOutput;
import ltsa.ui.EmptyLTSOuput;

import com.google.common.base.Preconditions;

/**
 * Contains the model checker. A model checker verifies if the controller in
 * parallel with the environment ensures the satisfaction of the property of
 * interest
 *
 */
public class ModelChecker {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The environment to be considered
	 */
	private final CompositeState environment;

	/**
	 * The controller to be considered
	 */
	private final CompositeState controller;

	/**
	 * The property to be considered
	 */
	private final CompositeState ltlProperty;

	/**
	 * The output to be printed
	 */
	private final LTSOutput output;

	private CompositeState modifiedController;

	/**
	 * Creates the model checker
	 * 
	 * @param output
	 *            the output to be used to print the results
	 * @param environment
	 *            the environment of the system
	 * @param controller
	 *            the controller of the system
	 * @param property
	 *            the composite state associated with the property to be
	 *            verified
	 * @throws NullPointerException
	 *             if one of the parameters is null
	 */
	public ModelChecker(@Nonnull LTSOutput output, @Nonnull CompositeState environment,
			@Nonnull CompositeState controller, @Nonnull CompositeState property) {

		Preconditions.checkNotNull(output, "The output of the system cannot be null");
		Preconditions.checkNotNull(environment, "The environment cannot be null");
		Preconditions.checkNotNull(controller, "The controller cannot be null");
		Preconditions.checkNotNull(property, "The property of interest cannot be null");

		output.outln("*********************************************************");
		output.outln("Running the model checker.\n" + "\t ENVIRONMENT: " + environment.getName() + "\n"
				+ "\t CONTROLLER: " + controller.getName() + "\n" + "\t PROPERTY: " + property.getName());
		output.outln("*********************************************************");

		this.output = output;
		this.environment = environment;
		this.controller = controller;
		this.ltlProperty = property;
	}

	/**
	 * It is used to return the modified controller. It must be called after the
	 * check method
	 * 
	 * @return a modified version of the controller
	 */
	public CompositeState getModifiedController() {
		return modifiedController;
	}

	/**
	 * runs the realizability checker
	 */
	public CompositeState check() {
		environment.compose(new EmptyLTSOuput());
		LabelledTransitionSystem controllerLTS = controller.getMachines().get(0);

		LabelledTransitionSystem modifiedController = new ModelCheckerLTSModifier(this.output).modify(controllerLTS);
		
		logger.debug("The controller works on the alphabet: "+modifiedController.getAlphabet());
		this.modifiedController = new CompositeState(modifiedController.getName());
		this.modifiedController.addMachine(modifiedController);

		CompositeState system = new CompositeState("System");
		environment.getMachines().stream().forEach(system::addMachine);
		system.addMachine(modifiedController);

		boolean result = system.checkLTL(new EmptyLTSOuput(), ltlProperty);

		output.outln("*********************************************************");
		if (result) {
			output.outln("RESULT: the property of interest is satisfied");
		} else {
			output.outln("RESULT: the property of interest is violated");
			output.outln("Violating trace events:");
			output.outln(system.getErrorTrace().toString());
			if (ltlProperty.getFluentTracer() != null) {
				output.outln("Violating trace fluents:");
				ltlProperty.getFluentTracer().print(output, system.getErrorTrace(), true);
			}
		}
		output.outln("*********************************************************");

		return system;

	}
}

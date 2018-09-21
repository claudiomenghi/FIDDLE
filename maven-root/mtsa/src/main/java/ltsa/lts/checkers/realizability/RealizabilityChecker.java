package ltsa.lts.checkers.realizability;

import javax.annotation.Nonnull;

import org.apache.commons.logging.LogFactory;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.output.LTSOutput;
import ltsa.ui.EmptyLTSOuput;

import com.google.common.base.Preconditions;

/**
 * Contains the algorithm that checks the realizability.
 *
 */
public class RealizabilityChecker {
	protected final org.apache.commons.logging.Log logger = LogFactory.getLog(getClass());
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
	 * The property to be considered
	 */
	private final CompositeState notProperty;
	
	private  CompositeState system;

	/**
	 * The output to be printed
	 */
	private final LTSOutput output;

	private LabelledTransitionSystem modifiedControllerLTSStep1;

	private LabelledTransitionSystem modifiedControllerLTSStep2;

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
	public RealizabilityChecker(@Nonnull LTSOutput output,
			@Nonnull CompositeState environment,
			@Nonnull CompositeState controller,
			@Nonnull CompositeState property,
			@Nonnull CompositeState notproperty) {
		Preconditions.checkNotNull(output,
				"The output of the system cannot be null");
		Preconditions.checkNotNull(environment,
				"The environment cannot be null");
		Preconditions.checkNotNull(controller, "The controller cannot be null");
		Preconditions.checkNotNull(property,
				"The property of interest cannot be null");

		output.outln("*********************************************************");
		output.outln("****************  REALIZABILITY CHECKER  ****************");
		output.outln("*********************************************************");
		output.outln("ENVIRONMENT: "+ environment.getName() + "\n" + 
					 "CONTROLLER: "+ controller.getName() + "\n" + 
					 "PROPERTY: "+ property.getName());


		this.output = output;
		this.environment = environment;
		this.controller = controller;
		this.ltlProperty = property;
		this.notProperty = notproperty;
	}

	public LabelledTransitionSystem getModifiedControllerStep1() {
		return this.modifiedControllerLTSStep1;
	}


	public LabelledTransitionSystem getModifiedControllerStep2() {
		return this.modifiedControllerLTSStep2;
	}
	/**
	 * runs the realizability checker
	 */
	public void check() {



		this.output
				.outln("***** STEP 1: Checking whether C^B || E |= phi ");

		modifiedControllerLTSStep1 = controller.getMachines().get(0).clone();
		modifiedControllerLTSStep1.setName("STEP_1_"+controller.getName());

		modifiedControllerLTSStep1.removeStates(modifiedControllerLTSStep1
				.getBoxIndexes().values());

		system = new CompositeState("system");
		environment.getMachines().forEach(system::addMachine);
		system.addMachine(modifiedControllerLTSStep1);

		logger.debug("STEP 1: Checking whether C^B || E |= phi");
		
		boolean satisfied;
		if(!system.getAlphabetEvents().containsAll(ltlProperty.getAlphabetEvents())){
			satisfied=true;
		}
		else{
		 satisfied = system.checkLTL(this.output, ltlProperty);
		}
		if (!satisfied) {
			this.output.outln("Counterexample found: ");
			this.output.outln(system.getErrorTrace().toString());
			this.output
					.outln("---- REALIZABILITY RESULT: The controller is not realizable");
		} else {
			this.output.outln("No counterexample found. ");
			this.output
					.outln("***** STEP 2: Checking whether C || E  |=  NOT phi ");
			logger.debug("STEP 2: Checking whether C || E  |=  NOT phi");
			modifiedControllerLTSStep2 = modifyController();
			modifiedControllerLTSStep2.setName("STEP_2_"+controller.getName());

			system = new CompositeState("system");
			environment.getMachines().forEach(system::addMachine);
			system.addMachine(modifiedControllerLTSStep2);

			boolean secondCheckSatisfied = system.checkLTL(this.output, notProperty);
			if (secondCheckSatisfied) {
				this.output.outln("No counterexample found. ");
				this.output
						.outln("---- REALIZABILITY RESULT: The controller is not realizable");
			} else {
				this.output.outln("Counterexample found. ");
				notProperty.getFluentTracer().print(output, system.getErrorTrace(), true);
				this.output
						.outln("---- REALIZABILITY RESULT: The controller could be realizable");
			}
		}
	}

	private LabelledTransitionSystem modifyController() {
		LabelledTransitionSystem modifiedControllerLTSStep2 = controller
				.getMachines().get(0).clone();

		for (String boxName : modifiedControllerLTSStep2.getBoxes()) {
			for (String event : modifiedControllerLTSStep2
					.getBoxInterface(boxName)) {
				modifiedControllerLTSStep2
						.addTransition(
								modifiedControllerLTSStep2.getBoxIndexes().get(
										boxName),
								modifiedControllerLTSStep2.addEvent(event),
								modifiedControllerLTSStep2.getBoxIndexes().get(
										boxName));
			}
		}
		return modifiedControllerLTSStep2;
	}
	
	public CompositeState getSystem(){
		return this.system;
	}
}

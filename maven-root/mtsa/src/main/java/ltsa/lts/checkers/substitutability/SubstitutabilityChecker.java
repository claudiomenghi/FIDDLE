package ltsa.lts.checkers.substitutability;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.PostconditionDefinition;
import ltsa.lts.ltl.PreconditionDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.operations.composition.sequential.SequentialCompositionEngine;
import ltsa.lts.operations.minimization.Minimiser;
import ltsa.lts.output.LTSOutput;
import ltsa.ui.EmptyLTSOuput;

import org.apache.commons.logging.LogFactory;

public class SubstitutabilityChecker {

	/** Logger available to subclasses */
	protected final org.apache.commons.logging.Log logger = LogFactory
			.getLog(getClass());

	/**
	 * The environment to be considered
	 */
	private final CompositeState environment;

	private final LabelledTransitionSystem subController;
	private final Formula precondition;

	private final Formula postCondition;

	private final String preconditionName;
	private final String postconditionName;

	private final LTSOutput ltsOutput;

	private LabelledTransitionSystem preConditionLTS;
	private CompositeState postConditionState;
	private LabelledTransitionSystem environmentParallelPrePlusReplacementLTS;

	private LabelledTransitionSystem preconditionPlusReplacement;

	/**
	 * Creates the model checker
	 * 
	 * @param output
	 *            the output to be used to print the results
	 * @param environment
	 *            the environment of the system
	 * @param subController
	 *            the sub-controller to be considered
	 * @param postcondition
	 *            the post-condition that must be ensured by the sub-controller
	 * @param precondition
	 *            the pre-condition of the sub-controller
	 * @throws NullPointerException
	 *             if one of the parameters is null
	 */
	public SubstitutabilityChecker(LTSOutput ltsOutput,
			CompositeState environment, LabelledTransitionSystem subController,
			PreconditionDefinition precondition,
			PostconditionDefinition postcondition) {

		this.ltsOutput = ltsOutput;
		this.environment = environment;
		this.subController = subController;
		this.precondition = precondition.getFormula(true);
		this.preconditionName = precondition.getName();
		this.postCondition = postcondition.getFormula(false);
		this.postconditionName = postcondition.getName();

		ltsOutput
				.outln("*********************************************************");
		ltsOutput
				.outln("*************  SUBSTITUTABILITY- CHECKER  ***************");
		ltsOutput
				.outln("*********************************************************");
		ltsOutput.outln("ENVIRONMENT: " + environment.getName() + "\n"
				+ "SUB-CONTROLLER: " + subController.getName() + "\n"
				+ "\t PRECONDITION: " + precondition.getName() + "\n"
				+ "POSTCONDITION: " + postcondition.getName());
		ltsOutput
				.outln("*********************************************************");
	}

	public LabelledTransitionSystem getEnvironmentParallelPrePlusReplacement() {
		return this.environmentParallelPrePlusReplacementLTS;
	}

	public LabelledTransitionSystem getPostConditionLTS() {
		return this.postConditionState.getComposition();
	}

	public LabelledTransitionSystem getPreconditionLTS() {
		return this.preConditionLTS;
	}

	public LabelledTransitionSystem getPreconditionPlusReplacementLTS() {
		return this.preconditionPlusReplacement;
	}

	public void check() {

		this.ltsOutput
				.outln("STEP 1: integrating the precondition and the sub-controller");

		logger.debug("SUB-CONTROLLER: " + subController.getName());
		logger.debug("Transforming the precondition: " + preconditionName
				+ " in a LTS");
		step1();

		// compute the composition between the
		// preconditionPlusReplacement
		// and the environment

		this.ltsOutput.outln("STEP 2: changing the post-condition");
		CompositeState environmentParallelPrePlusReplacement = step2();

		this.ltsOutput.outln("STEP 3: model checking");
		step3(environmentParallelPrePlusReplacement);

	}

	private CompositeState step2() {
		Vector<LabelledTransitionSystem> machines = new Vector<>();

		machines.addAll(environment.getMachines());

		machines.add(preconditionPlusReplacement);
		CompositeState environmentParallelPrePlusReplacement = new CompositeState(
				machines);

		this.ltsOutput.outln("Processing the post-condition");
		logger.debug("Processing the post-condition");

		final Set<String> systemAlphabet = new HashSet<>();

		environmentParallelPrePlusReplacement.getMachines().forEach(
				m -> systemAlphabet.addAll(m.getAlphabetEvents()));

		this.postConditionState = this
				.compilePostConditionForReplacementChecking(
						new EmptyLTSOuput(), systemAlphabet);
		this.postConditionState.setName("MODIFIED_POST_"
				+ this.postconditionName);
		return environmentParallelPrePlusReplacement;
	}

	private void step3(CompositeState environmentParallelPrePlusReplacement) {
		Vector<LabelledTransitionSystem> postCondition = new Vector<>();
		postCondition.add(this.postConditionState.getComposition());
		environmentParallelPrePlusReplacement.compose(new EmptyLTSOuput());

		final StringBuilder machineList = new StringBuilder();
		environmentParallelPrePlusReplacement.getMachines().forEach(
				machine -> machineList.append(machine.getName() + "\t events "
						+ machine.getAlphabetEvents() + "\n"));
		this.logger.debug("SYSTEM MACHINES: " + machineList.toString());

		Set<String> events = new HashSet<>();
		environmentParallelPrePlusReplacement.getMachines().forEach(
				machine -> events.addAll(machine.getAlphabetEvents()));
		this.logger.debug("SYSTEM MACHINES Alphabet: " + events.toString());

		Minimiser min = new Minimiser(
				environmentParallelPrePlusReplacement.getComposition(),
				new EmptyLTSOuput());

		CompositeState newEnvironmentParallelPrePlusReplacement = new CompositeState(
				environmentParallelPrePlusReplacement.getName());
		newEnvironmentParallelPrePlusReplacement.addMachine(min.minimise());
		newEnvironmentParallelPrePlusReplacement.compose(new EmptyLTSOuput());
		boolean result = environmentParallelPrePlusReplacement.checkLTL(
				ltsOutput, postConditionState);
		if (result) {
			this.ltsOutput.outln("The post-condition is satisfied");
		} else {
			this.ltsOutput.outln("The post-condition is violated");
			try{
			this.ltsOutput.outln(environmentParallelPrePlusReplacement
					.getErrorTrace().toString());
			postConditionState.getFluentTracer()
					.print(this.ltsOutput,
							environmentParallelPrePlusReplacement
									.getErrorTrace(), true);
			}
			catch(Exception e){
				
			}

		}

		environmentParallelPrePlusReplacementLTS = environmentParallelPrePlusReplacement
				.getComposition();
		environmentParallelPrePlusReplacementLTS
				.setName("PARALLEL_COMPOSITION");
	}

	private void step1() {
		// transform pre-condition in LTS
		preConditionLTS = this.transformPreconditioninLTS(
				environment.getAlphabetEvents(), precondition, "PRE_"
						+ this.preconditionName);

		logger.debug("Precondition states: "
				+ preConditionLTS.getStates().length + " transitions: "
				+ preConditionLTS.getTransitionNumber());

		logger.debug("Integrating the pre-condition and the sub-controller");

		// modifies the sub-controller
		Set<String> environmentAlphabet = new HashSet<>();
		environment.getMachines().forEach(
				machine -> environmentAlphabet.addAll(machine
						.getAlphabetEvents()));

		environmentAlphabet.forEach(event -> {
			if (event != null
					&& !subController.getAlphabetEvents().contains(event)) {
				int eventIndex = subController.addEvent(event);
				for (int i = 0; i < subController.getStates().length; i++) {
					subController.addTransition(i, eventIndex, i);
				}
			}
		});

		// integrating the post-condition and the replacement
		preconditionPlusReplacement = new SequentialCompositionEngine().apply(
				LTLf2LTS.initSymbol.getValue(), preConditionLTS, subController);

		preconditionPlusReplacement.setName("SUBCONTROLLER_WITH_PRE");
		int endEventIndex = preconditionPlusReplacement
				.addEvent(LTLf2LTS.endSymbol.getValue());

		logger.debug("Adding end-self loops on the final states of the automaton");
		// add end selfLoopTransition
		preconditionPlusReplacement.getFinalStateIndexes().forEach(
				index -> preconditionPlusReplacement.addTransition(index,
						endEventIndex, index));

		logger.debug("Size of the obtained sub-controller: "
				+ preconditionPlusReplacement.size());

		this.ltsOutput
				.outln("Computing the sequential composition between the precondition and the sub-controller");
	}

	private LabelledTransitionSystem transformPreconditioninLTS(
			Set<String> environmentEvents, Formula precondition,
			String preconditionName) {
		// transform the precondition in an automaton

		return new LTLf2LTS().toLTSForPostChecking(precondition,
				new EmptyLTSOuput(), environmentEvents, preconditionName);
	}

	private CompositeState compilePostConditionForReplacementChecking(
			LTSOutput output, Set<String> alphabetCharacters) {

		return new LTLf2LTS().toPropertyWithInit(output, this.postCondition,
				alphabetCharacters, postconditionName);
	}
}

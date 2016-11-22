package ltsa.lts.checkers.substitutability;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.operations.composition.sequential.SequentialCompositionEngine;
import ltsa.lts.output.LTSOutput;
import ltsa.ui.EmptyLTSOuput;

public class SubstitutabilityChecker {

	private final LabelledTransitionSystem environment;
	private final LabelledTransitionSystem subController;
	private final Formula precondition;

	private final Formula postCondition;

	private final String preconditionName;
	private final String postconditionName;

	private final LTSOutput ltsOutput;

	private LabelledTransitionSystem preConditionLTS;
	private LabelledTransitionSystem postConditionLTS;
	private LabelledTransitionSystem environmentParallelPrePlusReplacementLTS;

	private LabelledTransitionSystem preconditionPlusReplacement;

	public SubstitutabilityChecker(LabelledTransitionSystem environment,
			LabelledTransitionSystem subController, Formula precondition,
			String preconditionName, Formula postCondition,
			String postconditionName, LTSOutput ltsOutput) {
		this.environment = environment;
		this.subController = subController;
		this.precondition = precondition;
		this.ltsOutput = ltsOutput;
		this.postCondition = postCondition;
		this.preconditionName = preconditionName;
		this.postconditionName = postconditionName;
	}

	public LabelledTransitionSystem getEnvironmentParallelPrePlusReplacement() {
		return this.environmentParallelPrePlusReplacementLTS;
	}

	public LabelledTransitionSystem getPostConditionLTS() {
		return this.postConditionLTS;
	}

	public LabelledTransitionSystem getPreconditionLTS() {
		return this.preConditionLTS;
	}

	public LabelledTransitionSystem getPreconditionPlusReplacementLTS() {
		return this.preconditionPlusReplacement;
	}

	public void check() {

		this.ltsOutput.outln("SUB-CONTROLLER: " + subController.getName()
				+ " transformed into a compact state");

		// transform pre-condition in LTS
		preConditionLTS = transformPreconditioninLTS(environment, precondition,
				this.postconditionName);

		System.out.println("precondition states: "
				+ preConditionLTS.getStates().length + " transitions: "
				+ preConditionLTS.getTransitionNumber());

		// integrating the post-condition and the replacement
		preconditionPlusReplacement = new SequentialCompositionEngine().apply(
				LTLf2LTS.initSymbol.getValue(), preConditionLTS, subController);

		int endEventIndex = preconditionPlusReplacement
				.addEvent(LTLf2LTS.endSymbol.getValue());
		// add end selfLoopTransition
		preconditionPlusReplacement.getFinalStateIndexes().forEach(
				index -> preconditionPlusReplacement.addTransition(index,
						endEventIndex, index));

		System.out.println("pre plus replacement states: "
				+ preconditionPlusReplacement.getStates().length
				+ " transitions: "
				+ preconditionPlusReplacement.getTransitionNumber());
		System.out.println("init state outgoing: "
				+ preconditionPlusReplacement.getTransitions(0).getEvent()
				+ " transitions: "
				+ preconditionPlusReplacement.getTransitionNumber());

		this.ltsOutput
				.outln("MACHINE: "
						+ preconditionPlusReplacement.getName()
						+ " of the sequential composition between the precondition and the replacement loaded");

		System.out.println("environment states: "
				+ environment.getStates().length + " transitions: "
				+ environment.getTransitionNumber());

		// compute the composition between the
		// preconditionPlusReplacement
		// and the environment

		Vector<LabelledTransitionSystem> machines = new Vector<>();
		machines.add(preconditionPlusReplacement);
		machines.add(environment);
		CompositeState environmentParallelPrePlusReplacement = new CompositeState(
				machines);
		environmentParallelPrePlusReplacement.compose(this.ltsOutput);

		System.out.println(postCondition);
		CompositeState ltlPostCondition = this
				.compilePostConditionForReplacementChecking(
						this.ltsOutput,
						new HashSet<String>(Arrays
								.asList(environmentParallelPrePlusReplacement
										.getComposition().getAlphabet())),
						postCondition, this.postconditionName);
		ltlPostCondition.setName(this.postconditionName);

		ltlPostCondition.compose(new EmptyLTSOuput());
		this.postConditionLTS = ltlPostCondition.getComposition();

		String environmentSize = "environment states:"
				+ environment.getStates().length + " transitions: "
				+ environment.getTransitionNumber();

		String controllerSize = "partial controller states:"
				+ preconditionPlusReplacement.getStates().length
				+ "  transitions: "
				+ preconditionPlusReplacement.getTransitionNumber();
		String propertySize = "property states: "
				+ ltlPostCondition.getComposition().getStates().length
				+ " property transitions: "
				+ ltlPostCondition.getComposition().getTransitionNumber();

		System.out.println(environmentSize + "\t" + controllerSize + "\t"
				+ propertySize);

		this.ltsOutput.outln("POST-CONDTION: " + this.postconditionName
				+ " transformed into a compact state");

		environmentParallelPrePlusReplacement.checkLTL(// this.ltsOutput,
				new EmptyLTSOuput(), ltlPostCondition);

		environmentParallelPrePlusReplacementLTS = environmentParallelPrePlusReplacement
				.getComposition();

	}

	private LabelledTransitionSystem transformPreconditioninLTS(
			LabelledTransitionSystem environment, Formula precondition,
			String preconditionName) {
		// transform the precondition in an automaton

		Set<String> ltsAlphabet = new HashSet<>(environment.getAlphabetEvents());
		LabelledTransitionSystem preConditionLTS = new LTLf2LTS()
				.toLTSForPostChecking(precondition, new EmptyLTSOuput(),
						ltsAlphabet, preconditionName);

		return preConditionLTS;
	}

	private CompositeState compilePostConditionForReplacementChecking(
			LTSOutput output, Set<String> alphabetCharacters,
			Formula postConditionFormula, String postconditionName) {

		return new LTLf2LTS().toPropertyWithInit(postConditionFormula, output,
				alphabetCharacters, postconditionName);
	}
}

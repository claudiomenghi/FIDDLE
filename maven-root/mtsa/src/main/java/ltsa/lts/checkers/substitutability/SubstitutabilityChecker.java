package ltsa.lts.checkers.substitutability;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.LogFactory;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.operations.composition.sequential.SequentialCompositionEngine;
import ltsa.lts.output.LTSOutput;
import ltsa.ui.EmptyLTSOuput;

public class SubstitutabilityChecker {

	/** Logger available to subclasses */
	protected final org.apache.commons.logging.Log logger = LogFactory.getLog(getClass());

	/**
	 * The environment to be considered
	 */
	private final CompositeState environment;

	private final LabelledTransitionSystem subComponent;
	private final Formula precondition;

	private final Formula postCondition;

	private final String preconditionName;
	private final String postconditionName;

	private final LTSOutput ltsOutput;

	private LabelledTransitionSystem preConditionLTS;
	private CompositeState postConditionState;


	private LabelledTransitionSystem preconditionPlusSubcomponent;
	
	private boolean result;
	private int resultingcomponentsize;
	private long effectiveCheckingtime;
	
	public int getResultComponentSize(){
		return this.resultingcomponentsize;
	}
	
	public boolean getResult(){
		return this.result;
	}
	
	public CompositeState getPostCondition(){
		return postConditionState;
	}
	
	public long getEffectiveCheckingTime(){
		return effectiveCheckingtime;
	}

	/**
	 * Creates the model checker
	 * 
	 * @param output
	 *            the output to be used to print the results
	 * @param environment
	 *            the environment of the system
	 * @param subComponent
	 *            the sub-controller to be considered
	 * @param postcondition
	 *            the post-condition that must be ensured by the sub-controller
	 * @param precondition
	 *            the pre-condition of the sub-controller
	 * @throws NullPointerException
	 *             if one of the parameters is null
	 */
	public SubstitutabilityChecker(LTSOutput ltsOutput, CompositeState environment,
			LabelledTransitionSystem subComponent, Formula precondition, String preconditionName, Formula postcondition,
			String postconditionName) {

		this.ltsOutput = ltsOutput;
		this.environment = environment;
		this.subComponent = subComponent;
		this.precondition = precondition;
		this.preconditionName = preconditionName;
		this.postCondition = postcondition;
		this.postconditionName = postconditionName;

		ltsOutput.outln("*********************************************************");
		ltsOutput.outln("*************  SUBSTITUTABILITY- CHECKER  ***************");
		ltsOutput.outln("*********************************************************");
		ltsOutput.outln("ENVIRONMENT: " + environment.getName() + "\n" + "SUB-CONTROLLER: " + subComponent.getName()
				+ "\n" + "\t PRECONDITION: " + preconditionName + "\n" + "POSTCONDITION: " + postconditionName);
		ltsOutput.outln("*********************************************************");
	}

	

	public LabelledTransitionSystem getPostConditionLTS() {
		return this.postConditionState.getComposition();
	}

	public LabelledTransitionSystem getPreconditionLTS() {
		return this.preConditionLTS;
	}

	public LabelledTransitionSystem getPreconditionPlusSubcomponentLTS() {
		return this.preconditionPlusSubcomponent;
	}

	public void check() {

		this.ltsOutput.outln("STEP 1: integrating the precondition and the sub-controller");

		logger.debug("SUB-CONTROLLER: " + subComponent.getName());
		logger.debug("Transforming the precondition: " + preconditionName + " in a LTS");
		step1();

		// compute the composition between the
		// preconditionPlusReplacement
		// and the environment

		this.ltsOutput.outln("STEP 2: changing the post-condition");
		CompositeState environmentParallelPrePlusReplacement = step2();
		logger.debug("End of Step 2");
		
		this.ltsOutput.outln("STEP 3: model checking");
		this.step3(environmentParallelPrePlusReplacement);
		logger.debug("End of Step 3");
		

	}

	private CompositeState step2() {
		Vector<LabelledTransitionSystem> machines = new Vector<>();

		machines.addAll(environment.getMachines());

		machines.add(preconditionPlusSubcomponent);
		CompositeState environmentParallelPrePlusReplacement = new CompositeState(machines);

		this.ltsOutput.outln("Processing the post-condition");
		logger.debug("Processing the post-condition");

		final Set<String> systemAlphabet = new HashSet<>();

		environmentParallelPrePlusReplacement.getMachines().forEach(m -> systemAlphabet.addAll(m.getAlphabetEvents()));

		this.postConditionState = this.compilePostConditionForReplacementChecking(new EmptyLTSOuput(), systemAlphabet);
		this.postConditionState.setName("MODIFIED_POST_" + this.postconditionName);
		return environmentParallelPrePlusReplacement;
	}

	private void step3(CompositeState environmentParallelPrePlusReplacement) {
		//Vector<LabelledTransitionSystem> postCondition = new Vector<>();
	//	postCondition.add(this.postConditionState.getComposition());
		//environmentParallelPrePlusReplacement.compose(new EmptyLTSOuput());

		final StringBuilder machineList = new StringBuilder();
		environmentParallelPrePlusReplacement.getMachines().forEach(
				machine -> machineList.append(machine.getName() + "\t events " + machine.getAlphabetEvents() + "\n"));
		this.logger.debug("SYSTEM MACHINES: " + machineList.toString());

		Set<String> events = new HashSet<>();
		environmentParallelPrePlusReplacement.getMachines()
				.forEach(machine -> events.addAll(machine.getAlphabetEvents()));
		this.logger.debug("SYSTEM MACHINES Alphabet: " + events.toString());


		long subinit = System.currentTimeMillis();
		boolean result = environmentParallelPrePlusReplacement.checkLTL(ltsOutput, postConditionState);
		long  subend = System.currentTimeMillis();

		this.effectiveCheckingtime=subend-subinit;
		this.result=result;
		if (result) {
			this.ltsOutput.outln("The post-condition is satisfied");
		} else {
			this.ltsOutput.outln("The post-condition is violated");
			try {
				this.ltsOutput.outln(environmentParallelPrePlusReplacement.getErrorTrace().toString());
				postConditionState.getFluentTracer().print(this.ltsOutput,
						environmentParallelPrePlusReplacement.getErrorTrace(), true);
			} catch (Exception e) {

			}
		}
	}

	private void step1() {
		// transform pre-condition in LTS
		preConditionLTS = this.transformPreconditioninLTS(environment.getAlphabetEvents(), precondition,
				"PRE_" + this.preconditionName);

		logger.debug("Precondition states: " + preConditionLTS.getStates().length + " transitions: "
				+ preConditionLTS.getTransitionNumber());

		logger.debug("Integrating the pre-condition and the sub-controller");

		// modifies the sub-controller
		Set<String> environmentAlphabet = new HashSet<>();
		environment.getMachines().forEach(machine -> environmentAlphabet.addAll(machine.getAlphabetEvents()));

		logger.debug("Subcomponent end state index: "+subComponent.getEndOfSequenceIndex() );
		environmentAlphabet.forEach(event -> {
			if (event != null && !subComponent.getAlphabetEvents().contains(event)) {
				int eventIndex = subComponent.addEvent(event);
				for (int i = 0; i < subComponent.getStates().length; i++) {
					if (!subComponent.getFinalStateIndexes().contains(i)) {
						subComponent.addTransition(i, eventIndex, i);
					}
				}
			}
		});

		// integrating the post-condition and the replacement
		preconditionPlusSubcomponent = new SequentialCompositionEngine().apply(LTLf2LTS.initSymbol.getValue(),
				preConditionLTS, subComponent);

		preconditionPlusSubcomponent.setName("SUBCONTROLLER_WITH_PRE");
		int endEventIndex = preconditionPlusSubcomponent.addEvent(LTLf2LTS.endSymbol.getValue());

		logger.debug("Adding end-self loops on the final states of the automaton");
		// add end selfLoopTransition
		preconditionPlusSubcomponent.getFinalStateIndexes()
				.forEach(index -> preconditionPlusSubcomponent.addTransition(index, endEventIndex, index));

		logger.debug("Size of the obtained sub-component: " + preconditionPlusSubcomponent.size());

		resultingcomponentsize=preconditionPlusSubcomponent.size();
		this.ltsOutput.outln("Computing the sequential composition between the precondition and the sub-controller");
		
	}

	private LabelledTransitionSystem transformPreconditioninLTS(Set<String> environmentEvents, Formula precondition,
			String preconditionName) {
		// transform the precondition in an automaton

		return new LTLf2LTS().toLTSForPostChecking(precondition, new EmptyLTSOuput(), environmentEvents,
				preconditionName);
	}

	private CompositeState compilePostConditionForReplacementChecking(LTSOutput output,
			Set<String> alphabetCharacters) {

		return new LTLf2LTS().toPropertyWithInit(output, this.postCondition, alphabetCharacters, postconditionName);
	}
}

package ltsa.lts.checkers.wellformedness;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.automaton.StateMachine;
import ltsa.lts.automata.automaton.transition.Transition;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.checkers.IntegratorEngine;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.Symbol;
import ltsa.ui.EmptyLTSOuput;
import MTSTools.ac.ic.doc.mtstools.model.MTSConstants;

/**
 * 
 * It modifies the Labeled Transition System to check well formedness.<br/>
 * 
 * It implements STEPS 1,2,3 of the well-formedness checking procedure. <br/>
 * 
 * For each box that is not the box of interest, it injects the LTS associated
 * with the post condition of the box <br/>
 *
 *
 * <b>STEP 1</b> Translate ever post-condition \gamma of every black box state
 * of the controller, including the box b, into an FLTLf formula. This
 * transformation ensures that the infinite traces that satisfy the FLTLf
 * formulae have the form pi,end^\omega, where \pi satisfies \gamma. For each
 * box $b_i$, the corresponding post-condition $\gamma^\prime$ is transformed
 * into an equivalent LTS, named LTS$_{b_i}$, using the procedure in
 * \cite{uchitel2009synthesis}. Since the LTS$_{b_i}$ has traces in the form
 * $\pi, \{\text{\emph{end}} \}^\omega$, it has a state $s$ with an
 * \emph{end}-labelled self-loop. This self-loop is removed and $s$ is
 * considered as final state of LTS$_{b_i}$. All other \emph{end}-labeled
 * transitions are replaced by $\tau$ transitions. The computed automata
 * LTS$_{b_i}$ contain all the traces that do not violate the corresponding
 * post-conditions and their size is, in the worst case, exponential in the size
 * of the corresponding post-condition. This step is performed since the
 * pre-condition of the black box $b$ must be verified under the assumption that
 * all black boxes, including $b$ itself, satisfy their post-conditions.
 */
public class WellFormednessLTSModifier {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

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
	 * @param controller
	 *            is the LTS whose well-formedness must be verified
	 * @param boxOfInterest
	 *            the box of interest
	 * @return the LTS modified with post conditions
	 */
	public LabelledTransitionSystem modify(LabelledTransitionSystem controller,
			String boxOfInterest) {

		// STEP 1
		Map<String, LabelledTransitionSystem> mapBoxPostCondition = this
				.step1(controller);

		// STEP 2
		Set<String> boxesToBeConsideredInStep2 = new HashSet<>(
				controller.getBoxes());
		boxesToBeConsideredInStep2.remove(boxOfInterest);

		LabelledTransitionSystem newController = this.step2(controller,
				boxesToBeConsideredInStep2, mapBoxPostCondition);

		// STEP 3
		return this.step3(newController, boxOfInterest, mapBoxPostCondition);
	}

	private Map<String, LabelledTransitionSystem> step1(
			LabelledTransitionSystem controller) {
		output.outln("\t APPLYING STEP1. Boxes: " + controller.getBoxes());

		Map<String, LabelledTransitionSystem> mapBoxPostCondition = new HashMap<>();
		for (String box : controller.getBoxes()) {

			logger.debug("Searching the post condition for the box " + box
					+ " of the controller: " + controller.getName());

			if (LTSCompiler.postconditionDefinitionManager
					.getMapBoxPostcondition().containsKey(controller.getName())) {
				logger.debug("a postcondition is associated with the box "
						+ box);
				String postConditionName = LTSCompiler.postconditionDefinitionManager
						.getMapBoxPostcondition().get(controller.getName())
						.get(box);

				mapBoxPostCondition.put(box,
						LTSCompiler.postconditionDefinitionManager.toFiniteLTS(
								new EmptyLTSOuput(), new HashSet<String>(
										controller.getAlphabetEvents()),
								postConditionName));
			} else {
				logger.debug("no boxes associated with the controller "
						+ controller.getName());
			}
		}

		Map<String, LabelledTransitionSystem> postConditions = new HashMap<>();

		for (String box : mapBoxPostCondition.keySet()) {
			if (!mapBoxPostCondition.keySet().contains(box)) {
				Set<String> boxInterface = controller.getBoxInterface(box);
				LabelledTransitionSystem boxInterfaceAutomaton = this
						.createInterfaceLTS(boxInterface);
				postConditions.put(box, boxInterfaceAutomaton);
			} else {
				Set<String> boxInterface = controller.getBoxInterface(box);
				LabelledTransitionSystem boxInterfaceAutomaton = this
						.createInterfaceLTS(boxInterface);

				Vector<LabelledTransitionSystem> machines = new Vector<>();
				machines.add(boxInterfaceAutomaton);
				LabelledTransitionSystem machinePostCondition = mapBoxPostCondition
						.get(box);
				String postCondition = machinePostCondition.getName();

				postConditions.put(
						box,
						processPost(controller, box, boxInterface,
								postCondition, mapBoxPostCondition));
			}
		}
		return postConditions;
	}

	private LabelledTransitionSystem step2(LabelledTransitionSystem controller,
			Set<String> boxes,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition) {
		output.outln("\t APPLYING STEP2. Boxes: " + boxes);

		LabelledTransitionSystem cs = controller.clone();
		cs.setName(controller.getName() + POST_CONDITION_SUFFIX);
		for (String box : boxes) {

			int boxPosition = controller.getBoxIndexes().get(box);

			LabelledTransitionSystem postConditionLTS = mapBoxPostCondition
					.get(box);
			output.outln("\t \t Integrating the post-condition of box: " + box);

			cs = new IntegratorEngine().apply(cs, boxPosition, box,
					postConditionLTS);
		}
		return cs;

	}

	private LabelledTransitionSystem step3(LabelledTransitionSystem controller,
			String box,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition) {
		output.outln("\t APPLYING STEP3. Box: " + box);

		LabelledTransitionSystem newController;

		if (mapBoxPostCondition.containsKey(box)) {
			LabelledTransitionSystem machinePostCondition = mapBoxPostCondition
					.get(box);
			output.outln("\t \t Integrating the post-condition of box: " + box);
			String postCondition = machinePostCondition.getName();

			machinePostCondition = processPost(controller, box,
					controller.getBoxInterface(box), postCondition,
					mapBoxPostCondition);

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

			int boxPosition = controller.getBoxIndexes().get(box);
			newController = new IntegratorEngine().apply(controller,
					boxPosition, box, machinePostCondition);
			newController.setName(controller.getName() + POST_CONDITION_SUFFIX);

		} else {
			newController = this.addInterfaceSelfLoop(controller, box);
			int eventIndex = newController.addEvent(LTLf2LTS.endSymbol
					.getValue());
			int addedStateIndex = newController.addNewState();

			newController.addTransition(addedStateIndex, eventIndex,
					addedStateIndex);

			newController.addFinalStateIndex(addedStateIndex);

			int boxIndex = newController.getBoxIndexes().get(box);
			newController.addTransition(boxIndex, eventIndex, addedStateIndex);
		}
		return newController;
	}

	private LabelledTransitionSystem addInterfaceSelfLoop(
			LabelledTransitionSystem compiledProcess, String box) {
		Set<String> boxInterface = compiledProcess.getBoxInterface(box);
		Integer boxIndex = compiledProcess.getBoxIndexes().get(box);

		LTSTransitionList holdOutgoingTransitions = compiledProcess
				.getTransitions(boxIndex);

		compiledProcess.setState(boxIndex, null);

		int newState = compiledProcess.addNewState();
		compiledProcess.addTransition(boxIndex,
				compiledProcess.getEvent(MTSConstants.TAU), newState);

		LTSTransitionList newTransitions = holdOutgoingTransitions;
		for (String event : boxInterface) {
			compiledProcess.addEvent(event);
			int eventIndex = compiledProcess.getEvent(event);
			newTransitions = LTSTransitionList.addTransition(newTransitions,
					eventIndex, newState);
		}

		compiledProcess.setState(newState, newTransitions);
		return compiledProcess;
	}

	private LabelledTransitionSystem processPost(
			LabelledTransitionSystem compiledProcess, String box,
			Set<String> boxInterface, String postCondition,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition)
			throws InternalError {
		LabelledTransitionSystem machinePostCondition = mapBoxPostCondition
				.get(box);

		Set<String> postConditionCharacters = new HashSet<>(
				machinePostCondition.getAlphabetEvents());
		postConditionCharacters.remove("tau");
		postConditionCharacters.remove("@any");
		postConditionCharacters.remove("end");
		if (!boxInterface.containsAll(postConditionCharacters)) {

			postConditionCharacters.removeAll(boxInterface);
			Diagnostics.fatal("The actions " + postConditionCharacters
					+ " of the postcondition " + postCondition
					+ " are not contained in the interface of the box " + box);
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

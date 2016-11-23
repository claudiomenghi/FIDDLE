package ltsa.lts.checkers.wellformedness;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.checkers.IntegratorEngine;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.ui.EmptyLTSOuput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import MTSTools.ac.ic.doc.mtstools.model.MTSConstants;

import com.google.common.base.Preconditions;

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

		output.outln("\t APPLYING STEP1. Boxes: " + controller.getBoxes());
		logger.debug("APPLYING STEP1. Boxes: " + controller.getBoxes());

		// STEP 1
		Map<String, LabelledTransitionSystem> mapBoxPostCondition = this
				.step1(controller);

		logger.debug("Boxes with post-conditions: "
				+ mapBoxPostCondition.keySet());
		// STEP 2
		Set<String> boxesToBeConsideredInStep2 = new HashSet<>(
				controller.getBoxes());
		boxesToBeConsideredInStep2.remove(boxOfInterest);

		logger.debug("APPLYING STEP2. Boxes: " + boxesToBeConsideredInStep2);
		LabelledTransitionSystem newController = this.step2(controller,
				boxesToBeConsideredInStep2, mapBoxPostCondition);

		logger.debug("Size of the new controller: " + newController.getName()
				+ ": " + newController.size());
		logger.debug("APPLYING STEP3. Box of interest: " + boxOfInterest);
		// STEP 3
		newController = this.step3(newController, boxOfInterest,
				mapBoxPostCondition);
		logger.debug("Size of the new controller: " + newController.getName()
				+ ": " + newController.size());
		return newController;
	}

	private Map<String, LabelledTransitionSystem> step1(
			LabelledTransitionSystem controller) {

		Map<String, LabelledTransitionSystem> mapBoxPostCondition = new HashMap<>();
		for (String box : controller.getBoxes()) {

			logger.debug("Searching the post condition for the box " + box
					+ " of the controller: " + controller.getName());

			if (LTSCompiler.postconditionDefinitionManager
					.getMapBoxPostcondition().containsKey(controller.getName())) {
				logger.debug("a postcondition is associated with the controller "
						+ controller.getName());

				if (LTSCompiler.postconditionDefinitionManager
						.getMapBoxPostcondition().get(controller.getName())
						.containsKey(box)) {
					String postConditionName = LTSCompiler.postconditionDefinitionManager
							.getMapBoxPostcondition().get(controller.getName())
							.get(box);
					logger.debug("The postcondition: " + postConditionName
							+ " is associated with the box: " + box
							+ " of the controller " + controller.getName());

					mapBoxPostCondition.put(box,
							LTSCompiler.postconditionDefinitionManager
									.toFiniteLTS(
											new EmptyLTSOuput(),
											new HashSet<String>(controller
													.getAlphabetEvents()),
											postConditionName));
				} else {
					logger.debug("No postcondition is associated with the box: "
							+ box
							+ " of the controller "
							+ controller.getName());
				}
			} else {
				logger.debug("No post condition associated with the box: "
						+ box + " of the controller: " + controller.getName());
			}
		}
		return mapBoxPostCondition;
	}

	private LabelledTransitionSystem step2(LabelledTransitionSystem controller,
			Set<String> boxes,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition) {
		output.outln("\t APPLYING STEP2. Boxes: " + boxes);

		LabelledTransitionSystem cs = controller.clone();
		for (String box : boxes) {

			int boxPosition = controller.getBoxIndexes().get(box);

			logger.debug("Analizing the box: " + box);
			if (mapBoxPostCondition.containsKey(box)) {
				LabelledTransitionSystem postConditionLTS = mapBoxPostCondition
						.get(box);
				logger.debug("\t \t Integrating the post-condition of box: "
						+ box);
				Vector<String> toHide=new Vector<>();
				toHide.add(LTLf2LTS.endSymbol.getValue());
				
				postConditionLTS.conceal(toHide);
				cs = new IntegratorEngine().apply(cs, boxPosition, box,
						postConditionLTS);
			} else {
				logger.debug("No post-condition associated with the box: "
						+ box + " adding the self-loop");
				cs = this.addInterfaceSelfLoop(cs, box);
			}
		}
		cs.setName(controller.getName() + POST_CONDITION_SUFFIX);
		
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
			logger.debug("Integrating the post-condition: "
					+ machinePostCondition.getName()
					+ " associated with the box: " + box
					+ " in the controller: " + controller.getName());
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

			logger.debug("Post-condition size: " + machinePostCondition.size());

			newController = new IntegratorEngine().apply(controller,
					boxPosition, box, machinePostCondition);
			newController.setName(controller.getName() + POST_CONDITION_SUFFIX);

		} else {
			logger.debug("No post-condition associated with the box: " + box);
			logger.debug("Creating a self-loop with the interface events ");

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
			output.outln("The actions " + postConditionCharacters
					+ " of the postcondition " + postCondition
					+ " are not contained in the interface of the box " + box);
			Diagnostics.fatal("The actions " + postConditionCharacters
					+ " of the postcondition " + postCondition
					+ " are not contained in the interface of the box " + box);
		}

		return machinePostCondition;
	}
}

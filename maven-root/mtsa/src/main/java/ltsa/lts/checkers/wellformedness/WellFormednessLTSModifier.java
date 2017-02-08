package ltsa.lts.checkers.wellformedness;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

import MTSTools.ac.ic.doc.mtstools.model.MTSConstants;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.checkers.IntegratorEngine;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.ui.EmptyLTSOuput;

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
	public LabelledTransitionSystem modify(LabelledTransitionSystem controller, String boxOfInterest) {

		output.outln("\t APPLYING STEP1. Boxes: " + controller.getBoxes());
		logger.debug("APPLYING STEP1. Boxes: " + controller.getBoxes());

		// STEP 1
		Map<String, LabelledTransitionSystem> mapBoxPostCondition = this.step1(controller);

		logger.debug("Boxes with post-conditions: " + mapBoxPostCondition.keySet());
		// STEP 2
		Set<String> boxesToBeConsideredInStep2 = new HashSet<>(controller.getBoxes());
		boxesToBeConsideredInStep2.remove(boxOfInterest);

		logger.debug("APPLYING STEP2. Boxes: " + boxesToBeConsideredInStep2);
		LabelledTransitionSystem newController = this.step2(controller, boxesToBeConsideredInStep2,
				mapBoxPostCondition);

		logger.debug("Size of the new controller: " + newController.getName() + ": " + newController.size());
		logger.debug("APPLYING STEP3. Box of interest: " + boxOfInterest);
		// STEP 3
		newController = this.step3(newController, boxOfInterest, mapBoxPostCondition);
		logger.debug("Size of the new controller: " + newController.getName() + ": " + newController.size());
		return newController;
	}

	private Map<String, LabelledTransitionSystem> step1(LabelledTransitionSystem controller) {
		output.outln("APPLYING STEP1. Boxes: " + controller.getBoxes());

		Map<String, LabelledTransitionSystem> mapBoxPostCondition = new HashMap<>();
		for (String box : controller.getBoxes()) {

			logger.debug(
					"Searching the post condition for the box " + box + " of the controller: " + controller.getName());

			if (LTSCompiler.postconditionDefinitionManager.hasPostCondition(controller.getName(), box)) {

				String postConditionName = LTSCompiler.postconditionDefinitionManager
						.getPostCondition(controller.getName(), box);
				logger.debug("The postcondition " + postConditionName + " is associated with the box " + box);

				mapBoxPostCondition.put(box, LTSCompiler.postconditionDefinitionManager.toFiniteLTS(new EmptyLTSOuput(),
						controller.getBoxInterface(box), postConditionName));
			} else {
				logger.debug("no postcondition  associated with the box " + box + " of the controller "
						+ controller.getName());
			}
		}

		Map<String, LabelledTransitionSystem> postConditions = new HashMap<>();

		for (String box : controller.getBoxes()) {
			if (!mapBoxPostCondition.keySet().contains(box)) {
				logger.debug("no postcondition  associated with the box " + box + " of the controller "
						+ controller.getName());
				logger.debug("Adding an automaton that can recognize all the events of the interface of the box");
				Set<String> boxInterface = controller.getBoxInterface(box);
				logger.debug("Interface of the box " + box + ": " + boxInterface);

				LabelledTransitionSystem boxInterfaceAutomaton = this.createInterfaceLTS(boxInterface);
				postConditions.put(box, boxInterfaceAutomaton);

			} else {
				logger.debug(
						"postcondition  associated with the box " + box + " of the controller " + controller.getName());

				LabelledTransitionSystem machinePostCondition = mapBoxPostCondition.get(box);

				machinePostCondition.getAccepting().forEach(machinePostCondition::addFinalStateIndex);
				machinePostCondition.getAccepting().forEach(state -> machinePostCondition.removeTransition(state,

						machinePostCondition.getEvent("end"), state));

				Set<String> boxInterface = controller.getBoxInterface(box);
				boxInterface.add("end");
				logger.debug("Postcondition associated with  the box: " + box + " alphabet: " + boxInterface);

				Set<String> toBeRemoved = new HashSet<>(machinePostCondition.getAlphabetEvents());
				toBeRemoved.removeAll(boxInterface);
				machinePostCondition.removeTransitionsLabeledWithEvents(toBeRemoved);

				logger.debug("postcondition accepting states:" + machinePostCondition.getAccepting());
				boxInterface.remove("end");
				postConditions.put(box, mapBoxPostCondition.get(box));
			}
		}
		return postConditions;
	}

	private LabelledTransitionSystem step2(LabelledTransitionSystem controller, Set<String> boxes,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition) {
		output.outln("\t APPLYING STEP2. Boxes: " + boxes);

		output.outln("APPLYING STEP2. Boxes: " + mapBoxPostCondition.keySet());

		LabelledTransitionSystem cs = controller.clone();

		for (String box : boxes) {

			int boxPosition = controller.getBoxIndexes().get(box);

			LabelledTransitionSystem postConditionLTS = mapBoxPostCondition.get(box);

			output.outln("\t Integrating the post-condition of box: " + box);

			LabelledTransitionSystem cscopy = new IntegratorEngine().apply(cs, boxPosition, box, postConditionLTS);

			for (int eventIndex = 0; eventIndex < cs.getAlphabet().length; eventIndex++) {
				for (int finalStateIndex : cscopy.getFinalStateIndexes()) {
					cscopy.removeTransition(finalStateIndex,eventIndex , finalStateIndex);
				}
			}
			cs = cscopy;

		}
		cs.setName(controller.getName() + POST_CONDITION_SUFFIX);

		cs.relabel("end", "tau");
		cs.removeEvent("@any");
		return cs;

	}

	private LabelledTransitionSystem step3(LabelledTransitionSystem controller, String box,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition) {
		output.outln("\t APPLYING STEP3. Box: " + box);

		LabelledTransitionSystem newController;

		LabelledTransitionSystem machinePostCondition = mapBoxPostCondition.get(box);
		output.outln("\t \t Integrating the post-condition of box: " + box);
		logger.debug("Integrating the post-condition: " + machinePostCondition.getName() + " associated with the box: "
				+ box + " in the controller: " + controller.getName());

		machinePostCondition = mapBoxPostCondition.get(box);

		for (int eventIndex = 0; eventIndex < machinePostCondition.getAlphabet().length; eventIndex++) {
			for (int finalStateIndex : machinePostCondition.getFinalStateIndexes()) {
				machinePostCondition.removeTransition(finalStateIndex, eventIndex, finalStateIndex);
			}
		}

		machinePostCondition.setName(controller.getName() + POST_CONDITION_SUFFIX);
		machinePostCondition.relabel("end", "tau");
		machinePostCondition.removeEvent("@any");
		
		int newInitiatilState = machinePostCondition.addInitialState();

		int eventIndex = machinePostCondition.addEvent(LTLf2LTS.endSymbol.getValue());
		int endStateIndex = machinePostCondition.addNewState();
		machinePostCondition.addTransition(endStateIndex, eventIndex, endStateIndex);

		int tauIndex = machinePostCondition.addEvent(MTSConstants.TAU);
		machinePostCondition.addTransition(newInitiatilState, tauIndex, 1);
		machinePostCondition.addTransition(0, eventIndex, endStateIndex);

		int boxPosition = controller.getBoxIndexes().get(box);

		logger.debug("Post-condition size: " + machinePostCondition.size());

		// errore
		newController = new IntegratorEngine().apply(controller, boxPosition, box, machinePostCondition);
		newController.setName(controller.getName() + POST_CONDITION_SUFFIX);

		newController.removeEvent("@any");
		return newController;
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

		LabelledTransitionSystem lts = new LabelledTransitionSystem(LTSCompiler.boxOfInterest + "-interface");
		int stateIndex = lts.addNewState();
		// mc.addFinalState(initStateName);

		boxInterface.stream().forEach(event -> {
			int eventIndex = lts.addEvent(event);
			lts.addTransition(stateIndex, eventIndex, stateIndex);

		});
		lts.addFinalStateIndex(stateIndex);
		return lts;

	}

}

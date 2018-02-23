package ltsa.lts.checkers.modelchecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.checkers.IntegratorEngine;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.ui.EmptyLTSOuput;

/**
 * 
 * It modifies the Labeled Transition System of the controller for the model
 * checking phase.<br/>
 * 
 * It implements STEPS 1,2 of the model checking procedure. <br/>
 * 
 * For each box it injects the LTS associated with the post condition of the box
 * <br/>
 *
 */
public class ModelCheckerLTSModifier {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The suffix used to rename the machine of interest
	 */
	private static final String POST_CONDITION_SUFFIX = "_WITH_POST";

	/**
	 * The output used to write messages on the screen
	 */
	protected LTSOutput output;

	/**
	 * 
	 * @param output
	 *            the output used to print messages
	 * @throws NullPointerException
	 *             if the output is null
	 */
	public ModelCheckerLTSModifier(@Nonnull LTSOutput output) {
		Preconditions.checkNotNull(output, "The output cannot be null");
		this.output = output;
	}

	/**
	 * modifies the LTS to perform the model checking.<br/>
	 * 
	 * For each box it injects the LTS associated with the post condition of the
	 * box <br/>
	 * 
	 * @param controller
	 *            is the LTS that must be verified
	 * @return the LTS modified with post conditions
	 */
	public LabelledTransitionSystem modify(@Nonnull LabelledTransitionSystem controller) {

		// STEP 1
		Map<String, LabelledTransitionSystem> mapBoxPostCondition = this.step1(controller);

		// STEP 2
		LabelledTransitionSystem newController = this.step2(controller, mapBoxPostCondition,
				mapBoxPostCondition.keySet());

		return newController;
	}

	protected Map<String, LabelledTransitionSystem> step1(LabelledTransitionSystem controller) {
		output.outln("APPLYING STEP1. Boxes: " + controller.getBoxes());

		Map<String, LabelledTransitionSystem> mapBoxPostCondition = new HashMap<>();
		for (String box : controller.getBoxes()) {

			logger.debug(
					"Searching the post condition for the box " + box + " of the controller: " + controller.getName());

			if (LTSCompiler.postconditionDefinitionManager.hasPostCondition(controller.getName(), box)) {

				String postConditionName = LTSCompiler.postconditionDefinitionManager
						.getPostCondition(controller.getName(), box);
				logger.debug("The postcondition " + postConditionName + " is associated with the box " + box);

				mapBoxPostCondition.put(box, LTSCompiler.postconditionDefinitionManager.toLTS(new EmptyLTSOuput(),
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

	protected LabelledTransitionSystem step2(LabelledTransitionSystem controller,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition, Set<String> boxes) {
		output.outln("APPLYING STEP2. Boxes: " + mapBoxPostCondition.keySet());

		LabelledTransitionSystem cs = controller.clone();

		for (String box : boxes) {

			int boxPosition = cs.getBoxIndexes().get(box);

			LabelledTransitionSystem postConditionLTS = mapBoxPostCondition.get(box);

			if (postConditionLTS.getLabelIndex("end") != -1) {
				postConditionLTS.relabelAndRemoveOldLabel("end", "tau");
			}
			output.outln("\t Integrating the post-condition of box: " + box);

			LabelledTransitionSystem cscopy = new IntegratorEngine().apply(cs, boxPosition, box, postConditionLTS);

			if (LTSCompiler.postconditionDefinitionManager.hasPostCondition(controller.getName(), box)) {
				for (int eventIndex = 0; eventIndex < cs.getAlphabet().length; eventIndex++) {
					for (int finalStateIndex : cscopy.getFinalStateIndexes()) {
						cscopy.removeTransition(finalStateIndex, eventIndex, finalStateIndex);
					}
				}
			}
			cs = cscopy;

		}
		cs.setName(controller.getName() + POST_CONDITION_SUFFIX);

		return cs;

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

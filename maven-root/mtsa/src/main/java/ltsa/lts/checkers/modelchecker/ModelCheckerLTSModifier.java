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
import ltsa.lts.csp.Relation;
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
	private LTSOutput output;

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
		LabelledTransitionSystem newController = this.step2(controller, mapBoxPostCondition);

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

				//Vector<LabelledTransitionSystem> machines = new Vector<>();
				//machines.add(boxInterfaceAutomaton);

				LabelledTransitionSystem machinePostCondition = mapBoxPostCondition.get(box);
				String postCondition = machinePostCondition.getName();
				
				//machines.add(machinePostCondition);

				CompositeState state = new CompositeState(machines);
				//state.compose(new EmptyLTSOuput());

				//machinePostCondition = state.getComposition();
				//machinePostCondition.setName(postCondition);
				machinePostCondition.getAccepting().forEach(machinePostCondition::addFinalStateIndex);
				
				Set<String> boxInterface = controller.getBoxInterface(box);
				boxInterface.add("end");
				logger.debug(
						"postcondition  condition with filtered alphabet: " + machinePostCondition);
				logger.debug("boxInterface: "+boxInterface);

				Set<String> toBeRemoved=new HashSet<>(machinePostCondition.getAlphabetEvents());
				toBeRemoved.removeAll(boxInterface);
				machinePostCondition.removeTransitionsLabeledWithEvents(toBeRemoved);
				
				logger.debug(
						"postcondition  condition with filtered alphabet: " + machinePostCondition);

				
				logger.debug("postcondition accepting states:"+machinePostCondition.getAccepting());
				boxInterface.remove("end");
				
				postConditions.put(box,
						processPost(controller, box, boxInterface, postCondition,
								LTSCompiler.postconditionDefinitionManager.getPostCondition(controller.getName(), box),
								mapBoxPostCondition));
			}
		}
		return postConditions;
	}

	private LabelledTransitionSystem step2(LabelledTransitionSystem controller,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition) {
		output.outln("APPLYING STEP2. Boxes: " + mapBoxPostCondition.keySet());

		LabelledTransitionSystem cs = controller.clone();

		for (String box : mapBoxPostCondition.keySet()) {

			int boxPosition = controller.getBoxIndexes().get(box);

			LabelledTransitionSystem postConditionLTS = mapBoxPostCondition.get(box);
			

			output.outln("\t Integrating the post-condition of box: " + box);

			LabelledTransitionSystem cscopy = new IntegratorEngine().apply(cs, boxPosition, box, postConditionLTS);

			if (cscopy.usesLabel("end")) {
				cscopy.getAccepting().forEach(s -> cscopy.removeOutgoingTransitionsWithLabel(s, "end"));
			}
			cs = cscopy;

		}
		cs.setName(controller.getName() + POST_CONDITION_SUFFIX);
		return cs;

	}

	private LabelledTransitionSystem processPost(LabelledTransitionSystem compiledProcess, String box,
			Set<String> boxInterface, String postCondition, String postConditionName,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition) throws InternalError {
		LabelledTransitionSystem machinePostCondition = mapBoxPostCondition.get(box);

		Set<String> postConditionCharacters = new HashSet<>(machinePostCondition.getAlphabetEvents());
		postConditionCharacters.remove("tau");
		postConditionCharacters.remove("@any");
		postConditionCharacters.remove("end");

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

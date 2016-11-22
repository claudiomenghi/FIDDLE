package ltsa.lts.checkers.modelchecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.annotation.Nonnull;

import ltsa.lts.automata.automaton.StateMachine;
import ltsa.lts.automata.automaton.transition.Transition;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.checkers.IntegratorEngine;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.Symbol;
import ltsa.ui.EmptyLTSOuput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

/**
 * 
 * It modifies the Labeled Transition System of the controller for the model
 * checking phase.<br/>
 * 
 * It implements STEPS 1,2 of the model checking procedure. <br/>
 * 
 * For each box it injects the LTS associated with the post condition of the box <br/>
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
	public LabelledTransitionSystem modify(
			@Nonnull LabelledTransitionSystem controller) {

		// STEP 1
		Map<String, LabelledTransitionSystem> mapBoxPostCondition = this
				.step1(controller);

		// STEP 2
		LabelledTransitionSystem newController = this.step2(controller,
				mapBoxPostCondition);

		return newController;
	}

	private Map<String, LabelledTransitionSystem> step1(
			LabelledTransitionSystem controller) {
		output.outln("APPLYING STEP1. Boxes: " + controller.getBoxes());

		Map<String, LabelledTransitionSystem> mapBoxPostCondition = new HashMap<>();
		for (String box : controller.getBoxes()) {

			logger.debug("Searching the post condition for the box " + box
					+ " of the controller: " + controller.getName());

			if (LTSCompiler.postconditionDefinitionManager.hasPostCondition(
					controller.getName(), box)) {

				String postConditionName = LTSCompiler.postconditionDefinitionManager
						.getPostCondition(controller.getName(), box);
				logger.debug("The postcondition " + postConditionName
						+ " is associated with the box " + box);

				mapBoxPostCondition.put(box,
						LTSCompiler.postconditionDefinitionManager.toFiniteLTS(
								new EmptyLTSOuput(),
								controller.getBoxInterface(box),
								postConditionName));
			} else {
				logger.debug("no postcondition  associated with the box " + box
						+ " of the controller " + controller.getName());
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
								postCondition,
								LTSCompiler.postconditionDefinitionManager
										.getPostCondition(controller.getName(),
												box), mapBoxPostCondition));
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

			LabelledTransitionSystem postConditionLTS = mapBoxPostCondition
					.get(box);


			output.outln("\t Integrating the post-condition of box: " + box);

			cs = new IntegratorEngine().apply(cs, boxPosition, box,
					postConditionLTS);
		}
		cs.setName(controller.getName() + POST_CONDITION_SUFFIX);
		return cs;

	}

	private LabelledTransitionSystem processPost(
			LabelledTransitionSystem compiledProcess, String box,
			Set<String> boxInterface, String postCondition,
			String postConditionName,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition)
			throws InternalError {
		LabelledTransitionSystem machinePostCondition = mapBoxPostCondition
				.get(box);

		Set<String> postConditionCharacters = new HashSet<>(
				machinePostCondition.getAlphabetEvents());
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

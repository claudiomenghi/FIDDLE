package ltsa.lts.checkers.wellformedness;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

import MTSTools.ac.ic.doc.mtstools.model.MTSConstants;
import ltsa.lts.automata.lts.LTSConstants;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.checkers.IntegratorEngine;
import ltsa.lts.checkers.modelchecker.ModelCheckerLTSModifier;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;

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
public class WellFormednessLTSModifier extends ModelCheckerLTSModifier {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The suffix used to rename the machine of interest
	 */
	private static final String POST_CONDITION_SUFFIX = "_WITH_POST";

	/**
	 * 
	 * @param output
	 *            the output used to print messages
	 * @throws NullPointerException
	 *             if the output is null
	 */
	public WellFormednessLTSModifier(LTSOutput output) {
		super(output);
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
		LabelledTransitionSystem newController = this.step2(controller, mapBoxPostCondition,
				boxesToBeConsideredInStep2);

		// STEP 3
		logger.debug("APPLYING STEP3. Box of interest: " + boxOfInterest);
		newController = this.step3(newController, mapBoxPostCondition, boxOfInterest);

		newController.setName(controller.getName() + POST_CONDITION_SUFFIX);
		newController.getFinalStateIndexes().clear();
		newController.setEndOfSequence(LTSConstants.NO_SEQUENCE_FOUND);
		newController.removeEvent("@any");
		return newController;
	}

	/**
	 * This method is used for verifying the scalability 
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
	public LabelledTransitionSystem modify(LabelledTransitionSystem controller, String boxOfInterest,
			LabelledTransitionSystem postcondition) {

		Preconditions.checkNotNull(controller, "The partial component cannot be null");
		Preconditions.checkNotNull(boxOfInterest, "The box cannot be null");
		Preconditions.checkNotNull(postcondition, "The postcondition cannot be null");

		output.outln("\t APPLYING STEP1. Boxes: " + controller.getBoxes());
		logger.debug("APPLYING STEP1. Boxes: " + controller.getBoxes());

		LabelledTransitionSystem newController = controller;

		Map<String, LabelledTransitionSystem> mapBoxPostCondition = new HashMap<>();
		mapBoxPostCondition.put(boxOfInterest, postcondition);

		// STEP 3
		logger.debug("APPLYING STEP3. Box of interest: " + boxOfInterest);
		newController = this.step3(newController, mapBoxPostCondition, boxOfInterest);

		newController.setName(controller.getName() + POST_CONDITION_SUFFIX);
		newController.getFinalStateIndexes().clear();
		newController.setEndOfSequence(LTSConstants.NO_SEQUENCE_FOUND);
		newController.removeEvent("@any");
		return newController;
	}

	protected LabelledTransitionSystem step3(LabelledTransitionSystem controller,
			Map<String, LabelledTransitionSystem> mapBoxPostCondition, String boxOfInterest) {

		Preconditions.checkNotNull(controller, "The partial component cannot be null");
		Preconditions.checkArgument(mapBoxPostCondition.containsKey(boxOfInterest));

		output.outln("APPLYING STEP3. Box: " + boxOfInterest);
		controller.setName(controller.getName());

		int boxPosition = controller.getBoxIndexes().get(boxOfInterest);

		//logger.debug(controller);
		LabelledTransitionSystem postConditionLTS = mapBoxPostCondition.get(boxOfInterest);

		postConditionLTS.relabelAndKeepOldLabel("end", MTSConstants.TAU);
	
		int newInitiatilState = postConditionLTS.addInitialState();

		int tauIndex = postConditionLTS.addEvent(MTSConstants.TAU);
		postConditionLTS.addTransition(newInitiatilState, tauIndex, 1);

		int endStateIndex = postConditionLTS.addNewState();

		int endeventIndex = postConditionLTS.getEvent("end");

		postConditionLTS.addTransition(newInitiatilState, endeventIndex, endStateIndex);
		postConditionLTS.addTransition(endStateIndex, endeventIndex, endStateIndex);


		LabelledTransitionSystem cscopy = new IntegratorEngine().apply(controller, boxPosition, boxOfInterest,
				postConditionLTS);


		for (int eventIndex = 0; eventIndex < cscopy.getAlphabet().length; eventIndex++) {
			for (int finalStateIndex : cscopy.getFinalStateIndexes()) {
				cscopy.removeTransition(finalStateIndex, eventIndex, finalStateIndex);
			}
		}
		controller = cscopy;
		return controller;

	}
}

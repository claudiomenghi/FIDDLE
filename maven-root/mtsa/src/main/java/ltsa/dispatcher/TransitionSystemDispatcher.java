package ltsa.dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.lang.Validate;

import com.google.common.base.Preconditions;

import MTSTools.ac.ic.doc.commons.relations.BinaryRelation;
import MTSTools.ac.ic.doc.commons.relations.Pair;
import MTSTools.ac.ic.doc.mtstools.facade.MTSAFacade;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS.TransitionType;
import MTSTools.ac.ic.doc.mtstools.model.MTSConstants;
import MTSTools.ac.ic.doc.mtstools.model.Refinement;
import MTSTools.ac.ic.doc.mtstools.model.RefinementByRelation;
import MTSTools.ac.ic.doc.mtstools.model.SemanticType;
import MTSTools.ac.ic.doc.mtstools.model.impl.LTSSimulationSemantics;
import MTSTools.ac.ic.doc.mtstools.model.impl.MTSDeterminiser;
import MTSTools.ac.ic.doc.mtstools.model.impl.MTSMinimiser;
import MTSTools.ac.ic.doc.mtstools.model.impl.MTSMultipleComposer;
import MTSTools.ac.ic.doc.mtstools.model.impl.PlusCARulesApplier;
import MTSTools.ac.ic.doc.mtstools.model.impl.WeakAlphabetPlusCROperator;
import MTSTools.ac.ic.doc.mtstools.model.impl.WeakSemantics;
import MTSTools.ac.ic.doc.mtstools.model.operations.Consistency;
import MTSTools.ac.ic.doc.mtstools.model.operations.MTSAbstractBuilder;
import MTSTools.ac.ic.doc.mtstools.model.operations.MTSConstraintBuilder;
import MTSTools.ac.ic.doc.mtstools.model.operations.impl.MTSPropertyToBuchiConverter;
import MTSTools.ac.ic.doc.mtstools.model.operations.impl.WeakAlphabetMergeBuilder;
import ltsa.ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ltsa.ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ltsa.lts.Diagnostics;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.checkers.Analyser;
import ltsa.lts.operations.minimization.Minimiser;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.Symbol;
import ltsa.lts.util.MTSUtils;
import ltsa.ui.EmptyLTSOuput;
import ltsa.ui.MTSAnimator;

/**
 * This class consists exclusively of static methods that operate on or return
 * CompactState, CompositeState and MTS.
 * 
 * In the MTSA architecture this class is the communication layer between the
 * graphical interface, LTSA Core and MTSACore. None of them communicates each
 * other directly, they do it through this class.
 * 
 */
public class TransitionSystemDispatcher {

	/**
	 * Given a CompactState model returns the optimistic version of it.
	 * 
	 * @param compactState
	 * @param output
	 * @return the optimistic representation of the compactState parameter
	 */
	public static LabelledTransitionSystem getOptimisticModel(LabelledTransitionSystem compactState, LTSOutput output) {
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(compactState);
		long initialTime = System.currentTimeMillis();
		mts = MTSAFacade.getOptimisticModel(mts);
		output.outln("MTS Representation: Optimistic operator applied to " + compactState.getName() + " generated in: "
				+ (System.currentTimeMillis() - initialTime) + "ms.");

		return MTSToAutomataConverter.getInstance().convert(mts, compactState.getName());
	}

	/**
	 * Given a CompactState model returns the pessimistic version of it.
	 * 
	 * @param composition
	 * @return the pessimistic representation of the compactState parameter
	 */
	public static LabelledTransitionSystem getPessimistModel(LabelledTransitionSystem composition) {
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(composition);
		mts = MTSAFacade.getPesimisticModel(mts);
		return MTSToAutomataConverter.getInstance().convert(mts, composition.getName());
	}

	/**
	 * Given a CompositeState model this method builds the optimistic version of
	 * it. If the composition field of compositeState parameter it's not null
	 * then it's replaced for its optimistic version. Else every compactState
	 * inside compositeState parameter it's replaced for its optimistic version.
	 * 
	 * @param compositeState
	 */
	public static void makeOptimisticModel(CompositeState compositeState, LTSOutput ltsOutput) {
		if (MTSUtils.isMTSRepresentation(compositeState)) {
			if (compositeState.getComposition() == null) {
				long initialTime = System.currentTimeMillis();
				compositeState.setMachines(TransitionSystemDispatcher.getOptimistModels(compositeState.getMachines(),
						new EmptyLTSOuput()));
				ltsOutput.outln("MTS Representation: Optimistic model generated for all automatas in: "
						+ (System.currentTimeMillis() - initialTime) + "ms.");
			} else {
				compositeState.setMachines(TransitionSystemDispatcher.getOptimistModels(compositeState.getMachines(),
						new EmptyLTSOuput()));
				compositeState.setComposition(
						TransitionSystemDispatcher.getOptimisticModel(compositeState.getComposition(), ltsOutput));
			}
		}
	}

	/**
	 * Given a CompositeState model this method builds the optimistic version of
	 * it. If the composition field of compositeState parameter it's not null
	 * then it's replaced for its optimistic version. Else every compactState
	 * inside compositeState parameter it's replaced for its optimistic version.
	 * 
	 * @param compositeState
	 */
	public static void makePessimisticModel(CompositeState compositeState, LTSOutput ltsOutput) {
		if (MTSUtils.isMTSRepresentation(compositeState)) {
			if (compositeState.getComposition() == null) {
				if (MTSUtils.isMTSRepresentation(compositeState)) {
					long initialTime = System.currentTimeMillis();
					compositeState.setMachines(getPessimisticModels(compositeState.getMachines()));
					ltsOutput.outln("MTS Representation: Pessimistic model generated for all automatas in: "
							+ (System.currentTimeMillis() - initialTime) + "ms.");
				}
				applyComposition(compositeState, ltsOutput);
			} else {
				long initialTime = System.currentTimeMillis();
				compositeState
						.setComposition(TransitionSystemDispatcher.getPessimistModel(compositeState.getComposition()));
				ltsOutput.outln("MTS Representation: Pessimistic model generated for composition in: "
						+ (System.currentTimeMillis() - initialTime) + "ms.");
			}
		}
	}

	/**
	 * Safety check over compositeState parameter.
	 * 
	 * 
	 * @param compositeState
	 *            model to be checked
	 * @param ltsOutput
	 *            used to print output in MTSA
	 */
	public static void checkSafety(CompositeState compositeState, LTSOutput ltsOutput) {
		checkSafetyOrDeadlock(false, compositeState, ltsOutput);
	}

	private static void checkSafetyOrDeadlock(boolean checkDeadlocks, CompositeState compositeState,
			LTSOutput ltsOutput) {
		if (MTSUtils.isMTSRepresentation(compositeState)) {

			if (hasCompositionDeadlockFreeImplementations(compositeState, ltsOutput)) {
				checkSafety(compositeState, ltsOutput, checkDeadlocks);
			} else {
				ltsOutput.outln(
						"*****************************************************************************************");
				ltsOutput.outln(
						"Model must have at least one deadlock free implementation for a Safety or Deadlock check.");
				ltsOutput.outln(
						"*****************************************************************************************");
			}

		} else {
			compositeState.analyse(checkDeadlocks, ltsOutput);
		}
	}

	/**
	 * This method applies composition over the parameter model. If the
	 * <code>toCompose</code> model it's a LTS, then it can only be composed
	 * using parallel composition, it means that the compositionType field of
	 * <code>toCompose</code> should be set to Symbol.OR. If
	 * <code>toCompose</code> is an MTS then there are different kinds of model
	 * composition might be applied depending on the value of the
	 * compositionType field.
	 * 
	 * 
	 * @param toCompose
	 *            composite model to be composed
	 * @param ltsOutput
	 *            used for process output
	 */
	public static void applyComposition(CompositeState toCompose, LTSOutput ltsOutput) {
		compose(toCompose, ltsOutput);
		toCompose.applyOperations(ltsOutput);

	}

	private static void compose(CompositeState toCompose, LTSOutput ltsOutput) {
		int compositionType = toCompose.getCompositionType();
		switch (compositionType) {
		case Symbol.MERGE:
			merge(toCompose, ltsOutput);
			break;
		case Symbol.PLUS_CR:
			applyPlusCROperator(toCompose, ltsOutput);
			break;
		case Symbol.PLUS_CA:
			applyPlusCAOperator(toCompose, ltsOutput);
			break;
		case Symbol.OR:
		default:
			parallelComposition(toCompose, ltsOutput);
		}
	}

	private static void merge(CompositeState composition, LTSOutput ltsOutput) {
		if (composition.getComposition() == null) {
			ArrayList<MTS<Long, String>> toCompose = new ArrayList<>();
			List<String> names = new ArrayList<>();

			for (Iterator<LabelledTransitionSystem> it = composition.getMachines().iterator(); it.hasNext();) {
				LabelledTransitionSystem compactState = it.next();
				toCompose.add(AutomataToMTSConverter.getInstance().convert(compactState));
				names.add(compactState.getName());
			}
			if (ltsOutput != null) {
				ltsOutput.outln("Applying Merge Operator to MTSs...(" + composition.getName() + ")");
			}
			long initialTime = System.currentTimeMillis();

			if (toCompose.size() > 2) {
				ltsOutput.outln(
						"Warning: Merge is being applied to more than two models. Pair-wise merging will be performed. See [FM06] for details on associativity of merge");
			}

			Set<String> tau = Collections.singleton(MTSConstants.TAU);
			MTS<?, String> merge = null;
			MTS<?, String> mtsA = toCompose.get(0);
			String mtsAName = names.get(0);
			int i = 1;
			while (i < toCompose.size()) {

				MTS<Long, String> mtsB = toCompose.get(i++);

				try {
					merge = new WeakAlphabetMergeBuilder(tau).merge(mtsA, mtsB);
				} catch (Exception e) {
					if (CollectionUtils.isEqualCollection(mtsB.getActions(), mtsA.getActions())) {
						// Same alphabets
						// Strong or Weak
						ltsOutput.outln("***************************************************************");
						ltsOutput.outln("There is no weak consistency relation for these models.");
						ltsOutput.outln("This means they are inconsistent and  cannot be merged [TOSEM].");
						ltsOutput.outln("****************************************************************");
					} else {

						// Weak Alphabet
						ltsOutput.outln(
								"********************************************************************************");
						ltsOutput.outln("There is no weak alphabet consistency relation for these models.");
						ltsOutput.outln(
								"This does NOT mean they are inconsistent, however they cannot be merged [TOSEM].");
						ltsOutput.outln("Try merging them on their common alphabet. If they are still inconsistent,");
						ltsOutput.outln("then the models currently being merged are inconsistent [TOSEM].");
						ltsOutput.outln(
								"*********************************************************************************");
					}
					return;
				}

				ltsOutput.outln("Merge operator applied in " + (System.currentTimeMillis() - initialTime) + "ms.");

				Set<String> alphabetAminusB = new HashSet<String>(
						CollectionUtils.subtract(mtsA.getActions(), mtsB.getActions()));
				Set<String> alphabetBminusA = new HashSet<String>(
						CollectionUtils.subtract(mtsB.getActions(), mtsA.getActions()));

				alphabetAminusB.addAll(tau);
				alphabetBminusA.addAll(tau);

				RefinementByRelation refA = new WeakSemantics(alphabetBminusA);
				RefinementByRelation refB = new WeakSemantics(alphabetAminusB);

				ltsOutput.outln("Internal sanity check: Validating merge is a common refinement...");
				isRefinement(merge, composition.getName(), mtsA, mtsAName, refA, ltsOutput);
				isRefinement(merge, composition.getName(), mtsB, names.get(i - 1), refB, ltsOutput);

				mtsA = merge;
				mtsAName = "(" + mtsAName + "++" + names.get(i - 1) + ")";

				ltsOutput.outln(""); // leave an empty line
			}



		}
	}

	/***
	 * Applies parallel composition over all model instances in machines field
	 * of <code>compositeState</code> and it's result is set to composition
	 * field of <code>compositeState</code>.
	 * 
	 * If <code>compositeState</code> is an LTS then LTS parallel composition is
	 * applied. If <code>compositeState</code> is an MTS then MTS parallel
	 * composition is applied.
	 * 
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	public static void parallelComposition(CompositeState compositeState, LTSOutput ltsOutput) {
		Preconditions.checkNotNull(ltsOutput, "An output class is required");

		if (MTSUtils.isMTSRepresentation(compositeState)) {

			long initialTime = System.currentTimeMillis();
			ltsOutput.outln("Converting MTSs from " + compositeState.getName());
			ltsOutput.outln("Composing MTSs from " + compositeState.getName());

			MTS<Long, String> mts = MTSUtils.getMTSComposition(compositeState);

			ltsOutput.outln("MTSs composed in " + (System.currentTimeMillis() - initialTime) + "ms.\n");

			LabelledTransitionSystem convert = MTSToAutomataConverter.getInstance().convert(mts,
					compositeState.getName());
			compositeState.setComposition(convert);

		} else {
			compositeState.compose(ltsOutput);
		}
	}

	/**
	 * Applies plus CR merge operator over the models in <code>machines</code>
	 * field of <code>compositeState</code>. The result of +CR is set to the
	 * <code>composition</code> field of <code>compositeState</code> .
	 * 
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	public static void applyPlusCROperator(CompositeState compositeState, LTSOutput ltsOutput) {
		if (compositeState.getComposition() == null) {
			ArrayList<MTS<Long, String>> toCompose = new ArrayList<>();

			long initialTime = System.currentTimeMillis();
			ltsOutput.outln("Converting CompactState to MTSs...");

			for (Iterator<LabelledTransitionSystem> it = compositeState.getMachines().iterator(); it.hasNext();) {
				LabelledTransitionSystem compactState = it.next();
				toCompose.add(AutomataToMTSConverter.getInstance().convert(compactState));
			}

			ltsOutput.outln("Applying +CR Operator to MTSs...");
			initialTime = System.currentTimeMillis();

			Set<String> silentActions = Collections.singleton(MTSConstants.TAU);
			assert (toCompose.size() >= 2);
			Iterator<MTS<Long, String>> iterator = toCompose.iterator();

			MTS<Long, String> merge = iterator.next();
			while (iterator.hasNext()) {
				MTS<Long, String> mts = iterator.next();
				MTS<?, String> cr = new WeakAlphabetPlusCROperator(silentActions).compose(merge, mts);
				
			}

			ltsOutput.outln("+CR operator applied in " + (System.currentTimeMillis() - initialTime) + "ms.");
			compositeState
					.setComposition(MTSToAutomataConverter.getInstance().convert(merge, compositeState.getName()));
			ltsOutput.outln(""); // leave an empty line
		}
	}

	/**
	 * Applies plus CR merge operator over the models in <code>machines</code>
	 * field of <code>compositeState</code>. The result of +CR is set to the
	 * <code>composition</code> field of <code>compositeState</code> .
	 * 
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	public static void applyPlusCAOperator(CompositeState compositeState, LTSOutput ltsOutput) {
		if (compositeState.getComposition() == null) {
			PlusCARulesApplier plusCARulesApplier = new PlusCARulesApplier();
			String plusSymbol = "+CA";

			List<MTS<Long, String>> toCompose = new ArrayList<>();

			long initialTime = System.currentTimeMillis();
			ltsOutput.outln("Converting CompactState to MTSs...");

			for (Iterator<LabelledTransitionSystem> it = compositeState.getMachines().iterator(); it.hasNext();) {
				LabelledTransitionSystem compactState = it.next();
				toCompose.add(AutomataToMTSConverter.getInstance().convert(compactState));
			}
			ltsOutput.outln("MTSs converted in " + (System.currentTimeMillis() - initialTime) + "ms.");

			ltsOutput.outln("Applying " + plusSymbol + " operator to MTSs...");
			initialTime = System.currentTimeMillis();
			MTS<Long, String> merge = new MTSMultipleComposer<Long, String>(plusCARulesApplier).compose(toCompose);

			ltsOutput.outln(plusSymbol + " operator applied in " + (System.currentTimeMillis() - initialTime) + "ms.");

			compositeState
					.setComposition(MTSToAutomataConverter.getInstance().convert(merge, compositeState.getName()));
			ltsOutput.outln(""); // leave an empty line
		}
	}

	/**
	 * Applies determinisation to <code>composition</code> parameter. The
	 * determinisation semantics depends on the model type. If
	 * <code>compositeState</code> it's an MTS then MTS semantics is applied,
	 * otherwise LTS is applied. If <code>compositeState</code> it's an MTS then
	 * before applying determinisation, composition is applied.
	 * 
	 * The result of determinisation is setted to <code>composition</code> field
	 * of <code>compositeState</code>/
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	public static void determinise(CompositeState compositeState, LTSOutput ltsOutput) {
		TransitionSystemDispatcher.compose(compositeState, ltsOutput);
		LabelledTransitionSystem deterministic = determinise(compositeState.getComposition(), ltsOutput);
		compositeState.setComposition(deterministic);
	}

	/**
	 * Applies determinisation over <code>lts</code> depending on the model type
	 * it can be used LTS or MTS semantic.
	 * 
	 * @param lts
	 * @param ltsOutput
	 *            output support
	 * @return deterministic version of <code>lts</code>
	 */
	public static LabelledTransitionSystem determinise(LabelledTransitionSystem lts, LTSOutput ltsOutput) {
		LabelledTransitionSystem compactState = (LabelledTransitionSystem) lts;
		if (MTSUtils.isMTSRepresentation(compactState)) {
			long initialTime = System.currentTimeMillis();
			// ltsOutput.outln("Converting CompactState to MTS...");
			MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(compactState);
			// ltsOutput.outln("MTS converted in "
			// + (System.currentTimeMillis() - initialTime) + "ms.");
			// initialTime = System.currentTimeMillis();
			ltsOutput.outln("Determinising ...");
			MTSDeterminiser determiniser = new MTSDeterminiser(mts, true);
			mts = determiniser.determinize();
			ltsOutput.outln("Model " + lts.getName() + " determinised in " + (System.currentTimeMillis() - initialTime)
					+ "ms.");
			return MTSToAutomataConverter.getInstance().convert(mts, compactState.getName(),
					MTSUtils.isMTSRepresentation(compactState));
		} else {
			Vector<LabelledTransitionSystem> toDet = new Vector<LabelledTransitionSystem>();
			toDet.add(compactState);
			CompositeState compositeState = new CompositeState(toDet);
			compositeState.compose(ltsOutput);
			compositeState.determinise(ltsOutput);

			ltsOutput.outln("Determinising ...");
			Minimiser d = new Minimiser(compactState, ltsOutput);
			return d.trace_minimise();
			// if (isProperty) composition.makeProperty();

			// return compositeState.getComposition();
			// compositeState.determinise(ltsOutput);
		}
		// return lts;
	}

	public static boolean isLTSRefinement(LabelledTransitionSystem refines, LabelledTransitionSystem refined,
			LTSOutput output) {
		LTSSimulationSemantics ss = new LTSSimulationSemantics(); // previously
																	// using
																	// emptySet
		MTS<Long, String> refinedMTS = AutomataToMTSConverter.getInstance().convert(refined);
		MTS<Long, String> refinesMTS = AutomataToMTSConverter.getInstance().convert(refines);
		return isRefinement(refinesMTS, refines.getName(), refinedMTS, refined.getName(), ss, output);
	}

	/**
	 * 
	 * Checks if <code>refines</code> model it's a refinement of refined
	 * <code>semantic</code> as the semantic for the refinement check.
	 * 
	 * 
	 * @param refines
	 * @param refined
	 * @param semantic
	 * @param ltsOutput
	 * @return
	 */
	public static boolean isRefinement(LabelledTransitionSystem refines, LabelledTransitionSystem refined,
			SemanticType semantic, LTSOutput ltsOutput) {

		Refinement refinement = semantic.getRefinement();
		MTS<Long, String> refinedMTS = AutomataToMTSConverter.getInstance().convert((LabelledTransitionSystem) refined);
		MTS<Long, String> refinesMTS = AutomataToMTSConverter.getInstance().convert((LabelledTransitionSystem) refines);
		return isRefinement(refinesMTS, refines.getName(), refinedMTS, refined.getName(), refinement, ltsOutput);
	}

	/**
	 * 
	 * Checks if <code>refines</code> model is a refinement of
	 * <code>refined</code> using <code>refinement</code> parameter as the
	 * refinement notion
	 * 
	 * @param refines
	 * @param refinesName
	 * @param refined
	 * @param refinedName
	 * @param refinement
	 * @param ltsOutput
	 * 
	 * @param <A>
	 *            Models action type
	 * 
	 */
	public static <A> boolean isRefinement(MTS<?, A> refines, String refinesName, MTS<?, A> refined, String refinedName,
			Refinement refinement, LTSOutput ltsOutput) {

		String refinesOutput = "model [" + refinesName + "] ";
		String refinedOuput = "model [" + refinedName + "] ";
		ltsOutput.outln("Does " + refinesOutput + "refine " + refinedOuput + "? Verifying...");
		long initialTime = System.currentTimeMillis();
		boolean isRefinement = refinement.isARefinement(refined, refines);
		String refinesString = (isRefinement) ? "Yes" : "No";
		// ltsOutput.outln("Verified that " + refinesOutput + refinesString
		ltsOutput.outln(refinesString + ". (" + (System.currentTimeMillis() - initialTime) + "ms.)");
		return isRefinement;
	}

	public static boolean areConsistent(LabelledTransitionSystem csA, LabelledTransitionSystem csB,
			SemanticType semantic, LTSOutput ltsOutput) {

		MTS<Long, String> mtsA = AutomataToMTSConverter.getInstance().convert((LabelledTransitionSystem) csA);
		MTS<Long, String> mtsB = AutomataToMTSConverter.getInstance().convert((LabelledTransitionSystem) csB);
		return areConsistent(mtsA, csA.getName(), mtsB, csB.getName(), semantic, ltsOutput);
	}

	public static <A> boolean areConsistent(MTS<?, A> mtsA, String mtsAName, MTS<?, A> mtsB, String mtsBName,
			SemanticType semantic, LTSOutput ltsOutput) {

		long initialTime = System.currentTimeMillis();
		ltsOutput.outln(
				"Are " + mtsAName + " and " + mtsBName + " " + semantic.toString() + " consistent? Verifying...");

		Consistency consistency = semantic.getConsistency(Collections.singleton(MTSConstants.TAU));
		boolean areConsistent = consistency.areConsistent(mtsA, mtsB);

		ltsOutput.outln(((areConsistent) ? "Yes" : "No") + ". (" + (System.currentTimeMillis() - initialTime) + "ms.)");

		if (!areConsistent && semantic == SemanticType.WEAK_ALPHABET) {
			if (!CollectionUtils.isSubCollection(mtsB.getActions(), mtsA.getActions())
					&& !CollectionUtils.isSubCollection(mtsA.getActions(), mtsB.getActions())) {
				// Weak Alphabet
				ltsOutput.outln("********************************************************************************");
				ltsOutput.outln("There is no weak alphabet consistency relation for these models.");
				ltsOutput.outln("This does NOT mean they are inconsistent [TOSEM].");
				ltsOutput.outln("Try checking consistency on their common alphabet. If they are inconsistent,");
				ltsOutput.outln("then the models currently tested are inconsistent [TOSEM].");
				ltsOutput.outln("*********************************************************************************");
			}
		}

		return areConsistent;
	}

	/**
	 * Applies minimisation to <code>composition</code> parameter. The
	 * minimisation semantics depends on the model type. If
	 * <code>compositeState</code> it's an MTS then MTS semantics is applied,
	 * otherwise LTS is applied. If <code>compositeState</code> it's an MTS then
	 * before applying minimisation, composition is applied.
	 * 
	 * The result of minimisation is setted to <code>composition</code> field of
	 * <code>compositeState</code>/
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	public static void minimise(CompositeState compositeState, LTSOutput ltsOutput) {

		if (MTSUtils.isMTSRepresentation(compositeState)) {
			Validate.isTrue(compositeState.getComposition() != null,
					"MTS ON-THE-FLY minimisation it is not implemented yet.");

			LabelledTransitionSystem compactState = (LabelledTransitionSystem) compositeState.getComposition();

			// compactState may be null, for instance after trying
			// to merge two inconsistent MTSs.
			if (compactState != null) {
				MTS<Long, String> mts = mtsMinimise(compactState, ltsOutput);
				compositeState.setComposition(
						MTSToAutomataConverter.getInstance().convert(mts, compositeState.getComposition().getName()));
			}

		} else {
			compositeState.minimise(ltsOutput);
		}
	}

	private static MTS<Long, String> mtsMinimise(LabelledTransitionSystem compactState, LTSOutput ltsOutput) {
		long initialTime = System.currentTimeMillis();
		ltsOutput.outln("Converting CompactState " + compactState.getName() + " to MTS...");
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(compactState);
		// ltsOutput.outln("MTS converted in "
		// + (System.currentTimeMillis() - initialTime) + "ms.");

		// initialTime = System.currentTimeMillis();
		ltsOutput.outln("Minimising with respect to refinement equivalence...");
		MTSMinimiser<String> minimiser = new MTSMinimiser<String>();

		// get the minimised MTS
		MTS<Long, String> minimisedMTS = minimiser.minimise(mts);
		ltsOutput.outln(compactState.getName() + " minimised in " + (System.currentTimeMillis() - initialTime) + "ms.");

		// minimisation sanity check
		ltsOutput.outln("Internal sanity check: Validating minimised and original are equivalent by simulation...");
		WeakSemantics weakSemantics = new WeakSemantics(Collections.singleton(MTSConstants.TAU));
		isRefinement(mts, " original " + compactState.getName(), minimisedMTS, " minimised " + compactState.getName(),
				weakSemantics, ltsOutput);
		isRefinement(minimisedMTS, " minimised " + compactState.getName(), mts, " original " + compactState.getName(),
				weakSemantics, ltsOutput);
		ltsOutput.outln(""); // leave an empty line
		return minimisedMTS;
	}

	/**
	 * Minimise <code>compactState</code> model using MTS or LTS semantic
	 * depending on the model type.
	 * 
	 * @param compactState
	 * @param ltsOutput
	 * @return
	 */
	public static LabelledTransitionSystem minimise(LabelledTransitionSystem compactState, LTSOutput ltsOutput) {
		if (MTSUtils.isMTSRepresentation(compactState)) {
			MTS<Long, String> mts = mtsMinimise(compactState, ltsOutput);
			return MTSToAutomataConverter.getInstance().convert(mts, compactState.getName());

		} else {
			Minimiser me = new Minimiser(compactState, ltsOutput);
			return me.minimise();
		}
	}

	/**
	 * Returns a vector with the optimistic representation of every model in the
	 * <code>originalMachines</code> parameter.
	 * 
	 * @param originalMachines
	 * @param output
	 * @return optimistic version of the original models.
	 */
	private static Vector<LabelledTransitionSystem> getOptimistModels(Vector<LabelledTransitionSystem> originalMachines,
			LTSOutput output) {
		Vector<LabelledTransitionSystem> retValue = new Vector<>();
		for (Iterator<LabelledTransitionSystem> ir = originalMachines.iterator(); ir.hasNext();) {
			LabelledTransitionSystem compactState = ir.next();
			retValue.add(getOptimisticModel(compactState, output));
		}
		return retValue;
	}

	/**
	 * Returns a vector with the pessimistic representation of every model in
	 * the <code>originalMachines</code> parameter.
	 * 
	 * @param originalMachines
	 * @return pessimistic version of the original models.
	 */
	private static Vector<LabelledTransitionSystem> getPessimisticModels(
			Vector<LabelledTransitionSystem> originalMachines) {
		Vector<LabelledTransitionSystem> retValue = new Vector<>();
		for (Iterator<LabelledTransitionSystem> ir = originalMachines.iterator(); ir.hasNext();) {
			LabelledTransitionSystem compactState = ir.next();
			retValue.add(getPessimistModel(compactState));
		}
		return retValue;
	}

	private static void checkSafety(CompositeState compositeState, LTSOutput ltsOutput, boolean checkDeadlocks) {
		long initialCurrentTimeMillis = System.currentTimeMillis();
		printLine(" ", ltsOutput);
		printLine(" ", ltsOutput);
		printLine("Starting safety check on " + compositeState.getName(), ltsOutput);

		LabelledTransitionSystem compactState = compositeState.getComposition();
		Vector<LabelledTransitionSystem> machines = compositeState.getMachines();
		String reference = "[Missing Reference]";

		printLine(" ", ltsOutput);
		printLine("Phase I: Does " + compositeState.getName() + "+ have errors?", ltsOutput);

		LabelledTransitionSystem optimisticModel = getOptimisticModel(compactState, ltsOutput);
		Vector<LabelledTransitionSystem> toCheck = new Vector<>();
		toCheck.add(optimisticModel);
		compositeState.setMachines(toCheck);

		compositeState.analyse(checkDeadlocks, ltsOutput);

		if (compositeState.getErrorTrace() == null || compositeState.getErrorTrace().isEmpty()) {
			// M+ |= FI
			printLine(compositeState.getName() + "+ does not have errors. Which means that...", ltsOutput);
			printLine("*******************************************************************************************",
					ltsOutput);
			printLine("NO ERRORS FOUND: All implementations of " + compositeState.getName() + " do not have errors."
					+ reference, ltsOutput);
			printLine("********************************************************************************************",
					ltsOutput);

		} else {
			printLine(compositeState.getName() + "+ does have errors. Which means that...", ltsOutput);
			printLine("This means that some implementations of " + compositeState.getName() + " have errors.",
					ltsOutput);
			printLine("", ltsOutput);

			LabelledTransitionSystem pessimisticModel = getPessimistModel(compactState);
			compositeState.setErrorTrace(new ArrayList<String>());

			if (!MTSUtils.isEmptyMTS(pessimisticModel)) {
				printLine("Phase II: Does " + compositeState.getName() + "- have errors?", ltsOutput);

				toCheck = new Vector<LabelledTransitionSystem>();
				toCheck.add(pessimisticModel);
				compositeState.setMachines(toCheck);
				compositeState.setComposition(null);

				compositeState.analyse(checkDeadlocks, ltsOutput);

				if (compositeState.getErrorTrace() == null || compositeState.getErrorTrace().isEmpty()) {
					// M- |= FI
					printLine(compositeState.getName() + "- does not have errors. Which means that...", ltsOutput);
					printLine("*****************************************************************", ltsOutput);
					printLine(
							"Model " + compositeState.getName() + " has some implementations with errors." + reference,
							ltsOutput);
					printLine("*****************************************************************", ltsOutput);
				} else {
					// M- !|= FI
					printLine(compositeState.getName() + "- does have errors. Which means that...", ltsOutput);
					printLine("*****************************************************************", ltsOutput);
					printLine("All implmentations of  " + compositeState.getName() + " have errors." + reference,
							ltsOutput);
					printLine("*****************************************************************", ltsOutput);
				}
			} else {

				// complementar la propiedad
				// armar el buchi de la propiedad.
				printLine("Phase II: " + compositeState.getName() + "- turned out to be empty. Does the complement of "
						+ compositeState.getName() + "+ have errors?", ltsOutput);

				LabelledTransitionSystem originalProperty = null;
				for (Iterator<LabelledTransitionSystem> it = machines.iterator(); it.hasNext();) {
					LabelledTransitionSystem aModel = it.next();
					if (MTSUtils.isPropertyModel(aModel)) {
						originalProperty = aModel;
						break;
					}
				}
				if (originalProperty == null) {
					Diagnostics.fatal("There must be a property to check.");
				}
				compositeState.setComposition(compactState);
				compositeState.setMachines(machines);

				LabelledTransitionSystem property = TransitionSystemDispatcher.buildBuchiFromProperty(compositeState,
						ltsOutput);

				// chequear M+ junto con la propiedad complementada y dar
				// resultado.
				compositeState.setComposition(getOptimisticModel(compactState, ltsOutput));
				Vector<LabelledTransitionSystem> propVector = new Vector<>();
				propVector.add(property);
				CompositeState propertyComp = new CompositeState(propVector);
				propertyComp.compose(ltsOutput);
				checkFLTL(compositeState, propertyComp, ltsOutput);
				// System.out.println("si llega aca es de milagro!");

				if (compositeState.getErrorTrace() == null || compositeState.getErrorTrace().isEmpty()) {
					// M+ !|= NOT FI

					printLine(
							compositeState.getName()
									+ "+ does not satisfy the complement of the property. This means that... ",
							ltsOutput);
					printLine("*****************************************************************", ltsOutput);
					printLine("All implmentations of  " + compositeState.getName() + " have errors." + reference,
							ltsOutput);
					printLine("*****************************************************************", ltsOutput);

				} else {
					// M+ |= NOT FI
					printLine(compositeState.getName()
							+ "+ satisfies the complement of the property. This means that... ", ltsOutput);
					printLine("*****************************************************************", ltsOutput);
					printLine("Model " + compositeState.getName() + " has implementations with errors." + reference,
							ltsOutput);
					printLine("*****************************************************************", ltsOutput);
				}
			}
		}

		printLine(" ", ltsOutput);
		printLine("*******************************************", ltsOutput);
		printLine("Total safety analysis time: " + (System.currentTimeMillis() - initialCurrentTimeMillis), ltsOutput);
		printLine("*******************************************", ltsOutput);

		// leave the original compositeState intact
		compositeState.setComposition(compactState);
		compositeState.setMachines(machines);
	}

	private static LabelledTransitionSystem buildBuchiFromProperty(CompositeState compositeState, LTSOutput ltsOutput) {

		// mover a mtsUtils
		LabelledTransitionSystem compactState = getProperty(compositeState);
		MTS<Long, String> property = AutomataToMTSConverter.getInstance().convert(compactState);

		Long trapState = Collections.max(property.getStates()) + 1;

		MTSPropertyToBuchiConverter.convert(property, trapState, "@" + compactState.getName());
		return MTSToAutomataConverter.getInstance().convert(property, compactState.getName());
	}

	private static LabelledTransitionSystem getProperty(CompositeState compositeState) {
		for (Iterator<LabelledTransitionSystem> it = compositeState.getMachines().iterator(); it.hasNext();) {
			LabelledTransitionSystem compactState = it.next();
			if (MTSUtils.isPropertyModel(compactState)) {
				return compactState;
			}
		}
		Diagnostics.fatal("There must be exactly one property to check");
		return null;
	}

	/**
	 * Returns true if the <code>compositeState</code> parameter has any
	 * deadlock free implementation.
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	public static boolean hasCompositionDeadlockFreeImplementations(CompositeState compositeState,
			LTSOutput ltsOutput) {
		applyComposition(compositeState, ltsOutput);
		if (MTSUtils.isMTSRepresentation(compositeState)) {
			MTS<Long, String> mts = AutomataToMTSConverter.getInstance()
					.convert((LabelledTransitionSystem) compositeState.getComposition());

			String reference = "[Mising Reference]";
			int deadlockStatus = MTSAFacade.getDeadlockStatus(mts);
			if (deadlockStatus == 1) {
				String output = "All implementations of " + compositeState.getName() + " have a deadlock state."
						+ reference;
				ltsOutput.outln(output);

				return false;
			} else if (deadlockStatus == 2) {
				String output = "All implementations of " + compositeState.getName() + " are deadlock free."
						+ reference;
				ltsOutput.outln(output);
			} else {
				String output = "Some implementations of " + compositeState.getName()
						+ " are deadlock free while others have a deadlock state." + reference;
				ltsOutput.outln(output);
			}
		} else {
			compositeState.analyse(true, ltsOutput);
		}
		return true;
	}

	private static void printLine(String toPrint, LTSOutput ltsOutput) {
		ltsOutput.outln(toPrint);
	}

	/**
	 * Checks if the model <code>compositeState</code> satisfies the property
	 * <code>ltlProperty</code>. If <code>compositeState</code> is an LTS, then
	 * the traditional LTS model checking algorithm is applied. Otherwise MTS
	 * model checking algorithm is applied.
	 * 
	 * 
	 * @param compositeState
	 * @param ltlProperty
	 * @param notLtlProperty
	 * @param fairCheck
	 * @param ltsOutput
	 */
	public static void checkFLTL(CompositeState compositeState, CompositeState ltlProperty, LTSOutput ltsOutput) {
		if (compositeState.checkCompatible || compositeState.makeSyncController) {
			checkControllerFLTL(compositeState, ltlProperty, ltsOutput);
		} else {
			compositeState.checkLTL(ltsOutput, ltlProperty);
		}

	}

	/**
	 * This method applies the specified set of operations to the compositeState
	 * parameter, including parallel composition, and then builds a new
	 * CompositeState which machines vector is filled only with the just
	 * generated composition.
	 */
	private static void checkControllerFLTL(CompositeState compositeState, CompositeState ltlProperty,
			LTSOutput ltsOutput) {
		if (compositeState.getComposition() == null) {
			applyComposition(compositeState, ltsOutput);
		}
		Vector<LabelledTransitionSystem> machines = new Vector<>();
		machines.add(compositeState.getComposition());
		CompositeState cs = new CompositeState(compositeState.getName(), machines);
		cs.checkLTL(ltsOutput, ltlProperty);
	}

	private static LabelledTransitionSystem saved = null;

	/**
	 * Builds the <b>abstract</b> version of the original model. The result it's
	 * builded following the following procedure: <br>
	 * Firstly a state (called trap state) with loop transitions on every label
	 * in the alphabet is added to the abstract model. Then from every state in
	 * <code>compactState</code> and for every label in
	 * <code>compactState</code>s alphabet for which there are no outgoing
	 * transition from it, it is added one transition to trap state.
	 * 
	 * @param compactState
	 * @param output
	 * @return
	 */
	public static LabelledTransitionSystem getAbstractModel(LabelledTransitionSystem compactState, LTSOutput output) {
		long initialTime = System.currentTimeMillis();
		MTSAbstractBuilder abstractBuilder = new MTSAbstractBuilder();
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(compactState);
		Set<String> toDelete = new HashSet<String>();
		toDelete.add(MTSConstants.ASTERIX);
		toDelete.add(MTSConstants.AT);
		MTSUtils.removeActionsFromAlphabet(mts, toDelete);

		MTS<Long, String> abstractModel = abstractBuilder.getAbstractModel(mts);
		output.outln("Abstract model generated for " + compactState.getName() + " in: "
				+ (System.currentTimeMillis() - initialTime) + "ms.");

		return MTSToAutomataConverter.getInstance().convert(abstractModel, compactState.getName());
	}

	/**
	 * Sets the result of <code>getAbstractModel</code> method applied to
	 * composition field of <code>compositeState</code> to the composition field
	 * again.
	 * 
	 * @param compositeState
	 * @param output
	 */
	public static void makeAbstractModel(CompositeState compositeState, LTSOutput output) {
		if (compositeState.getComposition() != null) {
			compositeState.setComposition(getAbstractModel(compositeState.getComposition(), output));
		}
	}

	/**
	 * Computes the transitive tau-closure for compactState. It propagates may
	 * transitions.
	 * 
	 * @param compactState
	 * @param output
	 * @return
	 */
	public static LabelledTransitionSystem getTauClosure(LabelledTransitionSystem compactState, LTSOutput output) {
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(compactState);
		output.outln("Applying tau clousure [bisimulation-based]");
		MTSAFacade.applyClosure(mts, Collections.singleton(MTSConstants.TAU));
		return MTSToAutomataConverter.getInstance().convert(mts, compactState.getName());
	}

	/**
	 * Sets the result of <code>getClousureModel</code> method applied to
	 * composition field of <code>compositeState</code> to the composition field
	 * again.
	 * 
	 * @param compositeState
	 * @param output
	 */
	public static void makeClosureModel(CompositeState compositeState, LTSOutput output) {
		long initialTime = System.currentTimeMillis();
		if (compositeState.getComposition() != null) {
			LabelledTransitionSystem tauClosure = TransitionSystemDispatcher
					.getTauClosure(compositeState.getComposition(), output);
			compositeState.setComposition(tauClosure);
			output.outln("Clousure model generated for " + compositeState.getName() + " in: "
					+ (System.currentTimeMillis() - initialTime) + "ms.");
		}
	}

	/**
	 * For every state in which are choices transform those transitions to maybe
	 * transitions.
	 * 
	 * @param compactState
	 * @param output
	 * @return
	 */
	public static LabelledTransitionSystem makeMTSConstraintModel(LabelledTransitionSystem compactState,
			LTSOutput output) {
		MTS<Long, String> constrained = AutomataToMTSConverter.getInstance().convert(compactState);

		long initialTime = System.currentTimeMillis();

		Set<String> toDelete = new HashSet<String>();
		toDelete.add(MTSConstants.ASTERIX);
		toDelete.add(MTSConstants.AT);
		MTSUtils.removeActionsFromAlphabet(constrained, toDelete);

		MTSConstraintBuilder constraintBuilder = new MTSConstraintBuilder();
		constraintBuilder.makeConstrainedModel(constrained);

		output.outln("Constrained model generated for " + compactState.getName() + " in: "
				+ (System.currentTimeMillis() - initialTime) + "ms.");
		return MTSToAutomataConverter.getInstance().convert(constrained, compactState.getName());
	}

	public static void checkProgress(CompositeState compositeState, LTSOutput output) {

		if (MTSUtils.isMTSRepresentation(compositeState)) {
			Diagnostics.fatal("MTS Progress check has not been defined yet.");
		} else {
			if (compositeState.checkCompatible || compositeState.makeSyncController) {
				Vector<LabelledTransitionSystem> machines = new Vector<LabelledTransitionSystem>();
				machines.add(compositeState.getComposition());
				CompositeState cs = new CompositeState(compositeState.getName(), machines);
				cs.checkProgress(output);
			} else {
				compositeState.checkProgress(output);
			}
		}
	}

	private static Map<Long, List<String>> lastControllerStateLabels;

	public static Map<Long, List<String>> getLastControllerStateLabels() {
		return lastControllerStateLabels;
	}

	/**
	 * Generate the appropriate Animator depending on the type of the Composite
	 * State
	 * 
	 * @param compositeState
	 *            The machine to animate
	 * @param output
	 * @param eventManager
	 *            Tell this object whenever the current state within the machine
	 *            changes
	 * @return Animator depending on the type of the Composite State
	 */

	public static ltsa.lts.animator.Animator generateAnimator(CompositeState compositeState, LTSOutput output,
			ltsa.lts.gui.EventManager eventManager) {
		// return new MTSAnimator(compositeState, eventManager);

		if (MTSUtils.isMTSRepresentation(compositeState)) {
			return new MTSAnimator(compositeState, eventManager);
		} else {
			Analyser analyser = new Analyser(compositeState, output, eventManager);
			// DIPI: here we could remove the maybe transitions from the
			// alphabet
			// return new AnimatorDecorator(analyser);
			return analyser;
		}
	}

	/**
	 * Returns the number of machines n in the CompositeState, adding one for
	 * MTS because of the capability to draw the composite
	 * 
	 * @param compositeState
	 *            The machine to draw
	 * @return Number of machines to draw
	 */

	public static int numberMachinesToDraw(CompositeState compositeState) {
		boolean isComposite = (compositeState != null && compositeState.getComposition() != null);
		int numMachines = compositeState.getMachines().size();
		;
		if (MTSUtils.isMTSRepresentation(compositeState)) {
			if (isComposite)
				return numMachines + 1;
			else
				return numMachines;
		} else
			return numMachines;
	}

	/**
	 * 
	 * @param compositeState
	 * @param output
	 */
	public static void makeComponentModel(CompositeState compositeState, LTSOutput output) {

		if (compositeState.getComposition() != null) {
			if (compositeState.getComponentAlphabet() != null) {
				compositeState.setComposition(getComponentModel(compositeState.getComposition(),
						compositeState.getComponentAlphabet(), output));
			} else {
				StringBuffer errorMsg = new StringBuffer("The alphabet for component ");
				errorMsg.append(compositeState.getName()).append(" is missing");
				Diagnostics.fatal(errorMsg.toString());
			}
		}
	}

	/**
	 * TODO: this method should be delted. A method to get the whole
	 * decomposition will be provided
	 * 
	 * @param composition
	 * @param componentAlphabet
	 * @param output
	 * @return
	 */
	private static LabelledTransitionSystem getComponentModel(LabelledTransitionSystem composition,
			Vector<String> componentAlphabet, LTSOutput output) {

		MTS<Long, String> compositionMTS = AutomataToMTSConverter.getInstance().convert(composition);

		MTS<Long, String> componentMTS = MTSAFacade.getComponent(compositionMTS,
				new HashSet<String>(componentAlphabet));

		return MTSToAutomataConverter.getInstance().convert(componentMTS, composition.getName());
	}

	public static void makeProperty(CompositeState compositeState, LTSOutput output) {

		if (compositeState.getComposition() != null) {
			if (!MTSUtils.isMTSRepresentation(compositeState)) {
				MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(compositeState.getComposition());

				// This Function could be mts.getAlphabet() [[
				Set<Long> states = mts.getStates();
				Set<String> alphabet = new HashSet<String>();
				Map<Long, BinaryRelation<String, Long>> allTransitions = mts.getTransitions(TransitionType.REQUIRED);
				for (Entry<Long, BinaryRelation<String, Long>> transition : allTransitions.entrySet()) {
					for (Pair<String, Long> pair : transition.getValue()) {
						alphabet.add(pair.getFirst());
					}

				}
				// ]]
				mts.addState(new Long(-1));
				for (Long state : states) {
					if (state != -1) {
						Set<String> posibleTransitions = new HashSet<String>();
						posibleTransitions.addAll(alphabet);
						BinaryRelation<String, Long> transitionsFromState = mts.getTransitions(state,
								TransitionType.REQUIRED);
						for (Pair<String, Long> pair : transitionsFromState) {
							posibleTransitions.remove(pair.getFirst());

						}

						for (String transition : posibleTransitions) {
							mts.addTransition(state, transition, new Long(-1), TransitionType.REQUIRED);
						}
					}
				}

				MTSToAutomataConverter converter = new MTSToAutomataConverter();
				LabelledTransitionSystem compactState = (LabelledTransitionSystem) converter.convert(mts,
						compositeState.getName());
				compositeState.setComposition(compactState);
				output.outln("property model for: " + compositeState.getName());
			} else {
				// TODO: CHECK WHAT TO DO IN THIS CASE
				Diagnostics.fatal("Property keword is not defined for MTS");
			}
		}

	}
}
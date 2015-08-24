package dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import lts.Analyser;
import lts.CompactState;
import lts.CompositeState;
import lts.Diagnostics;
import lts.EmptyLTSOuput;
import lts.LTSOutput;
import lts.Minimiser;
import lts.Symbol;
import lts.distribution.DistributionDefinition;
import lts.distribution.DistributionTransformationException;
import lts.util.MTSUtils;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.ListUtils;
import org.apache.commons.lang.Validate;

import ui.MTSAnimator;
import updatingControllers.synthesis.UpdatingControllerSynthesizer;
import updatingControllers.structures.UpdatingControllerCompositeState;
import updatingControllers.synthesis.UpdatingControllerHandler;
import ac.ic.doc.commons.relations.BinaryRelation;
import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.distribution.DistributionFacade;
import ac.ic.doc.distribution.model.AlphabetDistribution;
import ac.ic.doc.distribution.model.DistributionFeedbackItem;
import ac.ic.doc.distribution.model.DistributionResult;
import ac.ic.doc.mtstools.facade.MTSAFacade;
import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.MTS.TransitionType;
import ac.ic.doc.mtstools.model.MTSConstants;
import ac.ic.doc.mtstools.model.Refinement;
import ac.ic.doc.mtstools.model.RefinementByRelation;
import ac.ic.doc.mtstools.model.SemanticType;
import ac.ic.doc.mtstools.model.impl.LTSSimulationSemantics;
import ac.ic.doc.mtstools.model.impl.MDP;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import ac.ic.doc.mtstools.model.impl.MTSDeterminiser;
import ac.ic.doc.mtstools.model.impl.MTSMinimiser;
import ac.ic.doc.mtstools.model.impl.MTSMultipleComposer;
import ac.ic.doc.mtstools.model.impl.PlusCARulesApplier;
import ac.ic.doc.mtstools.model.impl.WeakAlphabetPlusCROperator;
import ac.ic.doc.mtstools.model.impl.WeakSemantics;
import ac.ic.doc.mtstools.model.operations.Consistency;
import ac.ic.doc.mtstools.model.operations.MTSAbstractBuilder;
import ac.ic.doc.mtstools.model.operations.MTSConstraintBuilder;
import ac.ic.doc.mtstools.model.operations.TauRemoval;
import ac.ic.doc.mtstools.model.operations.TraceInclusionClosure;
import ac.ic.doc.mtstools.model.operations.impl.MTSPropertyToBuchiConverter;
import ac.ic.doc.mtstools.model.operations.impl.WeakAlphabetMergeBuilder;
import ac.ic.doc.mtstools.model.predicates.IsDeterministicMTSPredicate;
import ac.ic.doc.mtstools.util.fsp.AutomataToMDPConverter;
import ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ac.ic.doc.mtstools.util.fsp.MDPToAutomataConverter;
import ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ac.ic.doc.mtstools.utils.GenericMTSToLongStringMTSConverter;
import control.ControlStackSynthesiser;
import control.util.ControlConstants;
import control.util.ControllerUtils;
import controller.ControllerSynthesisFacade;
import controller.HeuristicControllerSynthesiser;
import controller.NondetControlProblem;
import controller.game.gr.StrategyState;
import controller.game.model.Assumptions;
import controller.game.model.Guarantees;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.GRGame;
import controller.synchronous.SynchronousControlProblem;

/**
 * This class consists exclusively of static methods that operate on or return
 * CompactState, CompositeState and MTS.
 * 
 * In the MTSA architecture this class is the communication layer between the graphical interface, LTSA Core and MTSACore. 
 * None of them communicates each other directly, they do it through this class.
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
	public static CompactState getOptimisticModel(CompactState compactState, LTSOutput output) {
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(compactState);
		long initialTime = System.currentTimeMillis();
		mts = MTSAFacade.getOptimisticModel(mts);
		output.outln("MTS Representation: Optimistic operator applied to " + compactState.getName() + " generated in: "
				+ (System.currentTimeMillis() - initialTime)
				+ "ms.");

		return MTSToAutomataConverter.getInstance().convert(mts, compactState.name);
	}

	/**
	 * Given a CompactState model returns the pessimistic version of it.
	 * 
	 * @param composition
	 * @return the pessimistic representation of the compactState parameter
	 */
	public static CompactState getPessimistModel(CompactState composition) {
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(composition);
		mts = MTSAFacade.getPesimisticModel(mts);
		return MTSToAutomataConverter.getInstance().convert(mts, composition.name);
	}

	/**
	 * Given a CompositeState model this method builds the optimistic version of
	 * it. If the composition field of compositeState parameter it's not null
	 * then it's replaced for its optimistic version. Else every compactState
	 * inside compositeState parameter it's replaced for its optimistic version.
	 * 
	 * @param compositeState
	 */
	@SuppressWarnings("unchecked")
	public static void makeOptimisticModel(CompositeState compositeState, LTSOutput ltsOutput) {
		if (MTSUtils.isMTSRepresentation(compositeState)) {
			if (compositeState.getComposition() == null) {
					long initialTime = System.currentTimeMillis();
					compositeState.setMachines(TransitionSystemDispatcher
							.getOptimistModels(compositeState.getMachines(), new EmptyLTSOuput()));
					ltsOutput
							.outln("MTS Representation: Optimistic model generated for all automatas in: "
									+ (System.currentTimeMillis() - initialTime)
									+ "ms.");
			} else {
					compositeState.setMachines(TransitionSystemDispatcher
							.getOptimistModels(compositeState.getMachines(), new EmptyLTSOuput()));
					compositeState.setComposition(TransitionSystemDispatcher
							.getOptimisticModel(compositeState
									.getComposition(), ltsOutput));
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
	@SuppressWarnings("unchecked")
	public static void makePessimisticModel(CompositeState compositeState,
			LTSOutput ltsOutput) {
		if (MTSUtils.isMTSRepresentation(compositeState)) {
			if (compositeState.getComposition() == null) {
				if (MTSUtils.isMTSRepresentation(compositeState)) {
					long initialTime = System.currentTimeMillis();
					compositeState.setMachines(getPessimisticModels(compositeState.getMachines()));
					ltsOutput.outln("MTS Representation: Pessimistic model generated for all automatas in: "
									+ (System.currentTimeMillis() - initialTime)
									+ "ms.");
				}
				applyComposition(compositeState, ltsOutput);
			} else {
				long initialTime = System.currentTimeMillis();
				compositeState.setComposition(TransitionSystemDispatcher
						.getPessimistModel(compositeState.getComposition()));
				ltsOutput
						.outln("MTS Representation: Pessimistic model generated for composition in: "
								+ (System.currentTimeMillis() - initialTime)
								+ "ms.");
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
	public static void checkSafety(CompositeState compositeState,
			LTSOutput ltsOutput) {
		checkSafetyOrDeadlock(false, compositeState, ltsOutput);
	}

	private static void checkSafetyOrDeadlock(boolean checkDeadlocks,
			CompositeState compositeState, LTSOutput ltsOutput) {
		if (MTSUtils.isMTSRepresentation(compositeState)) {

			if (hasCompositionDeadlockFreeImplementations(compositeState,
					ltsOutput)) {
				checkSafety(compositeState, ltsOutput, checkDeadlocks);
			} else {
				ltsOutput
						.outln("*****************************************************************************************");
				ltsOutput
						.outln("Model must have at least one deadlock free implementation for a Safety or Deadlock check.");
				ltsOutput
						.outln("*****************************************************************************************");
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
		
		// switch old actions to actions without old after having the winning game
		if (Symbol.UPDATING_CONTROLLER == toCompose.getCompositionType() && toCompose.composition != null){
			UpdatingControllerHandler.removeOldTransitions(toCompose);
		}
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
		case Symbol.UPDATING_CONTROLLER:
			applyUpdatingController(toCompose, ltsOutput);
			break;
		case Symbol.OR:
		default:
			parallelComposition(toCompose, ltsOutput);
		}
	}

	private static void applyUpdatingController(CompositeState toCompose, LTSOutput output) {
		if (! (toCompose instanceof UpdatingControllerCompositeState)){
			output.outln("MTSA tool is trying to solve an updatingController problem but the CompositeState given "
					+ "is not appropiated");
		}
		UpdatingControllerSynthesizer.generateController((UpdatingControllerCompositeState) toCompose, output);
	}

	@SuppressWarnings("unchecked")
	private static void merge(CompositeState composition, LTSOutput ltsOutput) {
		if (composition.getComposition() == null) {
			ArrayList<MTS<Long, String>> toCompose = new ArrayList<MTS<Long, String>>();
			List<String> names = new ArrayList<String>();

			for (Iterator<CompactState> it = composition.getMachines().iterator(); it.hasNext();) {
				CompactState compactState = it.next();
				toCompose.add(AutomataToMTSConverter.getInstance().convert(
						compactState));
				names.add(compactState.name);
			}

			ltsOutput.outln("Applying Merge Operator to MTSs...(" + composition.name + ")");
			long initialTime = System.currentTimeMillis();

			if (toCompose.size() > 2) {
				ltsOutput
						.outln("Warning: Merge is being applied to more than two models. Pair-wise merging will be performed. See [FM06] for details on associativity of merge");
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
						//Same alphabets
						//Strong or Weak
						ltsOutput.outln("***************************************************************");
						ltsOutput.outln("There is no weak consistency relation for these models.");
						ltsOutput.outln("This means they are inconsistent and  cannot be merged [TOSEM].");
						ltsOutput.outln("****************************************************************");
					} else {
						//Weak Alphabet
						ltsOutput.outln("********************************************************************************");
						ltsOutput.outln("There is no weak alphabet consistency relation for these models.");
						ltsOutput.outln("This does NOT mean they are inconsistent, however they cannot be merged [TOSEM].");
						ltsOutput.outln("Try merging them on their common alphabet. If they are still inconsistent,");
						ltsOutput.outln("then the models currently being merged are inconsistent [TOSEM].");
						ltsOutput.outln("*********************************************************************************");
					}
					return;
				}
				

				ltsOutput.outln("Merge operator applied in "
						+ (System.currentTimeMillis() - initialTime) + "ms.");
				
				Set<String> alphabetAminusB = new HashSet<String>(
						CollectionUtils.subtract(mtsA.getActions(), mtsB.getActions()));
				Set<String> alphabetBminusA = new HashSet<String>(
						CollectionUtils.subtract(mtsB.getActions(), mtsA.getActions()));

				alphabetAminusB.addAll(tau);
				alphabetBminusA.addAll(tau);

				RefinementByRelation refA = new WeakSemantics(alphabetBminusA);
				RefinementByRelation refB = new WeakSemantics(alphabetAminusB);

				ltsOutput.outln("Internal sanity check: Validating merge is a common refinement...");
				isRefinement(
						merge,
						composition.name,
						mtsA,
						mtsAName,
						refA, ltsOutput);
				isRefinement(
						merge,
						composition.name,
						mtsB,
						names.get(i - 1),
						refB, ltsOutput);
				
				mtsA = merge;
				mtsAName = "(" + mtsAName + "++" + names.get(i - 1) + ")";  
				
				ltsOutput.outln(""); // leave an empty line
			}
			
			MTS<Long, String> mts = new GenericMTSToLongStringMTSConverter().transform(merge);
			
			composition.setComposition(MTSToAutomataConverter.getInstance().convert(mts, composition.name));
			
		}
	}

	/***************************************************************************
	 * Applies parallel composition over all model instances in machines field
	 * of <code>compositeState</code> and it's result is set to composition
	 * field of <code>compositeState</code>.
	 * 
	 * If <code>compositeState</code> is an LTS then LTS parallel composition
	 * is applied. If <code>compositeState</code> is an MTS then MTS parallel
	 * composition is applied.
	 * 
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	public static void parallelComposition(CompositeState compositeState, LTSOutput ltsOutput) {
		if (MTSUtils.isMTSRepresentation(compositeState)) {
			
			long initialTime = System.currentTimeMillis();
			ltsOutput.outln("Converting MTSs from " + compositeState.name);
			ltsOutput.outln("Composing MTSs from " + compositeState.name);

			MTS<Long, String> mts = MTSUtils.getMTSComposition(compositeState);
			
			ltsOutput.outln("MTSs composed in "	+ (System.currentTimeMillis() - initialTime) + "ms.\n");

				CompactState convert = MTSToAutomataConverter.getInstance().convert(mts, compositeState.name);
				compositeState.setComposition(convert);
				
			}
			else if (compositeState.makeMDP)
			{
			  mdpComposeAbstraction(compositeState, ltsOutput);
		  }
			else if (compositeState.makeEnactment)
			{
			  mdpComposeEnactment(compositeState, compositeState.enactmentControlled, ltsOutput);
			} else {
				compositeState.compose(ltsOutput);
			}
	}


	/*
	Composition (R*A) of an abstract environment (A) with a less abstract one (R)
	Daniel Sykes 2014
	*/
	private static void mdpComposeAbstraction(CompositeState toCompose, LTSOutput ltsOutput)
  {
	  long initialTime = System.currentTimeMillis();
	  if (toCompose.machines.size() >= 2)
	  {
	    MDP lastMachine = AutomataToMDPConverter.getInstance().convert(toCompose.machines.get(0));
	    for (int i = 1; i < toCompose.machines.size(); i++)
	    {
	      MDP mdp1 = lastMachine;
	      MDP mdp2 = AutomataToMDPConverter.getInstance().convert(toCompose.machines.get(i));
	      System.out.println(mdp1);
        System.out.println(mdp2);
        
	      //with a better class hierarchy this shouldn't be needed
	      MTS<Long,String> mts1 = AutomataToMDPConverter.getInstance().convert(mdp1);
	      MTS<Long,String> mts2 = AutomataToMDPConverter.getInstance().convert(mdp2);
	      BinaryRelation<Long,Long> simulation = new LTSSimulationSemantics().getLargestRelation(mts1, mts2);
	      if (simulation.size() == 0)
          ltsOutput.outln("WARNING: simulation is empty");
	      
	      //CompositeState goalBuchi = AssertDefinition.compile(ltsOutput, "MDPGOAL");
	      
	      System.out.println("Name of *A* is "+toCompose.machines.get(i).name);  //
	      MDP composed = MDP.composeAbstraction(mdp1, mdp2, simulation); //MDP.compose(mdp1, mdp2);//
	      System.out.println("\n"+composed);
	      lastMachine = composed;
	    }
	    
	    toCompose.composition = MDPToAutomataConverter.getInstance().convert(lastMachine, toCompose.getName());
	    lastMachine.writePrismFile("h:/contsynth/controllers/"+toCompose.name+".nm", toCompose.name);
	    
	    ltsOutput.outln("MDPs composed in "  + (System.currentTimeMillis() - initialTime) + "ms.\n");
	  }
  }
	
	/*
	Enactment: the composition of a controller with a probabilistic environment
	Daniel Sykes 2014
	*/
	private static void mdpComposeEnactment(CompositeState toCompose, List<String> controlledActions, LTSOutput ltsOutput)
  {
    long initialTime = System.currentTimeMillis();
    if (toCompose.machines.size() >= 2)
    {
      MDP lastMachine = AutomataToMDPConverter.getInstance().convert(toCompose.machines.get(0));
      for (int i = 1; i < toCompose.machines.size(); i++)
      {
        MDP mdp1 = lastMachine;
        MDP mdp2 = AutomataToMDPConverter.getInstance().convert(toCompose.machines.get(i));
        System.out.println(mdp1);
        System.out.println(mdp2);
        
        System.out.println("Name of *C* is "+toCompose.machines.get(i).name);
        
        //apply state labels stored from synthesis
        for (Long s : mdp2.getStates())
        {
          List<String> labels = lastControllerStateLabels.get(s);
          for (String lab : labels)
            mdp2.addStateLabel(s, lab);
        }
        
        MDP composed = MDP.composeEnactment(mdp1, mdp2, controlledActions);
        System.out.println("\n"+composed);
        lastMachine = composed;
      }
      
      toCompose.composition = MDPToAutomataConverter.getInstance().convert(lastMachine, toCompose.getName());
      lastMachine.writePrismFile("h:/contsynth/controllers/"+toCompose.name+".nm", toCompose.name);
      
      ltsOutput.outln("MDPs composed in "  + (System.currentTimeMillis() - initialTime) + "ms.\n");
    }
  }

  /**
	 * Applies plus CR merge operator over the models in <code>machines</code>
	 * field of <code>compositeState</code>. The result of +CR is set to
	 * the <code>composition</code> field of <code>compositeState</code> .
	 * 
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	@SuppressWarnings("unchecked")
	public static void applyPlusCROperator(CompositeState compositeState, LTSOutput ltsOutput) {
		if (compositeState.getComposition() == null) {
			ArrayList<MTS<Long, String>> toCompose = new ArrayList<MTS<Long, String>>();

			long initialTime = System.currentTimeMillis();
			ltsOutput.outln("Converting CompactState to MTSs...");

			for (Iterator<CompactState> it = compositeState.getMachines().iterator(); it.hasNext();) {
				CompactState compactState = it.next();
				toCompose.add(AutomataToMTSConverter.getInstance().convert(
						compactState));
			}
			//ltsOutput.outln("MTSs converted in "
			//		+ (System.currentTimeMillis() - initialTime) + "ms.");

			ltsOutput.outln("Applying +CR Operator to MTSs...");
			initialTime = System.currentTimeMillis();

			Set<String> silentActions = Collections.singleton(MTSConstants.TAU);
			assert (toCompose.size() >= 2);
			Iterator<MTS<Long, String>> iterator = toCompose.iterator();

			MTS<Long, String> merge = iterator.next();
			while (iterator.hasNext()) {
				MTS<Long, String> mts = iterator.next();
				MTS<?, String> cr = new WeakAlphabetPlusCROperator(silentActions).compose(
						merge, mts);
				merge = new GenericMTSToLongStringMTSConverter().transform(cr);

			}

			ltsOutput.outln("+CR operator applied in "
					+ (System.currentTimeMillis() - initialTime) + "ms.");
			compositeState.setComposition(MTSToAutomataConverter.getInstance()
					.convert(merge, compositeState.name));
			ltsOutput.outln(""); // leave an empty line
			// ((CompositeState) composition).applyOperations(ltsOutput);
		}
	}

	/**
	 * Applies plus CR merge operator over the models in <code>machines</code>
	 * field of <code>compositeState</code>. The result of +CR is set to
	 * the <code>composition</code> field of <code>compositeState</code> .
	 * 
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	@SuppressWarnings("unchecked")
	public static void applyPlusCAOperator(CompositeState compositeState,
			LTSOutput ltsOutput) {
		if (compositeState.getComposition() == null) {
			PlusCARulesApplier plusCARulesApplier = new PlusCARulesApplier();
			String plusSymbol = "+CA";

			List<MTS<Long, String>> toCompose = new ArrayList<MTS<Long, String>>();

			long initialTime = System.currentTimeMillis();
			ltsOutput.outln("Converting CompactState to MTSs...");

			for (Iterator<CompactState> it = compositeState.getMachines().iterator(); it.hasNext();) {
				CompactState compactState = it.next();
				toCompose.add(AutomataToMTSConverter.getInstance().convert(
						compactState));
			}
			ltsOutput.outln("MTSs converted in "
					+ (System.currentTimeMillis() - initialTime) + "ms.");

			ltsOutput.outln("Applying " + plusSymbol + " operator to MTSs...");
			initialTime = System.currentTimeMillis();
			MTS<Long, String> merge = new MTSMultipleComposer<Long, String>(
					plusCARulesApplier).compose(toCompose);

			ltsOutput.outln(plusSymbol + " operator applied in "
					+ (System.currentTimeMillis() - initialTime) + "ms.");

			compositeState.setComposition(MTSToAutomataConverter.getInstance()
					.convert(merge, compositeState.name));
			ltsOutput.outln(""); // leave an empty line
			// ((CompositeState) composition).applyOperations(ltsOutput);
		}
	}

	/**
	 * Applies determinisation to <code>composition</code> parameter. The
	 * determinisation semantics depends on the model type. If
	 * <code>compositeState</code> it's an MTS then MTS semantics is applied,
	 * otherwise LTS is applied. If <code>compositeState</code> it's an MTS
	 * then before applying determinisation, composition is applied.
	 * 
	 * The result of determinisation is setted to <code>composition</code>
	 * field of <code>compositeState</code>/
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	public static void determinise(CompositeState compositeState, LTSOutput ltsOutput) {
		TransitionSystemDispatcher.compose(compositeState, ltsOutput);
		CompactState deterministic = determinise(compositeState.getComposition(), ltsOutput);
		compositeState.setComposition(deterministic);
	}

	/**
	 * Applies determinisation over <code>lts</code> depending on the model
	 * type it can be used LTS or MTS semantic.
	 * 
	 * @param lts
	 * @param ltsOutput
	 *            output support
	 * @return deterministic version of <code>lts</code>
	 */
	public static CompactState determinise(CompactState lts, LTSOutput ltsOutput) {
		CompactState compactState = (CompactState) lts;
		if (MTSUtils.isMTSRepresentation(compactState)) {
			long initialTime = System.currentTimeMillis();
//			ltsOutput.outln("Converting CompactState to MTS...");
			MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(compactState);
			//ltsOutput.outln("MTS converted in "
			//		+ (System.currentTimeMillis() - initialTime) + "ms.");
			//initialTime = System.currentTimeMillis();
			ltsOutput.outln("Determinising ...");
			MTSDeterminiser determiniser = new MTSDeterminiser(mts, true);
			mts = determiniser.determinize();
			ltsOutput.outln("Model " + lts.name + " determinised in " + (System.currentTimeMillis() - initialTime) + "ms.");
			return MTSToAutomataConverter.getInstance().convert(mts, compactState.name, MTSUtils.isMTSRepresentation(compactState));
		} 
		else {
			Vector<CompactState> toDet = new Vector<CompactState>();
			toDet.add(compactState);
			CompositeState compositeState = new CompositeState(toDet);
			compositeState.compose(ltsOutput);
			compositeState.determinise(ltsOutput);
			
			ltsOutput.outln("Determinising ...");
			Minimiser d = new Minimiser(compactState,ltsOutput);
      return d.trace_minimise();
      //if (isProperty) composition.makeProperty();
			
			//return compositeState.getComposition();
//			compositeState.determinise(ltsOutput);
		}
//		return lts;
	}

	public static boolean isLTSRefinement(CompactState refines, CompactState refined, LTSOutput output)
	{
	  LTSSimulationSemantics ss = new LTSSimulationSemantics(); //previously using emptySet
	  MTS<Long, String> refinedMTS = AutomataToMTSConverter.getInstance().convert(refined);
    MTS<Long, String> refinesMTS = AutomataToMTSConverter.getInstance().convert(refines);
    return isRefinement(refinesMTS, refines.name, refinedMTS, refined.name, ss, output);
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
	public static boolean isRefinement(CompactState refines,
			CompactState refined, SemanticType semantic, LTSOutput ltsOutput) {

		Refinement refinement = semantic.getRefinement();
		MTS<Long, String> refinedMTS = AutomataToMTSConverter.getInstance()
				.convert((CompactState) refined);
		MTS<Long, String> refinesMTS = AutomataToMTSConverter.getInstance()
				.convert((CompactState) refines);
		return isRefinement(refinesMTS, refines.name, refinedMTS, refined.name,
				refinement, ltsOutput);
	}

	/**
	 * 
	 * Checks if <code>refines</code> model is a refinement of
	 * <code>refined</code> using <code>refinement</code> parameter as the refinement
	 * notion
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
	public static <A> boolean isRefinement(MTS<?, A> refines,
			String refinesName, MTS<?, A> refined, String refinedName,
			Refinement refinement, LTSOutput ltsOutput) {

		String refinesOutput = "model [" + refinesName + "] ";
		String refinedOuput = "model [" + refinedName + "] ";
		ltsOutput.outln("Does " + refinesOutput + "refine "
				+ refinedOuput + "? Verifying...");
		long initialTime = System.currentTimeMillis();
		boolean isRefinement = refinement.isARefinement(refined, refines);
		String refinesString = (isRefinement) ? "Yes" : "No";
		//ltsOutput.outln("Verified that " + refinesOutput + refinesString
		ltsOutput.outln(refinesString + ". (" + (System.currentTimeMillis() - initialTime) + "ms.)");
		return isRefinement;
	}
	
	public static boolean areConsistent(CompactState csA,
			CompactState csB, SemanticType semantic, LTSOutput ltsOutput) {

		
		MTS<Long, String> mtsA = AutomataToMTSConverter.getInstance()
				.convert((CompactState) csA);
		MTS<Long, String> mtsB = AutomataToMTSConverter.getInstance()
				.convert((CompactState) csB);
		return areConsistent(mtsA, csA.name, mtsB, csB.name,
				semantic, ltsOutput);
	}

	public static <A> boolean areConsistent(MTS<?, A> mtsA,
			String mtsAName, MTS<?, A> mtsB, String mtsBName,
			SemanticType semantic, LTSOutput ltsOutput) {

		long initialTime = System.currentTimeMillis();
		ltsOutput.outln("Are " + mtsAName + " and "
				+ mtsBName + " " + semantic.toString() + " consistent? Verifying...");
		
		Consistency consistency = semantic.getConsistency(Collections.singleton(MTSConstants.TAU));
		boolean areConsistent = consistency.areConsistent(mtsA, mtsB);
		
		ltsOutput.outln( ((areConsistent) ? "Yes" : "No") + ". (" + (System.currentTimeMillis() - initialTime) + "ms.)");
		
		
		if (!areConsistent && semantic == SemanticType.WEAK_ALPHABET) {
			if (!CollectionUtils.isSubCollection(mtsB.getActions(), mtsA.getActions()) && 
				!CollectionUtils.isSubCollection(mtsA.getActions(), mtsB.getActions())	) {
				//Weak Alphabet
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
	 * otherwise LTS is applied. If <code>compositeState</code> it's an MTS
	 * then before applying minimisation, composition is applied.
	 * 
	 * The result of minimisation is setted to <code>composition</code> field
	 * of <code>compositeState</code>/
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	public static void minimise(CompositeState compositeState, LTSOutput ltsOutput) {

		if (MTSUtils.isMTSRepresentation(compositeState)) {
			Validate.isTrue(compositeState.getComposition()!=null, "MTS ON-THE-FLY minimisation it is not implemented yet.");

			CompactState compactState = (CompactState) compositeState
					.getComposition();
			
			// compactState may be null, for instance after trying
			// to merge two inconsistent MTSs.
			if(compactState != null) {
				MTS<Long, String> mts = mtsMinimise(compactState, ltsOutput);
				compositeState.setComposition(MTSToAutomataConverter.getInstance()
						.convert(mts, compositeState.getComposition().name));
			}
			
		} else {
			compositeState.minimise(ltsOutput);
		}
	}

	private static MTS<Long, String> mtsMinimise(CompactState compactState,
			LTSOutput ltsOutput) {
		long initialTime = System.currentTimeMillis();
		ltsOutput.outln("Converting CompactState " + compactState.name + " to MTS...");
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(
				compactState);
		//ltsOutput.outln("MTS converted in "
		//		+ (System.currentTimeMillis() - initialTime) + "ms.");

		//initialTime = System.currentTimeMillis();
		ltsOutput.outln("Minimising with respect to refinement equivalence...");
		MTSMinimiser<String> minimiser = new MTSMinimiser<String>();

		// get the minimised MTS
		MTS<Long, String> minimisedMTS = minimiser.minimise(mts);
		ltsOutput.outln(compactState.name + " minimised in " + (System.currentTimeMillis() - initialTime) + "ms.");
		
		
		// minimisation sanity check
		ltsOutput.outln("Internal sanity check: Validating minimised and original are equivalent by simulation...");
		WeakSemantics weakSemantics = new WeakSemantics(Collections.singleton(MTSConstants.TAU));
		isRefinement(
				mts,
				" original " + compactState.name,
				minimisedMTS,
				" minimised " + compactState.name, weakSemantics, 
				ltsOutput);
		isRefinement(
				minimisedMTS,
				" minimised " + compactState.name,
				mts,
				" original " + compactState.name,
				weakSemantics, 
				ltsOutput);
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
	public static CompactState minimise(CompactState compactState, LTSOutput ltsOutput) {
		if (MTSUtils.isMTSRepresentation(compactState)) {
			MTS<Long, String> mts = mtsMinimise(compactState, ltsOutput);
			return MTSToAutomataConverter.getInstance().convert(mts, compactState.name);

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
	private static Vector<CompactState> getOptimistModels(Vector<CompactState> originalMachines, LTSOutput output) {
		Vector<CompactState> retValue = new Vector<CompactState>();
		for (Iterator<CompactState> ir = originalMachines.iterator(); ir.hasNext();) {
			CompactState compactState = ir.next();
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
	private static Vector<CompactState> getPessimisticModels(Vector<CompactState> originalMachines) {
		Vector<CompactState> retValue = new Vector<CompactState>();
		for (Iterator<CompactState> ir = originalMachines.iterator(); ir.hasNext();) {
			CompactState compactState = ir.next();
			retValue.add(getPessimistModel(compactState));
		}
		return retValue;
	}

	@SuppressWarnings("unchecked")
	private static void checkSafety(CompositeState compositeState, LTSOutput ltsOutput, boolean checkDeadlocks) {
		long initialCurrentTimeMillis = System.currentTimeMillis();
		printLine(" ", ltsOutput);
		printLine(" ", ltsOutput);
		printLine("Starting safety check on " + compositeState.name, ltsOutput);

		CompactState compactState = compositeState.getComposition();
		Vector<CompactState> machines = compositeState.getMachines();
		String reference = "[Missing Reference]";

		printLine(" ", ltsOutput);
		printLine("Phase I: Does " + compositeState.name
				+ "+ have errors?", ltsOutput);

		CompactState optimisticModel = getOptimisticModel(compactState, ltsOutput);
		Vector<CompactState> toCheck = new Vector<CompactState>();
		toCheck.add(optimisticModel);
		compositeState.setMachines(toCheck);

		compositeState.analyse(checkDeadlocks, ltsOutput);

		if (compositeState.getErrorTrace() == null
				|| compositeState.getErrorTrace().isEmpty()) {
			// M+ |= FI
			printLine(compositeState.name + "+ does not have errors. Which means that...", ltsOutput);
			printLine("*******************************************************************************************",
					ltsOutput);
			printLine("NO ERRORS FOUND: All implementations of "
					+ compositeState.name + " do not have errors." + reference, ltsOutput);
			printLine(
					"********************************************************************************************",
					ltsOutput);

		} else {
			printLine(compositeState.name + "+ does have errors. Which means that...", ltsOutput);
			printLine("This means that some implementations of " + compositeState.name + " have errors.", ltsOutput);
			printLine("", ltsOutput);

			CompactState pessimisticModel = getPessimistModel(compactState);
			compositeState.setErrorTrace(new ArrayList<String>());

			if (!MTSUtils.isEmptyMTS(pessimisticModel)) {
				printLine("Phase II: Does " + compositeState.name
						+ "- have errors?", ltsOutput);
				
				toCheck = new Vector<CompactState>();
				toCheck.add(pessimisticModel);
				compositeState.setMachines(toCheck);
				compositeState.composition = null;

				compositeState.analyse(checkDeadlocks, ltsOutput);

				if (compositeState.getErrorTrace() == null
						|| compositeState.getErrorTrace().isEmpty()) {
					// M- |= FI
					printLine(compositeState.name
						+ "- does not have errors. Which means that...", ltsOutput);
					printLine(
							"*****************************************************************",
							ltsOutput);
					printLine("Model " + compositeState.name
							+ " has some implementations with errors."+reference, ltsOutput);
					printLine(
							"*****************************************************************",
							ltsOutput);
				} else {
					// M- !|= FI
					printLine(compositeState.name
							+ "- does have errors. Which means that...", ltsOutput);
					printLine(
							"*****************************************************************",
							ltsOutput);
					printLine("All implmentations of  "
							+ compositeState.name + " have errors."+ reference, ltsOutput);
					printLine(
							"*****************************************************************",
							ltsOutput);
				}
			} else {

				// complementar la propiedad
				// armar el buchi de la propiedad.
				printLine("Phase II: " + compositeState.name + "- turned out to be empty. Does the complement of " + compositeState.name
						+ "+ have errors?", ltsOutput);
	
				CompactState originalProperty = null;
				for (Iterator<CompactState> it = machines.iterator(); it.hasNext();) {
					CompactState aModel = it.next();
					if (MTSUtils.isPropertyModel(aModel)) {
						originalProperty = aModel;
						break;
					}
				}
				if (originalProperty == null) {
					Diagnostics.fatal(
							"There must be a property to check.");
				}
				compositeState.composition = compactState;
				compositeState.machines = machines;

				CompactState property = TransitionSystemDispatcher
						.buildBuchiFromProperty(compositeState, ltsOutput);

				// chequear M+ junto con la propiedad complementada y dar
				// resultado.
				compositeState.composition = getOptimisticModel(compactState, ltsOutput);
				Vector<CompactState> propVector = new Vector<CompactState>();
				propVector.add(property);
				CompositeState propertyComp = new CompositeState(propVector);
				propertyComp.compose(ltsOutput);
				checkFLTL(compositeState, propertyComp, null, false, ltsOutput);
			//	System.out.println("si llega aca es de milagro!");
				
				if (compositeState.getErrorTrace() == null
						|| compositeState.getErrorTrace().isEmpty()) {
					// M+ !|= NOT FI
					
					printLine(compositeState.name
							+ "+ does not satisfy the complement of the property. This means that... ", ltsOutput);
					printLine(
							"*****************************************************************",
							ltsOutput);
					printLine("All implmentations of  "
							+ compositeState.name + " have errors." + reference, ltsOutput);
					printLine(
							"*****************************************************************",
							ltsOutput);

				} else {
					// M+ |= NOT FI
					printLine(compositeState.name
							+ "+ satisfies the complement of the property. This means that... ", ltsOutput);
					printLine(
							"*****************************************************************",
							ltsOutput);
					printLine("Model " + compositeState.name
							+ " has implementations with errors."+ reference, ltsOutput);
					printLine(
							"*****************************************************************",
							ltsOutput);
				}
			}
		}

		printLine(" ", ltsOutput);
		printLine("*******************************************", ltsOutput);
		printLine("Total safety analysis time: "
				+ (System.currentTimeMillis() - initialCurrentTimeMillis),
				ltsOutput);
		printLine("*******************************************", ltsOutput);

		// leave the original compositeState intact
		compositeState.setComposition(compactState);
		compositeState.setMachines(machines);
	}

	private static CompactState buildBuchiFromProperty(CompositeState compositeState, LTSOutput ltsOutput) {

		// mover a mtsUtils
		CompactState compactState = getProperty(compositeState);
		MTS<Long, String> property = AutomataToMTSConverter.getInstance().convert(compactState);

		Long trapState = Collections.max(property.getStates()) + 1;

		MTSPropertyToBuchiConverter.convert(property, trapState, "@"
				+ compactState.name);
		return MTSToAutomataConverter.getInstance().convert(property,
				compactState.name);
	}

	private static CompactState getProperty(CompositeState compositeState) {
		for (Iterator<CompactState> it = compositeState.machines.iterator(); it.hasNext();) {
			CompactState compactState = it.next();
			if (MTSUtils.isPropertyModel(compactState)) {
				return compactState;
			}
		}
		Diagnostics.fatal(
				"There must be exactly one property to check");
		return null;
	}

	/**
	 * Returns true if the <code>compositeState</code> parameter has any 
	 * deadlock free implementation. 
	 * 
	 * @param compositeState
	 * @param ltsOutput
	 */
	public static boolean hasCompositionDeadlockFreeImplementations(CompositeState compositeState, LTSOutput ltsOutput) {
		applyComposition(compositeState, ltsOutput);
		if (MTSUtils.isMTSRepresentation(compositeState)) {
			MTS<Long, String> mts = AutomataToMTSConverter.getInstance()
					.convert((CompactState) compositeState.getComposition());
			
			String reference = "[Mising Reference]";
			int deadlockStatus = MTSAFacade.getDeadlockStatus(mts);
			if (deadlockStatus == 1) {
				String output = "All implementations of " + compositeState.name
						+ " have a deadlock state."+ reference;
				ltsOutput.outln(output);

				return false;
			} else if (deadlockStatus == 2) {
				String output = "All implementations of " + compositeState.name
						+ " are deadlock free."+ reference;
				ltsOutput.outln(output);
			} else {
				String output = "Some implementations of "
						+ compositeState.name
						+ " are deadlock free while others have a deadlock state."+ reference;
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
	 * Checks if the model <code>compositeState</code> satisfies the property <code>ltlProperty</code>.
	 * If <code>compositeState</code> is an LTS, then the traditional LTS model checking algorithm is applied. 
	 * Otherwise MTS model checking algorithm is applied.  
	 * 
	 * 
	 * @param compositeState
	 * @param ltlProperty
	 * @param not_ltl_property
	 * @param fairCheck
	 * @param ltsOutput
	 */
	@SuppressWarnings("unchecked")
	public static void checkFLTL(CompositeState compositeState,
			CompositeState ltlProperty, CompositeState not_ltl_property, boolean fairCheck, LTSOutput ltsOutput) {

		if (MTSUtils.isMTSRepresentation(compositeState)) {

			if (fairCheck) {
				throw new UnsupportedOperationException(
						"FLTL model checking of MTS with Fair Choice is not yet defined.");
			}
			 //ISSUE We can't do FLTL check on-the-fly.
			applyComposition(compositeState, ltsOutput);
		
			if (saved != null) {
				compositeState.getMachines().remove(saved);
				saved = null;
			}

			compositeState.setErrorTrace(new ArrayList<String>());

			if (hasCompositionDeadlockFreeImplementations(compositeState, ltsOutput)) {

				long initialCurrentTimeMillis = System.currentTimeMillis();

				String reference = "[TOSEM]";
				printLine(" ", ltsOutput);
				printLine(" ", ltsOutput);
				printLine("Starting model check of "
						+ compositeState.name + " against property "
						+ ltlProperty.name, ltsOutput);

				Vector<CompactState> machines = compositeState.getMachines();
				CompactState composition = compositeState.getComposition();

				CompactState optimistModel = getOptimisticModel(composition, ltsOutput);
				Vector<CompactState> toCheck = new Vector<CompactState>();
				toCheck.add(optimistModel);
				compositeState.setMachines(toCheck);

				printLine(" ", ltsOutput);
				printLine("Phase I: Does " + compositeState.name
						+ "+ satisfy " + ltlProperty.name + "?", ltsOutput);

				// phase I checking
				compositeState.checkLTL(ltsOutput, ltlProperty);

				if (compositeState.getErrorTrace() == null
						|| compositeState.getErrorTrace().isEmpty()) {
					// M+ |= FI
					printLine(" ", ltsOutput);
					printLine("Yes. " + compositeState.name + "+ satisfies. "
							+ ltlProperty.name, ltsOutput);
					printLine("This means that...", ltsOutput);
					printLine(
							"*****************************************************************",
							ltsOutput);
					printLine("All deadlock-free implementations of "+ compositeState.name + " satisfy " + ltlProperty.name + " " + reference, ltsOutput);
					printLine(
							"*****************************************************************",
							ltsOutput);

				} else {
					printLine(" ", ltsOutput);
					printLine("No. "+compositeState.name + "+ does not satisfy "
							+ ltlProperty.name, ltsOutput);
					printLine("This means that some deadlock-free implementations of " +compositeState.name + " do not satisfy " + ltlProperty.name, ltsOutput);

					//M-
					CompactState pessimisticModel = getPessimistModel(composition);
					compositeState.setErrorTrace(ListUtils.EMPTY_LIST);

					if (!MTSUtils.isEmptyMTS(pessimisticModel)) {
						//M- is not the empty MTS
						
						toCheck = new Vector<CompactState>();
						toCheck.add(pessimisticModel);
						compositeState.setMachines(toCheck);

						printLine(" ", ltsOutput);
						printLine("Phase II: Does "+ compositeState.name + "- satisfy "
								+ ltlProperty.name + "?", ltsOutput);

						// phase II checking
						compositeState.checkLTL(ltsOutput, ltlProperty);

						if (compositeState.getErrorTrace() == null
								|| compositeState.getErrorTrace().isEmpty()) {
							// M- |= FI
							printLine("Yes. " + compositeState.name + "- does satisfy "
									+ ltlProperty.name + ", which means that...", ltsOutput);
							printLine(
									"*****************************************************************",
									ltsOutput);
							printLine("There exists a deadlock-free implementation of " + compositeState.name + 
									" that satisfies "
									+ ltlProperty.name +" but there may be deadlock-free implementations " +
											"that violate the property. This is the case in which thorough semantics " +
											"is approximated by inductive semantics. "
									+ reference, ltsOutput);
							printLine(
									"*****************************************************************",
									ltsOutput);
						} else {
							// M- !|= FI
							printLine("No. " + compositeState.name + "- does not satisfy "
									+ ltlProperty.name + ", which means that...", ltsOutput);
							printLine(
									"*****************************************************************",
									ltsOutput);
							printLine("No deadlock-free implementation of " + compositeState.name + " satisfies "
									+ ltlProperty.name + " "+ reference, ltsOutput);
							printLine(
									"*****************************************************************",
									ltsOutput);
						}

					} else {
						printLine(" ", ltsOutput);
						printLine("Phase II: As " + compositeState.name
										+ "- is empty it cannot be checked against " 
										+ ltlProperty.name + ". " + reference, ltsOutput);
						printLine("Will check " + compositeState.name + 
										"+ against the negation of " + ltlProperty.name + " " 
										+ reference, ltsOutput);
						printLine("Does " + compositeState.name + "+ satisfy "
								+ not_ltl_property.name + "?", ltsOutput);

						// phase II checking
						compositeState.checkLTL(ltsOutput, not_ltl_property);

						if (compositeState.getErrorTrace() == null
								|| compositeState.getErrorTrace().isEmpty()) {
							// M+ |= !FI
							printLine("Yes. " + compositeState.name + "+ does satisfy "
									+ not_ltl_property.name + ", which means that...", ltsOutput);
				
							printLine(
									"*****************************************************************",
									ltsOutput);
							printLine("No deadlock-free implementation of " + compositeState.name + " satisfies "
									+ ltlProperty.name + " " + reference, ltsOutput);
							printLine(
									"*****************************************************************",
									ltsOutput);
						} else {
							// M+ !|= !FI
							printLine("No. " + compositeState.name + "+ does not satisfy "
									+ not_ltl_property.name + ", which means that...", ltsOutput);
				
							printLine(
									"*****************************************************************",
									ltsOutput);
							printLine("There might exist some deadlock-free implementations of " + compositeState.name + 
									" that satisfy " + ltlProperty.name +", but there are deadlock-free implementations " +
											"that don't satisfty " + ltlProperty.name + ". " +
											"This is the case in which thorough semantics " +
											"is approximated by inductive semantics. "
									+ reference, ltsOutput);
							printLine(
									"*****************************************************************",
									ltsOutput);
						}
					}
				}

				printLine(
						compositeState.name
								+ " model checked in "
								+ (System.currentTimeMillis() - initialCurrentTimeMillis)
								+ "ms", ltsOutput);

				// restore the orginal compositeState
				machines.add(saved = ltlProperty.getComposition());
				compositeState.setMachines(machines);
				compositeState.setComposition(composition);
			} else {
				ltsOutput
						.outln("The model must have deadlock free implementations to be checked.");
			}
		} else {
			if (compositeState.makeController || compositeState.checkCompatible || 
					compositeState.makeSyncController) {
				checkControllerFLTL(compositeState, ltlProperty, ltsOutput);
			} else {
				compositeState.checkLTL(ltsOutput, ltlProperty);
			}
		}
	}
	
	/*
	 * This method applies the specified set of operations to the 
	 * compositeState parameter, including parallel composition, and
	 * then builds a new CompositeState which machines vector is filled 
	 * only with the just generated composition.  
	 */
	private static void checkControllerFLTL(CompositeState compositeState,
			CompositeState ltlProperty, LTSOutput ltsOutput) {
		if (compositeState.getComposition()==null) {
			applyComposition(compositeState, ltsOutput);
		}
		Vector<CompactState> machines = new Vector<CompactState>();
		machines.add(compositeState.getComposition());
		CompositeState cs = new CompositeState(compositeState.name, machines);
		cs.checkLTL(ltsOutput, ltlProperty);
	}

	private static CompactState saved = null;

	/**
	 * Builds the <b>abstract</b> version of the original model.
	 * The result it's builded following the following procedure:
	 * <br> 
	 * Firstly a state (called trap state) with loop transitions on every 
	 * label in the alphabet
	 * is added to the abstract model. 
	 * Then from every state in <code>compactState</code> and for every 
	 * label in <code>compactState</code>s alphabet 
	 * for which there are no outgoing transition from it,  
	 * it is added one transition to trap state.
	 * 
	 * @param compactState
	 * @param output
	 * @return
	 */
	public static CompactState getAbstractModel(CompactState compactState,
			LTSOutput output) {
		long initialTime = System.currentTimeMillis();
		MTSAbstractBuilder abstractBuilder = new MTSAbstractBuilder();
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(
				compactState);
		Set<String> toDelete = new HashSet<String>();
		toDelete.add(MTSConstants.ASTERIX);
		toDelete.add(MTSConstants.AT);
		MTSUtils.removeActionsFromAlphabet(mts, toDelete);

		MTS<Long, String> abstractModel = abstractBuilder.getAbstractModel(mts);
		output.outln("Abstract model generated for "
				+ compactState.name + " in: "
				+ (System.currentTimeMillis() - initialTime) + "ms.");

		return MTSToAutomataConverter.getInstance().convert(abstractModel,
				compactState.name);
	}
	
	
	/**
	 * Sets the result of <code>getAbstractModel</code> method 
	 * applied to composition field of
	 * <code>compositeState</code> to the composition field again.
	 * 
	 * @param compositeState
	 * @param output
	 */
	public static void makeAbstractModel(CompositeState compositeState,
			LTSOutput output) {
		if (compositeState.getComposition() != null) {
			compositeState.setComposition(getAbstractModel(compositeState
					.getComposition(), output));
		}
	}

	/**
	 * Computes the transitive tau-closure for compactState. It propagates 
	 * may transitions. 
	 * 
	 * @param compactState
	 * @param output
	 * @return
	 */
	public static CompactState getTauClosure(CompactState compactState, LTSOutput output) {
		MTS<Long, String> mts = AutomataToMTSConverter.getInstance().convert(compactState);
		output.outln("Applying tau clousure [bisimulation-based]");
		MTSAFacade.applyClosure(mts, Collections.singleton(MTSConstants.TAU));
		return MTSToAutomataConverter.getInstance().convert(mts, compactState.name);
	}

	/**
	 * Sets the result of <code>getClousureModel</code> method applied to composition field of
	 * <code>compositeState</code> to the composition field again.
	 * 
	 * @param compositeState
	 * @param output
	 */
	public static void makeClosureModel(CompositeState compositeState, LTSOutput output) {
		long initialTime = System.currentTimeMillis();
		if (compositeState.getComposition() != null) {
			CompactState tauClosure = TransitionSystemDispatcher.getTauClosure(compositeState.getComposition(), output);
			compositeState.setComposition(tauClosure);
			output.outln("Clousure model generated for " + compositeState.name + " in: "
					+ (System.currentTimeMillis() - initialTime) + "ms.");
		}
	}

	/**
	 * For every state in which are choices transform those transitions to 
	 * maybe transitions.
	 * 
	 * @param compactState
	 * @param output
	 * @return
	 */
	public static CompactState makeMTSConstraintModel(CompactState compactState, LTSOutput output) {
		MTS<Long, String> constrained = AutomataToMTSConverter.getInstance()
				.convert(compactState);

		long initialTime = System.currentTimeMillis();

		Set<String> toDelete = new HashSet<String>();
		toDelete.add(MTSConstants.ASTERIX);
		toDelete.add(MTSConstants.AT);
		MTSUtils.removeActionsFromAlphabet(constrained, toDelete);

		MTSConstraintBuilder constraintBuilder = new MTSConstraintBuilder();
		constraintBuilder.makeConstrainedModel(constrained);

		output.outln("Constrained model generated for "
				+ compactState.name + " in: "
				+ (System.currentTimeMillis() - initialTime) + "ms.");
		return MTSToAutomataConverter.getInstance().convert(constrained,
				compactState.name);
	}

	public static void checkProgress(CompositeState compositeState, LTSOutput output) {

		if (MTSUtils.isMTSRepresentation(compositeState)) {
			Diagnostics.fatal(
					 "MTS Progress check has not been defined yet.");
		} else {
			if (compositeState.makeController || compositeState.checkCompatible || 
					compositeState.makeSyncController) {
				Vector<CompactState> machines = new Vector<CompactState>();
				machines.add(compositeState.getComposition());
				CompositeState cs = new CompositeState(compositeState.name, machines);
				cs.checkProgress(output);
			} else {
				compositeState.checkProgress(output);
			}
		}
	}

	public static void checkCompatible(CompositeState compositeState, LTSOutput output) {
		CompactState composition = compositeState.getComposition();
		if (composition != null) {
			if (MTSUtils.isMTSRepresentation(composition)) {
				Diagnostics.fatal("Compatibility check is not defined yet for MTSs.");
			}
			long initialTime = System.currentTimeMillis();
			MTS<Long, String> env = AutomataToMTSConverter.getInstance().convert(composition);
			env = ControllerUtils.embedFluents(env, compositeState.goal, output);
			
			ControllerSynthesisFacade<Long, String, Integer> instance = new ControllerSynthesisFacade<Long, String, Integer>();
			boolean compatible = instance.checkAssumptionsCompatibility(env, compositeState.goal);
			if (compatible) {
				output.outln("The assumptions are compatible with the given environment.[FSE2010]"); 
			} else {
				output.outln("The assumptions are NOT compatible with the given environment.[FSE2010]");
			}
			output.outln("Analysis time: " + (System.currentTimeMillis() - initialTime) + "ms.");
		}
	}

	public static void makePlant(CompositeState compositeState, LTSOutput output) {
		CompactState composition = compositeState.getComposition();
		if (composition != null) {
			if (compositeState.goal!=null) {
				MTS<Long, String> env = AutomataToMTSConverter.getInstance().convert(composition);
				
				MTS<Long, String> plant = ControllerUtils.embedFluents(env, compositeState.goal, output);
				
				compositeState.setComposition(MTSToAutomataConverter.getInstance().convert(plant, compositeState.getName()));
			} else {
				Diagnostics.fatal("The plant must have a goal.");
			}
		}
	}

	public static void makeControlledDeterminisation(CompositeState compositeState, LTSOutput output) {
		CompactState composition = compositeState.getComposition();
		if (composition != null) {
			GRControllerGoal<String> goal = compositeState.goal;
			MTS<Long, String> env = AutomataToMTSConverter.getInstance().convert(composition);
			IsDeterministicMTSPredicate<Long, String> pred = new IsDeterministicMTSPredicate<Long, String>(TransitionType.POSSIBLE);
			if (!pred.evaluate(env)) {
				if (goal != null) {
					MTS<Pair<Set<Long>, String>, String> controlledDet = 
							ControllerUtils.getControllableDeterminisationFor(env,goal.getControllableActions(), output);
					IsDeterministicMTSPredicate<Pair<Set<Long>, String>, String> pred2 = new IsDeterministicMTSPredicate<Pair<Set<Long>, String>, String>(TransitionType.POSSIBLE);
					if (!pred2.evaluate(controlledDet)) {
						Diagnostics.fatal("Controlled determinisation error.");
					}
					MTS<Long, String> det = new GenericMTSToLongStringMTSConverter<Pair<Set<Long>, String>, String>().transform(controlledDet);
					compositeState.setComposition(MTSToAutomataConverter.getInstance().convert(det,composition.getName()));
				} else {
					Diagnostics.fatal("Controllable actions not defined.");
				}
			}
		}
	}
	
	//TODO this method is copied from synthesiseGRController. Some polymorfism would be nice. 
	public static void synthesiseSyncController(CompositeState compositeState, LTSOutput output) {
		CompactState composition = compositeState.getComposition();
		if (composition != null) {
			if (compositeState.goal!=null) {
				CompactState synthesiseController = synthesiseSyncController(compositeState, compositeState.goal, output);
				if (synthesiseController!=null) {
					compositeState.setComposition(synthesiseController);
				} else if (!composition.name.contains(ControlConstants.NO_CONTROLLER)) {
					composition.name = composition.name + ControlConstants.NO_CONTROLLER;
				}
			} else {
				Diagnostics.fatal("The controller must have a goal.");
			}
		}
	}
	
	public static CompactState synthesiseSyncController(CompositeState compositeState,	GRControllerGoal<String> goal, LTSOutput output) {
		CompactState c =  compositeState.composition;
		long initialTime = System.currentTimeMillis();
		
		//Validation
		if (c.hasTau()) {
			Diagnostics.fatal("Partial Observable Environments are not yet supported.");
		} else if (MTSUtils.isMTSRepresentation(c)) {
			//TODO implement MTS Control for synchronous assumption.
			Diagnostics.fatal("Partially specified environmen models are not yet supported.");
		} else {
			output.outln("Solving the nondeterministic control problem.");
		}
		
		MTS<Long, String> env = AutomataToMTSConverter.getInstance().convert(c);
		
		//TODO preprocess must be pulled into to SynchronousControlProblem construction. 
		//That requires ControllersUtils to be generic and placed in MTSSynthesis project
		//Preprocess required for building the BGR game
		//Adding synchronisation
		String envYieldsAction = "envYields";
		String contYieldsAction = "contYields";
		MTS<Long, String> synchronous = ControllerUtils.getSynchronous(env, goal.getControllableActions(), 0L, 1L, envYieldsAction, contYieldsAction);
		env = ControllerUtils.embedFluents(synchronous, goal, output);
		

		//TODO refactor needed. there is a problem with the generics we're using. It shouldn't be so hard to work with LTS/MTS 
		//interchangeably and also there is an issue with the generic to MTS to be fixed. 
		
		//Synthesis Process
		SynchronousControlProblem<Long, String> cp = new SynchronousControlProblem<Long, String>(env, goal, envYieldsAction, contYieldsAction);
		LTS<StrategyState<Long, Integer>, String> synthesised = new ControllerSynthesisFacade<Long, String, Integer>().synthesiseController(cp);
		
		//process the output
		if (synthesised==null) {
			output.outln("There is no controller for model " + compositeState.name + " for the given setting.");
			output.outln("Analysis time: " + (System.currentTimeMillis() - initialTime) + "ms.");
			return null;
		} else {

			MTSAdapter<StrategyState<Long, Integer>, String> mtsAdapter = 
					new MTSAdapter<StrategyState<Long, Integer>, String>(synthesised);
			MTS<Long, String> plainController = 
					new GenericMTSToLongStringMTSConverter<StrategyState<Long, Integer>, String>()
						.transform(mtsAdapter);
			
			output.outln("Analysis time: " + (System.currentTimeMillis() - initialTime) + "ms.");
			output.outln("Controller [" + plainController.getStates().size() + "] generated successfully.");
			
			CompactState compactState = MTSToAutomataConverter.getInstance().convert(plainController, compositeState.getName());
			return compactState;
		} 
	}
	
	public static void synthesiseGR(CompositeState compositeState, LTSOutput output) {
		CompactState composition = compositeState.getComposition();
		if (composition != null) {
			if (compositeState.goal!=null) {
				CompactState synthesiseController = synthesiseGR(compositeState, compositeState.goal, output);
				if (synthesiseController!=null) {
					synthesiseController = applyLatencyHeuristic(compositeState, synthesiseController);
					compositeState.setComposition(synthesiseController);
				} else if (!composition.name.contains(ControlConstants.NO_CONTROLLER)) {
					composition.name = composition.name + ControlConstants.NO_CONTROLLER;
				}
			} else {
				Diagnostics.fatal("The controller must have a goal.");
			}
		}
	}

	private static CompactState applyLatencyHeuristic(
			CompositeState compositeState, CompactState synthesiseController) {
		GRControllerGoal<String> goal = compositeState.goal;
		boolean PRUNNING_HEURISTIC_DEFINED = goal.isNonTransient() || !goal.getConcurrencyFluents().isEmpty();
		if(PRUNNING_HEURISTIC_DEFINED || goal.isReachability()){
			MTS<Long, String> temp = AutomataToMTSConverter.getInstance().convert(synthesiseController);
			HeuristicControllerSynthesiser<Long,String> synthesiser = new HeuristicControllerSynthesiser<Long, String>();
			MTS<Long,String> plainController; 
			if(PRUNNING_HEURISTIC_DEFINED){
				CompactState e =  compositeState.env;
				MTS<Long, String> env = AutomataToMTSConverter.getInstance().convert(e);
				System.out.println("Applying Heuristics!");
				plainController = synthesiser.applyHeuristics(temp, env, goal);
			}else {
				System.out.println("Applying Reachability Prunning!");
				plainController = synthesiser.applyReachabilityPrunning(temp, goal);
			}
			synthesiseController = MTSToAutomataConverter.getInstance().convert(plainController, compositeState.getName());
		}
		return synthesiseController;
	}

	//ToDani: este metodo deberia ser synthesise y resolver que control problem hay que aplicar.
	public static CompactState synthesiseGR(CompositeState compositeState,	GRControllerGoal<String> goal, LTSOutput output) {
		
		//ToDani: MTS env = AutomataToMTS.getInstance().convert(compositeState.composition);
		//ToDani: LTS controller = ControlProblemFactory.buildControlProblem(env, goal).solve()
		//ToDani: return MTSToAutomata.convert(controller);
		
		
		CompactState c =  compositeState.composition;
		
		long initialTime = System.currentTimeMillis();

		boolean isMTS = MTSUtils.isMTSRepresentation(c);

		if (!isMTS && (c.isNonDeterministic() || c.hasTau())) {
			MTS<Long, String> env = AutomataToMTSConverter.getInstance().convert(c);

			if (c.hasTau()) {
				output.outln("Solving the partial observability control problem.");
				TauRemoval.muAddition(env, "-2", MTSConstants.TAU);
				TauRemoval.apply(env, MTSConstants.TAU);
				c = compositeState.composition =
					MTSToAutomataConverter.getInstance().convert(env, compositeState.name, isMTS);
			} else {
				output.outln("Solving the nondeterministic control problem.");
			}

			MTS<StrategyState<Set<Long>, Integer>, String> synthesised = null;
			if (goal.isNonBlocking()) {
				//Synthesising nonblocking controllers
				output.outln("Checking for Nonblocking LTS controllers.");
				env = ControllerUtils.embedFluents(env, goal, output);
				NondetControlProblem<Long, String> cp = new NondetControlProblem<Long, String>(env, goal);
				LTS<StrategyState<Set<Long>, Integer>, String> synthesisResult =
					new ControllerSynthesisFacade<StrategyState<Set<Long>, Integer>, String, Integer>().synthesiseController(cp);
				if (synthesisResult != null) {
					synthesised = new MTSAdapter<StrategyState<Set<Long>,Integer>, String>(synthesisResult);
					if (synthesised != null && c.hasTau())
						TauRemoval.muRemoval(synthesised, "-2");
				}
			} else {
				//synthesising Legal Controllers
				output.outln("Checking for Legal LTS controllers.");
				output.outln("Building controlled determinisation.");
				//check: do i need to embed fluents?
				long time = System.currentTimeMillis();
				makeControlledDeterminisation(compositeState, output);
				output.outln("Determinisation time: " + (System.currentTimeMillis() - time) + "ms.");
				CompactState synthesiseController = synthesiseGR(compositeState, goal, output);
				if (synthesiseController != null) {
					MTS<Long, String> temp = AutomataToMTSConverter.getInstance().convert(synthesiseController);
					TraceInclusionClosure.getInstance().applyMTSClosure(temp, Collections.singleton("-1"));
					if (c.hasTau())
						TauRemoval.muRemoval(temp, "-2");
//				compositeState.setComposition(MTSToAutomataConverter.getInstance().convert(temp, compositeState.name));
					return MTSToAutomataConverter.getInstance().convert(temp, compositeState.name, isMTS);
				} else {
					return null;
				}
			}
			
			if (synthesised==null) {
				output.outln("There is no controller for model " + compositeState.name + " for the given setting.");
				output.outln("Analysis time: " + (System.currentTimeMillis() - initialTime) + "ms.");
				return null;
			} else {
				MTS<Long, String> plainController = new GenericMTSToLongStringMTSConverter<StrategyState<Set<Long>, Integer>, String>().transform(synthesised);
				output.outln("Analysis time: " + (System.currentTimeMillis() - initialTime) + "ms.");
				output.outln("Controller [" + plainController.getStates().size() + "] generated successfully.");
				CompactState compactState = MTSToAutomataConverter.getInstance().convert(plainController, compositeState.getName(), isMTS);
				compactState = minimise(compactState, output);
				return compactState;
			} 

		} else if (!isMTS) {
			//ToMarian: add this (most of it?) code to a new class LTSControlProblem
			output.outln("Solving the LTS control problem.");
			
			MTS<Long, String> env = AutomataToMTSConverter.getInstance().convert(c);
			MTS<Long, String> plant = ControllerUtils.embedFluents(env, goal, output);
			
			ControllerSynthesisFacade<Long, String, Integer> facade = new ControllerSynthesisFacade<Long, String, Integer>();
			MTS<StrategyState<Long, Integer>, String> synthesised = facade.synthesiseController(plant, goal);
			
			if (synthesised==null) {
				output.outln("There is no controller for model " + compositeState.name + " for the given setting.");
				output.outln("Analysis time: " + (System.currentTimeMillis() - initialTime) + "ms.");
				return null;
			} else {
			  GenericMTSToLongStringMTSConverter<StrategyState<Long, Integer>, String> transformer = new GenericMTSToLongStringMTSConverter<StrategyState<Long, Integer>, String>();
				MTS<Long, String> plainController = transformer.transform(synthesised);
				
				recordControllerStateLabels(transformer.getStateMapping(), facade.getGame(), goal); //needed for MDP translation
				
				output.outln("Analysis time: " + (System.currentTimeMillis() - initialTime) + "ms.");
				output.outln("Controller [" + plainController.getStates().size() + "] generated successfully.");
				return MTSToAutomataConverter.getInstance().convert(plainController, compositeState.getName(), isMTS);
			}
		} else {
//			Checking MTS control problem
			if (c.isNonDeterministic()) { 
				Diagnostics.fatal("The domain model [" + compositeState.getName() + "] must be deterministic [FM-2012]."); 
			}
			
			output.outln("Solving the MTS control problem.");
			
			MTS<Long, String> env = AutomataToMTSConverter.getInstance().convert(c);

			MTS<Pair<Long, Set<String>>, String> envStar = ControllerUtils.generateStarredEnvModel(env);
			
			MTS<Long, String> plant = new GenericMTSToLongStringMTSConverter<Pair<Long, Set<String>>, String>().transform(envStar);
			
			plant = ControllerUtils.embedFluents(plant, goal, output);
			
//			Is it true that for all implementations of mts there exists an LTS controller?
			output.outln("Checking whether all implementations are controllable.");
			MTS<StrategyState<Long, Integer>, String> synthesised = new ControllerSynthesisFacade<Long, String, Integer>().synthesiseController(plant, goal);
			
			if (synthesised!=null) {
				MTS<Long, String> plainController = new GenericMTSToLongStringMTSConverter<StrategyState<Long, Integer>, String>().transform(synthesised);
//				Set<String> disjunction = Sets.difference(synthesised.getActions(), env.getActions());
//				TraceInclusionClosure.getInstance().applyMTSClosure(plainController, disjunction); 
				output.outln("All implementations of " + compositeState.getName() + " can be controlled");
				return MTSToAutomataConverter.getInstance().convert(plainController, compositeState.getName());
			}
			
//			Is it true that there is no implementation of mts for which there exists an LTS controller?
			output.outln("Checking whether no implementation is controllable.");			
			Set<String> newActions = new HashSet<String>();
			for (String action : envStar.getActions()) {
				if (!env.getActions().contains(action)) {
					newActions.add(action);
				}
			}
			
			goal.addAllControllableActions(newActions);
			synthesised = new ControllerSynthesisFacade<Long, String, Integer>().synthesiseController(plant, goal);
			if (synthesised==null) {
				output.outln("No implementation of " + compositeState.getName() + " can be controlled");
				return null;
			} else {
				output.outln("Some implementations of " + compositeState.getName() + " can be controlled and some cannot.");
				MTS<Long, String> plainController = new GenericMTSToLongStringMTSConverter<StrategyState<Long, Integer>, String>().transform(synthesised);
				return MTSToAutomataConverter.getInstance().convert(plainController, compositeState.getName());
			}
		}
	}	
	private static Map<Long,List<String>> lastControllerStateLabels;
	
	private static void recordControllerStateLabels(Map<StrategyState<Long,Integer>,Long> mapping, GRGame<Long> lastGame, GRControllerGoal<String> goal)
	{
    Map<Long,Long> actualMapping = new HashMap<Long,Long>(); //from controller state to original environment state
    for (StrategyState<Long,Integer> ss : mapping.keySet())
    {
      System.out.println("Environment's "+ss.getState()+" is controller's "+mapping.get(ss));
      actualMapping.put(mapping.get(ss), ss.getState());
    }
    Map<Long,List<String>> stateLabels = new HashMap<Long,List<String>>();
    Assumptions<Long> assumptions = lastGame.getGoal().getAssumptions();
    for (int j = 1; j <= assumptions.getSize(); j++) //1-based for some reason
    {
      for (Long s : actualMapping.keySet()) //controller states
      {
        if (assumptions.getAssume(j).getStateSet().contains(actualMapping.get(s))) //this assumption applies to whichever environ state s maps to
        {
          List<String> labels = stateLabels.get(s);
          if (labels == null)
          {
            labels = new Vector<String>();
            stateLabels.put(s, labels);
          }
          String formula = (goal.getAssumptions().size() >= j ? goal.getAssumptions().get(j-1).toString() : "assu"+j);
          labels.add(formula);
        }
      }
    }
    Guarantees<Long> guarantees = lastGame.getGoal().getGuarantees();
    for (int j = 1; j <= guarantees.size(); j++) //1-based for some reason
    {
      for (Long s : actualMapping.keySet()) //controller states
      {
        if (guarantees.getGuarantee(j).getStateSet().contains(actualMapping.get(s))) //this guarantee applies to whichever environ state s maps to
        {
          List<String> labels = stateLabels.get(s);
          if (labels == null)
          {
            labels = new Vector<String>();
            stateLabels.put(s, labels);
          }
          String formula = (goal.getGuarantees().size() >= j ? goal.getGuarantees().get(j-1).toString() : "guar"+j);
          labels.add(formula);
        }
      }
    }
    lastControllerStateLabels = stateLabels;
	}
	
	public static Map<Long,List<String>> getLastControllerStateLabels()
	{
	  return lastControllerStateLabels;
	}
	
	/**
	 * Generates the Star model for c
	 * @param c
	 * @param output
	 * @return
	 */
	public static CompactState getStarEnv(CompactState c, LTSOutput output) {
		long initialTime = System.currentTimeMillis();
		MTS<Long, String> env = AutomataToMTSConverter.getInstance().convert(c);
		MTS<Pair<Long, Set<String>>, String> envStar = ControllerUtils.generateStarredEnvModel(env);
		MTS<Long, String> plainController = new GenericMTSToLongStringMTSConverter<Pair<Long, Set<String>>, String>().transform(envStar);
		output.outln("Starred model for " + c.getName() + " generated in " + (System.currentTimeMillis()-initialTime) + "ms.");
		return MTSToAutomataConverter.getInstance().convert(plainController, c.getName());
	}
	
	/**
	 * Generates the Star model for c
	 * @param c
	 * @param output
	 * @return
	 */
	public static void makeStarEnv(CompositeState c, LTSOutput output) {
		if (c.getComposition()==null) {
			applyComposition(c, output);
		}
		c.setComposition(getStarEnv(c.getComposition(), output));
	}

	
	/**
	 * Generate the appropriate Animator depending on the type of the Composite State
	 * 
	 * @param compositeState The machine to animate
	 * @param output
	 * @param eventManager Tell this object whenever the current state within the machine changes
	 * @return Animator depending on the type of the Composite State
	 */
	
	public static lts.Animator generateAnimator(CompositeState compositeState, 
			LTSOutput output, lts.EventManager eventManager) {
//		return new MTSAnimator(compositeState, eventManager);

		if(MTSUtils.isMTSRepresentation(compositeState)) {
			return new MTSAnimator(compositeState, eventManager);
		} else {
			Analyser analyser = new Analyser(compositeState, output, eventManager);
			//DIPI: here we could remove the maybe transitions from the alphabet
//			return new AnimatorDecorator(analyser);
			return analyser;
		}
	}

	/**
	 * Returns the number of machines n in the CompositeState, adding one
	 * for MTS because of the capability to draw the composite
	 * 
	 * @param compositeState The machine to draw
	 * @return Number of machines to draw
	 */

	public static int numberMachinesToDraw(CompositeState compositeState) {
		boolean isComposite = (compositeState != null && compositeState.composition != null);
		int numMachines = compositeState.machines.size();;
		if(MTSUtils.isMTSRepresentation(compositeState)) {
			if(isComposite)
				return numMachines + 1;
			else
				return numMachines;
		}
		else
			return numMachines;
	}

	/**
	 * Tries to apply the distribution. calculatedDistributedComponents is a parameter
	 * that should be an empty set. 
	 * Returns true if the distribution is possible and the resulting
	 * distributed components are added to calculatedDistributedComponents.
	 * @param systemModelCS
	 * @param distributionDefinition
	 * @param output
	 * @return
	 */
	public static boolean tryDistribution(CompactState systemModelCS, DistributionDefinition distributionDefinition, LTSOutput output, Collection<CompactState> calculatedDistributedComponents) {
		Validate.notNull(calculatedDistributedComponents, "A set to store the calculated components must be provided");
		Validate.notNull(systemModelCS, "System model is null");
		
		output.outln("Trying to distribute model " + systemModelCS.getName());
		
		MTS<Long, String> systemModelMTS = AutomataToMTSConverter.getInstance().convert(systemModelCS);
		DistributionFacade<Long, String> distributionFacade = new DistributionFacade<Long, String>();
		
		Map<String, Set<String>> componentsNameAndAlphabet;
	
		try {
			componentsNameAndAlphabet = distributionDefinition.calculateAndGetComponentsNameAndAlphabet(output);
			output.outln("Using distribution: " + componentsNameAndAlphabet );
			
			Set<Set<String>> allAlphabets = new HashSet<Set<String>>(componentsNameAndAlphabet.values());
			
			// create the alphabet distribution 
			AlphabetDistribution<String> alphabetDistribution = new AlphabetDistribution<String>(allAlphabets);


			long initialTime = System.currentTimeMillis();

			// try to distribute
			DistributionResult<Long, String> distributionResult = distributionFacade.tryDistribute(systemModelMTS, alphabetDistribution, MTSConstants.TAU);

			output.outln("Distribution attempt finished in " + (System.currentTimeMillis()-initialTime) + "ms. Model is distributable?: " + distributionResult.isDistributable());

			if(distributionResult.isDistributable()) {
				Set<Entry<String, Set<String>>> componentsNameAndAlphabetEntrySet = componentsNameAndAlphabet.entrySet();
				
				Set<String> componentAlphabet;
				String componentName;
				MTSToAutomataConverter converter = MTSToAutomataConverter.getInstance();
				GenericMTSToLongStringMTSConverter<Set<Long>, String> toLongStringMTSConverter = new GenericMTSToLongStringMTSConverter<Set<Long>, String>();
				
				for (Entry<String, Set<String>> entry : componentsNameAndAlphabetEntrySet) {
					componentName = entry.getKey();
					componentAlphabet = entry.getValue();
					
					MTS<Set<Long>, String> mTSComponent = distributionResult.getComponent(componentAlphabet);
					MTS<Long, String> mTSComponentLongString = toLongStringMTSConverter.transform(mTSComponent);
	
					
					CompactState compactStateComponent = converter.convert(mTSComponentLongString, componentName);
					
					// add the component to the result
					calculatedDistributedComponents.add(compactStateComponent);
				}
				//TODO result and feedback should be written to the file set in the distribution spec
				return true;
			} else {
				List<DistributionFeedbackItem> feedback = distributionResult.getFeedback();
				for (DistributionFeedbackItem feedbackItem : feedback) {
					output.outln(feedbackItem.getMessage());
				}
				//TODO result and feedback should be written to the file set in the distribution spec
				return false;
			}
		} catch (DistributionTransformationException e) {
			Diagnostics.fatal(e.getMessage());
			return false;
		}
	}


	/**
	 * 
	 * @param compositeState
	 * @param output
	 */
	public static void makeComponentModel(CompositeState compositeState,
			LTSOutput output) {
		
		if (compositeState.getComposition() != null) {
			if (compositeState.getComponentAlphabet() != null) {
			compositeState.setComposition(getComponentModel(compositeState
					.getComposition(), compositeState.getComponentAlphabet(), output));
			} else {
				StringBuffer errorMsg = new StringBuffer("The alphabet for component ");
				errorMsg.append(compositeState.getName()).append(" is missing");
				Diagnostics.fatal(errorMsg.toString());
			}
		}
	}

	/**
	 * TODO: this method should be delted. A method to get the whole decomposition will be provided
	 * @param composition
	 * @param componentAlphabet
	 * @param output
	 * @return
	 */
	private static CompactState getComponentModel(CompactState composition,
			Vector<String> componentAlphabet, LTSOutput output) {
		
		MTS<Long, String> compositionMTS = AutomataToMTSConverter.getInstance().convert(composition);
		
		MTS<Long, String> componentMTS = MTSAFacade.getComponent(compositionMTS, new HashSet<String>(componentAlphabet));		

		return MTSToAutomataConverter.getInstance().convert(componentMTS, composition.getName());
	}

	public static void makeProperty(CompositeState compositeState,
			LTSOutput output) {

		if (compositeState.getComposition() != null) {
			if (!MTSUtils.isMTSRepresentation(compositeState)) {
				MTS<Long, String> mts = AutomataToMTSConverter.getInstance()
						.convert(compositeState.getComposition());

				// This Function could be mts.getAlphabet() [[
				Set<Long> states = mts.getStates();
				Set<String> alphabet = new HashSet<String>();
				Map<Long, BinaryRelation<String, Long>> allTransitions = mts
						.getTransitions(TransitionType.REQUIRED);
				for (Entry<Long, BinaryRelation<String, Long>> transition : allTransitions
						.entrySet()) {
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
						BinaryRelation<String, Long> transitionsFromState = mts
								.getTransitions(state, TransitionType.REQUIRED);
						for (Pair<String, Long> pair : transitionsFromState) {
							posibleTransitions.remove(pair.getFirst());

						}

						for (String transition : posibleTransitions) {
							mts.addTransition(state, transition, new Long(-1),
									TransitionType.REQUIRED);
						}
					}
				}

				MTSToAutomataConverter converter = new MTSToAutomataConverter();
				CompactState compactState = (CompactState) converter.convert(
						mts, compositeState.name);
				compositeState.setComposition(compactState);
				output.outln("property model for: " + compositeState.name);
			} else {
				// TODO: CHECK WHAT TO DO IN THIS CASE
				Diagnostics.fatal("Property keword is not defined for MTS");
			}
		}

	}

  public static CompactState synthesiseControlStack(CompositeState cStackDef, LTSOutput output)
  {
    return ControlStackSynthesiser.synthesiseControlStack(cStackDef, output);
  }
	
  /*public static void makeProperty(CompositeState cs, LTSOutput output)
  {
    cs.composition.makeProperty();
  }*/

}
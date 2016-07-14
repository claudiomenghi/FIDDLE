package ltsa.updatingControllers.synthesis;

import static ltsa.updatingControllers.UpdateConstants.BEGIN_UPDATE;
import static ltsa.updatingControllers.UpdateConstants.RECONFIGURE;
import static ltsa.updatingControllers.UpdateConstants.START_NEW_SPEC;
import static ltsa.updatingControllers.UpdateConstants.STOP_OLD_SPEC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ltsa.control.ControllerGoalDefinition;
import ltsa.lts.Diagnostics;
import ltsa.lts.chart.util.FormulaUtils;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.lts.UpdatingControllersDefinition;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.LTSOutput;
import ltsa.lts.parser.Symbol;
import ltsa.updatingControllers.UpdateConstants;
import MTSSynthesis.ar.dc.uba.model.condition.Fluent;
import MTSSynthesis.ar.dc.uba.model.condition.FluentImpl;
import MTSSynthesis.ar.dc.uba.model.condition.FluentPropositionalVariable;
import MTSSynthesis.ar.dc.uba.model.language.SingleSymbol;
import MTSSynthesis.controller.model.gr.GRControllerGoal;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSTools.ac.ic.doc.mtstools.model.impl.MarkedMTS;
import MTSTools.ac.ic.doc.mtstools.utils.GraphUtils;

public class UpdatingControllersUtils {

	public static final Set<Fluent> UPDATE_FLUENTS = new HashSet<>();
	public static final Fluent beginFluent;
	public static final Fluent stopFluent;
	public static final Fluent reconFluent;
	public static final Fluent startFluent;

	static {
		HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol> beginAction = new HashSet<>();
		HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol> stopAction = new HashSet<>();
		HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol> reconfigureAction = new HashSet<>();
		HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol> startAction = new HashSet<>();
		beginAction.add(new SingleSymbol(UpdateConstants.BEGIN_UPDATE));
		stopAction.add(new SingleSymbol(UpdateConstants.STOP_OLD_SPEC));
		reconfigureAction.add(new SingleSymbol(UpdateConstants.RECONFIGURE));
		startAction.add(new SingleSymbol(UpdateConstants.START_NEW_SPEC));

		beginFluent = new FluentImpl("BeginUpdate", beginAction, new HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol>(), false);
		stopFluent = new FluentImpl("StopOldSpec", stopAction, new HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol>(), false);
		reconFluent = new FluentImpl("Reconfigure", reconfigureAction, new HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol>(), false);
		startFluent = new FluentImpl("StartNewSpec", startAction, new HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol>(), false);
		
		UpdatingControllersUtils.UPDATE_FLUENTS.add(beginFluent);
		UpdatingControllersUtils.UPDATE_FLUENTS.add(stopFluent);
		UpdatingControllersUtils.UPDATE_FLUENTS.add(reconFluent);
		UpdatingControllersUtils.UPDATE_FLUENTS.add(startFluent);
	}


	public static List<Fluent> compileFluents(List<Symbol> updPropertyDef) {
		List<Fluent> compiledDef = new ArrayList<Fluent>();
		for (Symbol toCompileProperty : updPropertyDef) {
			
			Set<Fluent> involvedFluents = new HashSet<Fluent>();
			
			LTSCompiler.makeFluents(toCompileProperty, involvedFluents);
			
			if (involvedFluents.size() == 1) {
				compiledDef.add(involvedFluents.iterator().next());
			} else {
				System.out.println("TWO OR MORE FLUENT NOT EXPECTED");
			}
		}
		return compiledDef;
	}

	public static List<String> compileCheckTraces(List<Symbol> checkTraceDef) {

		ArrayList<String> compiledDef = new ArrayList<>();

		for (Symbol symbolAction : checkTraceDef) {
			compiledDef.add(symbolAction.toString());
		}

		return compiledDef;
	}

	public static GRControllerGoal<String> generateGRUpdateGoal(UpdatingControllersDefinition updContDef,
																ControllerGoalDefinition oldGoalDef,
																ControllerGoalDefinition newGoalDef, Set<String>
																	controllableSet) {
		GRControllerGoal<String> grcg = new GRControllerGoal<String>();

		grcg.setNonBlocking(updContDef.isNonblocking());
		grcg.addAllControllableActions(controllableSet);
		Set<Fluent> involvedFluents = new HashSet<Fluent>();

		addFluentAndAssumption(grcg, involvedFluents, BEGIN_UPDATE);
		addFluentAndGuarantee(grcg, involvedFluents, STOP_OLD_SPEC);
		addFluentAndGuarantee(grcg, involvedFluents, START_NEW_SPEC);
		addFluentAndGuarantee(grcg, involvedFluents, RECONFIGURE);
		addFailures(grcg, involvedFluents, oldGoalDef);
		addFailures(grcg, involvedFluents, newGoalDef);
		grcg.addAllFluents(involvedFluents);
		return grcg;
	}

	public static ControllerGoalDefinition generateSafetyGoalDef(UpdatingControllersDefinition updContDef,
																 ControllerGoalDefinition oldGoalDef,
																 ControllerGoalDefinition newGoalDef, Symbol
																	 controllableSetSymbol, LTSOutput output) {
		ControllerGoalDefinition cgd = new ControllerGoalDefinition(updContDef.getName());
		cgd.addAssumeDefinition(new Symbol(123, "BeginUpdate")); //is this useless?we use the assumption in GR not here
		cgd.addGuaranteeDefinition(new Symbol(123, "StopOldSpec")); //is this useless? we use Guarantee in GR not here
		cgd.addGuaranteeDefinition(new Symbol(123, "StartNewSpec")); //besides the symbol redirects to nothing.
		cgd.addGuaranteeDefinition(new Symbol(123, "Reconfigure"));
		addFailures(cgd, oldGoalDef); //same here, failures are for liveness right?
		addFailures(cgd, newGoalDef);

		cgd.setControllableActionSet(controllableSetSymbol);
		cgd.setNonBlocking(updContDef.isNonblocking());

		generateUpdateGoals(oldGoalDef, newGoalDef, updContDef.getSafety(), cgd);

//		UpdatingControllersGoalsMaker.addDontDoTwiceGoal(cgd, STOP_OLD_SPEC, DONT_DROP_TWICE);
//		UpdatingControllersGoalsMaker.addDontDoTwiceGoal(cgd, START_NEW_SPEC, DONT_START_TWICE);

		// adding ControllerGoalDefinition
		ControllerGoalDefinition.addDefinition(cgd.getNameString(), cgd);
		AssertDefinition.compileAll(output); // this is for filling the fac attribute in constraints added before
		return cgd;
	}

	/**
	 * @param action
	 * @return whether action is not one of the controller update special actions.
	 */
	public static boolean isNotUpdateAction(String action) {
		return !START_NEW_SPEC.equals(action) && !STOP_OLD_SPEC.equals(action) &&
			!RECONFIGURE.equals(action);
	}

	/**
	 * @param cgd
	 * @param controllerGoalDef
	 */
	private static void addFailures(ControllerGoalDefinition cgd, ControllerGoalDefinition controllerGoalDef) {
		//Check with dipi. we are not sure if this will work as expected
		List<Symbol> faultsDefinitions = controllerGoalDef.getFaultsDefinitions();
		for (Symbol faultsDefinition : faultsDefinitions) {
			cgd.addFaultDefinition(faultsDefinition);
		}
	}

	/**
	 * @param grcg
	 * @param involvedFluents
	 * @param controllerGoalDefinition
	 */
	private static void addFailures(GRControllerGoal<String> grcg, Set<Fluent> involvedFluents,
									ControllerGoalDefinition controllerGoalDefinition) {
		// TODO: refactor, code copied from GoalDefToControllerGoal.
		// Check with dipi. we are not sure if this will work as expected
		Set<Fluent> fluentsInFaults = new HashSet<Fluent>();
		for (ltsa.lts.parser.Symbol faultsDefinition : controllerGoalDefinition.getFaultsDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(faultsDefinition.getValue());
			if (def != null) {
				grcg.addFault(FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), fluentsInFaults));
			} else {
				Diagnostics.fatal("Assertion not defined [" + faultsDefinition.getValue() + "].");
			}
		}
		involvedFluents.addAll(fluentsInFaults);
		grcg.addAllFluentsInFaults(fluentsInFaults);
	}

	/**
	 * @param grcg
	 * @param fluents
	 * @param action
	 */
	private static void addFluentAndAssumption(GRControllerGoal<String> grcg, Set<Fluent> fluents, String action) {
		FluentPropositionalVariable fluentPropositionalVariable = generateAndAddFluent(fluents, action);
		grcg.addAssume(fluentPropositionalVariable);
	}

	/**
	 * @param grcg
	 * @param fluents
	 * @param action
	 */
	private static void addFluentAndGuarantee(GRControllerGoal<String> grcg, Set<Fluent> fluents, String action) {
		FluentPropositionalVariable fluentPropositionalVariable = generateAndAddFluent(fluents, action);
		grcg.addGuarantee(fluentPropositionalVariable);
	}

	/**
	 * @param fluents
	 * @param action
	 * @return
	 */
	private static FluentPropositionalVariable generateAndAddFluent(Set<Fluent> fluents, String action) {
		Fluent turnOnFluent = UpdatingControllerHandler.createOnlyTurnOnFluent(action);
		fluents.add(turnOnFluent);
		FluentPropositionalVariable fluentPropositionalVariable = new FluentPropositionalVariable(turnOnFluent);
		return fluentPropositionalVariable;
	}

	/**
	 * Changes the safety goals of each controller to (OLD W stopOldSPec) and [](startNewSpec -> []NEW). Adds T too.
	 *
	 * @param oldGoalDef
	 * @param newGoalDef
	 * @param safety
	 * @param cgd
	 */
	private static void generateUpdateGoals(ControllerGoalDefinition oldGoalDef, ControllerGoalDefinition newGoalDef,
											List<Symbol> safety, ControllerGoalDefinition cgd) {
		
		for (Symbol formula : oldGoalDef.getSafetyDefinitions()) {
			UpdatingControllersGoalsMaker.addOldGoals(formula, cgd);
		}
		for (Symbol formula : newGoalDef.getSafetyDefinitions()) {
			UpdatingControllersGoalsMaker.addImplyUpdatingGoal(formula, cgd);
		}
		for (Symbol transitionSymbol : safety) {
			cgd.addSafetyDefinition(transitionSymbol);
		}
	}

	/**
	 * Marks the states of the update controller (Cu) that belong to the terminal set.
	 *
	 * @param updateController
	 * @return a minimized CompactState of updateController.
	 */
	public static MarkedMTS<Long, String> markCuTerminalSet(MTS<Long, String> updateController) {
		Set<Set<Long>> stronglyConnectedComponents = GraphUtils.getStronglyConnectedComponents(updateController);
		Set<Set<Long>> terminalSets = GraphUtils.getTerminalSets(stronglyConnectedComponents, updateController);
		//		MarkedCompactState markedCu = new MarkedCompactState(updateController, terminalSet, name);
		//		CompactState minimised = TransitionSystemDispatcher.minimise(markedCu, output);
		return new MarkedMTS<Long, String>(updateController, updateController.getInitialState(), terminalSets);
	}
}
package updatingControllers.synthesis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import lts.CompactState;
import lts.Diagnostics;
import lts.LTSOutput;
import lts.chart.util.FormulaUtils;
import lts.ltl.AssertDefinition;

import org.apache.commons.collections15.map.HashedMap;

import updatingControllers.UpdateConstants;
import updatingControllers.structures.UpdatingControllerCompositeState;
import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ac.ic.doc.mtstools.utils.GenericMTSToLongStringMTSConverter;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.Formula;
import control.ControllerGoalDefinition;
import control.util.ControllerUtils;
import controller.game.gr.GRGameSolver;
import controller.game.gr.GRRankSystem;
import controller.game.gr.StrategyState;
import controller.game.gr.perfect.PerfectInfoGRGameSolver;
import controller.game.model.Strategy;
import controller.game.util.FluentStateValuation;
import controller.game.util.GRGameBuilder;
import controller.game.util.GameStrategyToMTSBuilder;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.GRGame;

/**
 * Created by Victor Wjugow on 10/06/15.
 */
public class UpdatingControllerSynthesizer {

	/**
	 *
	 * @param uccs
	 * @param output
	 * @author leanaha
	 */
	public static void generateController(
			UpdatingControllerCompositeState uccs, LTSOutput output) {
		Set<String> contActions = uccs.getControllableActions();

		// set environment
		MTS<Long, String> oldC = uccs.getOldController();
		MTS<Long, String> oldE = uccs.getOldEnvironment();
		MTS<Long, String> hatE = uccs.getHatEnvironment();
		MTS<Long, String> newE = uccs.getNewEnvironment();
		List<Fluent> propositions = uccs.getUpdProperties();
		UpdatingEnvironmentGenerator updEnvGenerator = new UpdatingEnvironmentGenerator(
				oldC, oldE, hatE, newE, propositions);
		updEnvGenerator.generateEnvironment(contActions, output);
		

		if (!uccs.debugModeOn() && uccs.getCheckTrace().isEmpty()) {

			solveControlProblem(uccs, updEnvGenerator, uccs.getNewGoalGR(),
					uccs.getNewGoalDef(), output);

		} else {
			if (!uccs.debugModeOn()) {
				// removed support for debugging
				// updateHandler.checkMappingValue(uccs.getCheckTrace(),
				// output);
			}
		}
	}

	private static void solveControlProblem(
			UpdatingControllerCompositeState uccs, UpdatingEnvironmentGenerator updEnvGenerator,
			GRControllerGoal<String> newGoalGR,
			ControllerGoalDefinition newGoalDef, LTSOutput output) {

		MTS<Long, String> environment = updEnvGenerator.getUpdEnv();
		MTS<Long, String> metaEnvironment = ControllerUtils.removeTopStates(
				environment, UpdatingControllersUtils.UPDATE_FLUENTS);

		HashedMap<Long, ArrayList<Boolean>> valuation = buildValuationsManually(
				environment, metaEnvironment, updEnvGenerator);

		ArrayList<Fluent> propositions = (ArrayList<Fluent>) updEnvGenerator
				.getPropositions();

		FluentStateValuation<Long> fluentStateValuation = buildFluentStateValuation(metaEnvironment, valuation, propositions);

		Set<Fluent> safetyFluents = new HashSet<Fluent>();
		List<Formula> safetyFormulas = new ArrayList<Formula>();
		for (lts.Symbol safetyDefinition : newGoalDef.getSafetyDefinitions()) {
			AssertDefinition def = AssertDefinition
					.getConstraint(safetyDefinition.getName());
			if (def != null) {
				safetyFormulas.add(FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(false), safetyFluents));

			} else {
				Diagnostics.fatal("Assertion not defined ["
						+ safetyDefinition.getName() + "].");
			}
		}
		HashSet<Long> toBuild = new HashSet<Long>();
		formulaToStateSet(toBuild, metaEnvironment.getStates(), safetyFormulas,	fluentStateValuation);
		
		for (Long state : toBuild) {
			for (Pair<String, Long> transition : metaEnvironment.getTransitions(state, MTS.TransitionType.REQUIRED)) {
				metaEnvironment.removeRequired(state, transition.getFirst(), transition.getSecond());
			}
		}
		metaEnvironment.removeUnreachableStates();

		uccs.setUpdateEnvironment(metaEnvironment);
		CompactState compactEnv = MTSToAutomataConverter.getInstance().convert(metaEnvironment, "E_u||G(safety)", false);

		Vector<CompactState> machines = new Vector<CompactState>();
		machines.add(compactEnv);

		uccs.setMachines(machines);
		
		
		GRGame<Long> game;

		game = new GRGameBuilder<Long, String>().buildGRGameFrom(metaEnvironment,newGoalGR);
		GRRankSystem<Long> system = new GRRankSystem<Long>(game.getStates(),game.getGoal().getGuarantees(),
				game.getGoal().getAssumptions(), game.getGoal().getFailures());
		PerfectInfoGRGameSolver solver = new PerfectInfoGRGameSolver<Long>(game, system);
		solver.solveGame();
		
		
		// ojo que el estado del environment puede tener problemas.
		if (solver.isWinning(metaEnvironment.getInitialState())) {
			Strategy<Long, Integer> strategy = solver.buildStrategy();
			GRGameSolver<Long> grSolver = (GRGameSolver<Long>) solver;
			Set<Pair<StrategyState<Long, Integer>, StrategyState<Long, Integer>>> worseRank = grSolver.getWorseRank();
			MTS<StrategyState<Long, Integer>, String> result = GameStrategyToMTSBuilder.getInstance().buildMTSFrom(metaEnvironment, strategy, worseRank, null);

			if (result == null) {
				output.outln("There is no controller for model " + uccs.name + " for the given setting.");
				// output.outln("Analysis time: " + (System.currentTimeMillis()
				// - initialTime) + "ms.");
				uccs.setComposition(null);
			} else {
				GenericMTSToLongStringMTSConverter<StrategyState<Long, Integer>, String> transformer = new GenericMTSToLongStringMTSConverter<StrategyState<Long, Integer>, String>();
				MTS<Long, String> plainController = transformer.transform(result);

				// output.outln("Analysis time: " + (System.currentTimeMillis()
				// - initialTime) + "ms.");
				output.outln("Controller [" + plainController.getStates().size() + "] generated successfully.");
				CompactState convert = MTSToAutomataConverter.getInstance().convert(plainController, uccs.getName());
				uccs.setComposition(convert);
			}
		}
	}

	private static void formulaToStateSet(Set<Long> toBuild, Set<Long> allStates, List<Formula> formulas, FluentStateValuation<Long> valuation) {

		for (Formula formula : formulas) {
			for (Long state : allStates) {
				formulaToStateSet(toBuild, formula, state, valuation);
			}
			if (toBuild.isEmpty()) {
				Logger.getAnonymousLogger().log(Level.WARNING,
						"No state satisfies formula: " + formula);
			}
		}
	}

	private static void formulaToStateSet(Set<Long> toBuild, Formula formula, Long state, FluentStateValuation<Long> valuation) {
		valuation.setActualState(state);
		if (formula.evaluate(valuation)) {
			toBuild.add(state);
		}
	}

	private static FluentStateValuation<Long> buildFluentStateValuation(
			MTS<Long, String> metaEnvironment,
			HashedMap<Long, ArrayList<Boolean>> valuation,
			ArrayList<Fluent> propositions) {
		FluentStateValuation<Long> fluentValuation = new FluentStateValuation<Long>(
				metaEnvironment.getStates());
		for (Entry<Long, ArrayList<Boolean>> valuationEntry : valuation
				.entrySet()) {

			ArrayList<Boolean> thisValuation = valuationEntry.getValue();
			for (int i = 0; i < thisValuation.size(); i++) {
				if (thisValuation.get(i) && i < propositions.size()) {
					fluentValuation.addHoldingFluent(valuationEntry.getKey(),
							propositions.get(i));
				} else if (thisValuation.get(i) && i == propositions.size()) {
					fluentValuation.addHoldingFluent(valuationEntry.getKey(),
							UpdatingControllersUtils.beginFluent);
				} else if (thisValuation.get(i) && i == propositions.size() + 1) {
					fluentValuation.addHoldingFluent(valuationEntry.getKey(),
							UpdatingControllersUtils.stopFluent);
				} else if (thisValuation.get(i) && i == propositions.size() + 2) {
					fluentValuation.addHoldingFluent(valuationEntry.getKey(),
							UpdatingControllersUtils.startFluent);
				} else if (thisValuation.get(i) && i == propositions.size() + 3) {
					fluentValuation.addHoldingFluent(valuationEntry.getKey(),
							UpdatingControllersUtils.reconFluent);
				}
			}
		}
		return fluentValuation;
	}

	// order is: {OriginalFluents, begin, stopOld, startNew, reconfig}
	private static HashedMap<Long, ArrayList<Boolean>> buildValuationsManually(
			MTS<Long, String> env, MTS<Long, String> metaEnv,
			UpdatingEnvironmentGenerator updEnvGenerator) {

		HashedMap<Long, ArrayList<Boolean>> resultantValuation = new HashedMap<Long, ArrayList<Boolean>>();
		HashedMap<Long, Long> statesMapping = new HashedMap<Long, Long>();

		statesMapping.put(metaEnv.getInitialState(), env.getInitialState());
		ArrayList<Boolean> initialValuation = new ArrayList<Boolean>(
				updEnvGenerator.getOldValuation(env.getInitialState()));
		initialValuation.add(false); // initial State beginUpdate is false
		initialValuation.add(false); // initial State stopOld is false
		initialValuation.add(false); // initial State startNew is false
		initialValuation.add(false); // initial State reconfig is false
		resultantValuation.put(metaEnv.getInitialState(), initialValuation);
		// BFS
		Queue<Long> toVisit = new LinkedList<Long>();
		Long firstState = new Long(metaEnv.getInitialState());
		toVisit.add(firstState);
		ArrayList<Long> discovered = new ArrayList<Long>();

		while (!toVisit.isEmpty()) {
			Long actualInMetaEnv = toVisit.remove();
			if (!discovered.contains(actualInMetaEnv)) {
				discovered.add(actualInMetaEnv);

				if (updEnvGenerator.isEParrallelCState(statesMapping
						.get(actualInMetaEnv))) {
					ArrayList<Boolean> valuation = new ArrayList<Boolean>(
							updEnvGenerator.getOldValuation(statesMapping
									.get(actualInMetaEnv)));
					valuation.add(false); // beginUpdate is false in E||C
					valuation.add(false); // stopOldSpec is false in E||C
					valuation.add(false); // startNewSpec is false in E||C
					valuation.add(false); // reconfigure is false in E||C
					resultantValuation.put(actualInMetaEnv, valuation);

				} else if (updEnvGenerator.isHatEnvironmentState(statesMapping
						.get(actualInMetaEnv))) {
					ArrayList<Boolean> valuation = new ArrayList<Boolean>(
							updEnvGenerator.getOldValuation(statesMapping
									.get(actualInMetaEnv)));
					valuation.add(true); // beginUpdate is true in E
					valuation.add(isTrueStop(metaEnv, actualInMetaEnv)); // stopOldSpec
																			// is
																			// false
																			// in
																			// E
					valuation.add(IsTrueStart(metaEnv, actualInMetaEnv)); // startNewSpec
																			// is
																			// false
																			// in
																			// E
					valuation.add(false); // reconfigure is false in E
					resultantValuation.put(actualInMetaEnv, valuation);

				} else { // is new Part
					Long magicState = updEnvGenerator
							.mapStateToValuationState(statesMapping
									.get(actualInMetaEnv));
					ArrayList<Boolean> valuation = new ArrayList<Boolean>(
							updEnvGenerator.getNewValuation(magicState));
					valuation.add(true); // beginUpdate is true in E'
					valuation.add(isTrueStop(metaEnv, actualInMetaEnv)); // stopOldSpec
																			// is
																			// false
																			// in
																			// E'
					valuation.add(IsTrueStart(metaEnv, actualInMetaEnv)); // startNewSpec
																			// is
																			// false
																			// in
																			// E'
					valuation.add(true); // reconfigure is true in E'
					resultantValuation.put(actualInMetaEnv, valuation);
				}

				for (Pair<String, Long> action_toStateInMetaEnv : metaEnv
						.getTransitions(actualInMetaEnv,
								MTS.TransitionType.REQUIRED)) {

					toVisit.addAll(nextStatesToVisit(actualInMetaEnv,
							action_toStateInMetaEnv, env, statesMapping));
				}
			}
		}

		return resultantValuation;
	}

	private static ArrayList<Long> nextStatesToVisit(Long actualInMetaEnv,
			Pair<String, Long> action_toStateInMetaEnv, MTS<Long, String> env,
			HashedMap<Long, Long> statesMapping) {

		ArrayList<Long> toVisit = new ArrayList<Long>();

		for (Pair<String, Long> action_toStateInEnv : env
				.getTransitions(statesMapping.get(actualInMetaEnv),
						MTS.TransitionType.REQUIRED)) {

			String actionInEnv = action_toStateInEnv.getFirst();
			Long toStateInEnv = action_toStateInEnv.getSecond();

			if (action_toStateInMetaEnv.getFirst().equals(actionInEnv)) {

				Long toStateInMetaEnv = action_toStateInMetaEnv.getSecond();

				statesMapping.put(toStateInMetaEnv, toStateInEnv);
				toVisit.add(toStateInMetaEnv);

			}
		}

		return toVisit;
	}

	private static Boolean IsTrueStart(MTS<Long, String> metaEnv, Long metaState) {

		for (Pair<String, Long> transition : metaEnv.getTransitions(metaState,
				MTS.TransitionType.REQUIRED)) {
			if (transition.getFirst().equals(UpdateConstants.START_NEW_SPEC)
					&& transition.getSecond().equals(metaState)) {
				return true;
			}
		}
		return false;
	}

	private static Boolean isTrueStop(MTS<Long, String> metaEnv, Long metaState) {

		for (Pair<String, Long> transition : metaEnv.getTransitions(metaState,
				MTS.TransitionType.REQUIRED)) {
			if (transition.getFirst().equals(UpdateConstants.STOP_OLD_SPEC)
					&& transition.getSecond().equals(metaState)) {
				return true;
			}
		}
		return false;

	}

}

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
import ac.ic.doc.mtstools.model.MTS.TransitionType;
import ac.ic.doc.mtstools.model.impl.LTSAdapter;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import ac.ic.doc.mtstools.model.impl.MTSImpl;
import ac.ic.doc.mtstools.model.operations.TauRemoval;
import ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ac.ic.doc.mtstools.utils.GenericMTSToLongStringMTSConverter;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.FluentUtils;
import ar.dc.uba.model.condition.Formula;
import control.ControllerGoalDefinition;
import control.util.ControllerUtils;
import controller.game.gr.GRGameSolver;
import controller.game.gr.GRRankSystem;
import controller.game.gr.StrategyState;
import controller.game.gr.knowledge.KnowledgeGRGame;
import controller.game.gr.knowledge.KnowledgeGRGameSolver;
import controller.game.gr.perfect.PerfectInfoGRGameSolver;
import controller.game.model.Assume;
import controller.game.model.Assumptions;
import controller.game.model.Guarantee;
import controller.game.model.Guarantees;
import controller.game.model.Strategy;
import controller.game.util.FluentStateValuation;
import controller.game.util.GRGameBuilder;
import controller.game.util.GameStrategyToMTSBuilder;
import controller.game.util.SubsetConstructionBuilder;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.GRGame;
import controller.model.gr.GRGoal;

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
		UpdatingEnvironmentGenerator updEnvGenerator = new UpdatingEnvironmentGenerator(oldC, oldE, hatE, newE, propositions);
		updEnvGenerator.generateEnvironment(contActions, output);

		if (!uccs.debugModeOn() && uccs.getCheckTrace().isEmpty()) {

			solveControlProblem(uccs, updEnvGenerator, uccs.getNewGoalGR(),
					uccs.getNewGoalDef(), output);

		} else {
			if (!uccs.debugModeOn()) {
				// removed support for debugging
//				 updateHandler.checkMappingValue(uccs.getCheckTrace(), output);
			}
		}
	}

	private static void solveControlProblem(
			UpdatingControllerCompositeState uccs, UpdatingEnvironmentGenerator updEnvGenerator,
			GRControllerGoal<String> newGoalGR,
			ControllerGoalDefinition newGoalDef, LTSOutput output) {

		MTS<Long, String> environment = updEnvGenerator.getUpdEnv();
		MTS<Long, String> metaEnvironment = ControllerUtils.removeTopStates(environment, UpdatingControllersUtils.UPDATE_FLUENTS);

		output.outln("Environment states:"+ metaEnvironment.getStates().size());
		
		Pair<HashedMap<Long, ArrayList<Boolean>>, HashSet<Long>> rarePair = buildValuationsManually(environment, metaEnvironment, updEnvGenerator);
		
		HashedMap<Long, ArrayList<Boolean>> valuation = rarePair.getFirst();
		HashSet<Long> eParallelCStates = rarePair.getSecond();
		
		ArrayList<Fluent> propositions = (ArrayList<Fluent>) updEnvGenerator.getPropositions();
		FluentStateValuation<Long> fluentStateValuation = buildFluentStateValuation(metaEnvironment, valuation, propositions);

		makeOldActionsUncontrollable(newGoalGR.getControllableActions(), eParallelCStates, metaEnvironment);
		MTS<Long, String> safetyEnv = applySafety(newGoalDef, metaEnvironment, fluentStateValuation);

		output.outln("Environment states after safety: "+ safetyEnv.getStates().size());
		
		uccs.setUpdateEnvironment(safetyEnv);

		CompactState compactSafetyEnv = MTSToAutomataConverter.getInstance().convert(safetyEnv, "E_u||G(safety)", false);
//		CompactState compactMetaEnv = MTSToAutomataConverter.getInstance().convert(metaEnvironment, "meta E_u", false);
//		CompactState compactEnv = MTSToAutomataConverter.getInstance().convert(environment, "E_u", false);
		
		Vector<CompactState> machines = new Vector<CompactState>();
		machines.add(compactSafetyEnv);
//		machines.add(compactMetaEnv);
//		machines.add(compactEnv);

		uccs.setMachines(machines);
		
		output.outln("Synthezising GR");
		if (compactSafetyEnv.isNonDeterministic()){
			output.outln("Environment after safety is non-deterministic");
			output.outln("Solving a non-deterministic controller synthesis");
			nonBlockingGR(uccs, newGoalGR, output, safetyEnv);
		} else {
			output.outln("Environment after safety is deterministic");
			output.outln("Solving a deterministic controller synthesis");
			synthesizeGRDeterministic(uccs, newGoalGR, output, safetyEnv);
		}
		
	}

	private static void nonBlockingGR(UpdatingControllerCompositeState uccs, GRControllerGoal<String> newGoalGR, LTSOutput output, MTS<Long, String> safetyEnv) {
		
		KnowledgeGRGame<Long, String> game;
		GRGoal<Set<Long>> grGoal;
		MTS<Set<Long>, String> perfectInfoGame;
		SubsetConstructionBuilder<Long, String> subsetConstructionBuilder;
		
		FluentUtils fluentUtils = FluentUtils.getInstance();

		subsetConstructionBuilder = new SubsetConstructionBuilder<Long, String>(safetyEnv);
		
		perfectInfoGame = subsetConstructionBuilder.build();
		
		FluentStateValuation<Set<Long>> valuation = fluentUtils.buildValuation(perfectInfoGame, newGoalGR.getFluents());
		Assumptions<Set<Long>> assumptions = formulasToAssumptions(perfectInfoGame.getStates(), newGoalGR.getAssumptions(), valuation);
		Guarantees<Set<Long>> guarantees = formulasToGuarantees(perfectInfoGame.getStates(), newGoalGR.getGuarantees(), valuation);
		Set<Set<Long>> faults = new HashSet<Set<Long>>();
		
		grGoal = new GRGoal<Set<Long>>(guarantees, assumptions, faults, newGoalGR.isPermissive());
		Set<Set<Long>> initialStates = new HashSet<Set<Long>>();
		Set<Long> initialState = new HashSet<Long>();
		initialState.add(safetyEnv.getInitialState());
		initialStates.add(initialState);
		
		game = new KnowledgeGRGame<Long, String>(initialStates, safetyEnv, perfectInfoGame, newGoalGR.getControllableActions(), grGoal);
		
		GRRankSystem<Set<Long>> system = new GRRankSystem<Set<Long>>(game.getStates(), grGoal.getGuarantees(), grGoal.getAssumptions(), grGoal.getFailures());

		KnowledgeGRGameSolver<Long, String> solver = new KnowledgeGRGameSolver<Long, String>(game, system);
		solver.solveGame();

		if (solver.isWinning(perfectInfoGame.getInitialState())) {
			Strategy<Set<Long>, Integer> strategy = solver.buildStrategy();

			Set<Pair<StrategyState<Set<Long>, Integer>, StrategyState<Set<Long>, Integer>>> worseRank = solver.getWorseRank();
			MTS<StrategyState<Set<Long>, Integer>, String> result = GameStrategyToMTSBuilder.getInstance().buildMTSFrom(perfectInfoGame, strategy, worseRank);

			result.removeUnreachableStates();
			LTSAdapter<StrategyState<Set<Long>, Integer>, String> ltsAdapter = new LTSAdapter<StrategyState<Set<Long>,Integer>, String>(result, TransitionType.POSSIBLE);
			MTS<StrategyState<Set<Long>, Integer>, String> synthesised  = new MTSAdapter<StrategyState<Set<Long>,Integer>, String>(ltsAdapter);
			MTS<Long, String> plainController = new GenericMTSToLongStringMTSConverter<StrategyState<Set<Long>, Integer>, String>().transform(synthesised);

			output.outln("Controller [" + plainController.getStates().size() + "] generated successfully.");
			CompactState compactState = MTSToAutomataConverter.getInstance().convert(plainController, uccs.getName(), false);
			uccs.setComposition(compactState);
		} else {
			output.outln("There is no controller for model " + uccs.name + " for the given setting.");
			uccs.setComposition(null);		
		}
	}

	private static void synthesizeGRDeterministic(UpdatingControllerCompositeState uccs, GRControllerGoal<String> newGoalGR, LTSOutput output, MTS<Long, String> safetyEnv) {
		GRGame<Long> game;

		game = new GRGameBuilder<Long, String>().buildGRGameFrom(safetyEnv,newGoalGR);
		GRRankSystem<Long> system = new GRRankSystem<Long>(game.getStates(),game.getGoal().getGuarantees(),
				game.getGoal().getAssumptions(), game.getGoal().getFailures());
		PerfectInfoGRGameSolver<Long> solver = new PerfectInfoGRGameSolver<Long>(game, system);
		solver.solveGame();
		
		if (solver.isWinning(safetyEnv.getInitialState())) {
			Strategy<Long, Integer> strategy = solver.buildStrategy();
			GRGameSolver<Long> grSolver = (GRGameSolver<Long>) solver;
			Set<Pair<StrategyState<Long, Integer>, StrategyState<Long, Integer>>> worseRank = grSolver.getWorseRank();
			MTS<StrategyState<Long, Integer>, String> result = GameStrategyToMTSBuilder.getInstance().buildMTSFrom(safetyEnv, strategy, worseRank, newGoalGR.getLazyness());

			if (result == null) {
				output.outln("There is no controller for model " + uccs.name + " for the given setting.");
				uccs.setComposition(null);
			} else {
				GenericMTSToLongStringMTSConverter<StrategyState<Long, Integer>, String> transformer = new GenericMTSToLongStringMTSConverter<StrategyState<Long, Integer>, String>();
				MTS<Long, String> plainController = transformer.transform(result);

				output.outln("Controller [" + plainController.getStates().size() + "] generated successfully.");
				CompactState convert = MTSToAutomataConverter.getInstance().convert(plainController, uccs.getName());
				uccs.setComposition(convert);
			}
		} else {
			output.outln("There is no controller for model " + uccs.name + " for the given setting.");
			uccs.setComposition(null);
		}
		
	}

	private static MTS<Long, String> applySafety( ControllerGoalDefinition newGoalDef, MTS<Long, String> metaEnvironment, FluentStateValuation<Long> fluentStateValuation) {
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
		
		MTS<Long, String> safetyEnv = applySafetyInEnvironment(metaEnvironment, toBuild);
		return safetyEnv;
	}

	private static MTS<Long, String> applySafetyInEnvironment(MTS<Long, String> metaEnvironment, HashSet<Long> toBuild) {
		
		MTS<Long, String> result = new MTSImpl<Long, String>(metaEnvironment.getInitialState());
		
		for (Long state : metaEnvironment.getStates()) {
			result.addState(state);
			if (!toBuild.contains(state)){
				for (Pair<String, Long> transition : metaEnvironment.getTransitions(state, MTS.TransitionType.REQUIRED)) {
					
					result.addState(transition.getSecond());
					result.addAction(transition.getFirst());
					
					result.addRequired(state, transition.getFirst(), transition.getSecond());
				}
			}
			
		}
		result.removeUnreachableStates();
		return result;
		
	}

	private static void formulaToStateSet(Set<Long> toBuild, Set<Long> allStates, List<Formula> formulas, FluentStateValuation<Long> valuation) {

		for (Formula formula : formulas) {
			for (Long state : allStates) {
				formulaToStateSet(toBuild, formula, state, valuation);
			}
			if (toBuild.isEmpty()) {
				Logger.getAnonymousLogger().log(Level.WARNING, "No state satisfies formula: " + formula);
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
	private static Pair<HashedMap<Long, ArrayList<Boolean>>, HashSet<Long>> buildValuationsManually(
			MTS<Long, String> env, MTS<Long, String> metaEnv,
			UpdatingEnvironmentGenerator updEnvGenerator) {

		HashedMap<Long, ArrayList<Boolean>> resultantValuation = new HashedMap<Long, ArrayList<Boolean>>();
		HashedMap<Long, Long> statesMapping = new HashedMap<Long, Long>();
		HashSet<Long> eParallelCStates = new HashSet<Long>();

		statesMapping.put(metaEnv.getInitialState(), env.getInitialState());
		ArrayList<Boolean> initialValuation = new ArrayList<Boolean>(updEnvGenerator.getOldValuation(env.getInitialState()));
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

				if (updEnvGenerator.isEParrallelCState(statesMapping.get(actualInMetaEnv))) {
					ArrayList<Boolean> valuation = new ArrayList<Boolean>(updEnvGenerator.getOldValuation(statesMapping.get(actualInMetaEnv)));
					valuation.add(false); // beginUpdate is false in E||C
					valuation.add(false); // stopOldSpec is false in E||C
					valuation.add(false); // startNewSpec is false in E||C
					valuation.add(false); // reconfigure is false in E||C
					resultantValuation.put(actualInMetaEnv, valuation);
					eParallelCStates.add(actualInMetaEnv);

				} else if (updEnvGenerator.isHatEnvironmentState(statesMapping.get(actualInMetaEnv))) {
					ArrayList<Boolean> valuation = new ArrayList<Boolean>(updEnvGenerator.getOldValuation(statesMapping.get(actualInMetaEnv)));
					valuation.add(true); // beginUpdate is true in E
					valuation.add(isTrueStop(metaEnv, actualInMetaEnv)); 
					valuation.add(IsTrueStart(metaEnv, actualInMetaEnv));
					valuation.add(false); // reconfigure is false in E
					resultantValuation.put(actualInMetaEnv, valuation);

				} else { // is new Part
//					Long magicState = updEnvGenerator.mapStateToValuationState(statesMapping.get(actualInMetaEnv));
//					ArrayList<Boolean> valuation = new ArrayList<Boolean>(updEnvGenerator.getNewValuation(magicState));
//					valuation.add(true); // beginUpdate is true in E'
//					valuation.add(isTrueStop(metaEnv, actualInMetaEnv)); 
//					valuation.add(IsTrueStart(metaEnv, actualInMetaEnv));
//					valuation.add(true); // reconfigure is true in E'
//					resultantValuation.put(actualInMetaEnv, valuation);
					resultantValuation.put(actualInMetaEnv, new ArrayList<Boolean>());
				}

				for (Pair<String, Long> action_toStateInMetaEnv : metaEnv.getTransitions(actualInMetaEnv,MTS.TransitionType.REQUIRED)) {

					toVisit.addAll(nextStatesToVisit(actualInMetaEnv, action_toStateInMetaEnv, env, statesMapping));
				}
			}
		}

		completeResultantValuationWithNewEnvironment(env, metaEnv, resultantValuation, updEnvGenerator);
		
		
		return new Pair<HashedMap<Long, ArrayList<Boolean>>, HashSet<Long>>(resultantValuation,eParallelCStates);
	}

	private static ArrayList<Long> nextStatesToVisit(Long actualInMetaEnv,
			Pair<String, Long> action_toStateInMetaEnv, MTS<Long, String> env,
			HashedMap<Long, Long> statesMapping) {

		ArrayList<Long> toVisit = new ArrayList<Long>();
		
		for (Pair<String, Long> action_toStateInEnv : env
				.getTransitions(statesMapping.get(actualInMetaEnv),	MTS.TransitionType.REQUIRED)) {

			String actionInEnv = action_toStateInEnv.getFirst();
			Long toStateInEnv = action_toStateInEnv.getSecond();

			if (action_toStateInMetaEnv.getFirst().equals(actionInEnv) && !actionInEnv.equals(UpdateConstants.RECONFIGURE)) {

				Long toStateInMetaEnv = action_toStateInMetaEnv.getSecond();

				statesMapping.put(toStateInMetaEnv, toStateInEnv);
				toVisit.add(toStateInMetaEnv);

			}
		}

		return toVisit;
	}
	
	private static void completeResultantValuationWithNewEnvironment(MTS<Long, String> env, MTS<Long, String> metaEnv, HashedMap<Long, ArrayList<Boolean>> resultantValuation, UpdatingEnvironmentGenerator updEnvGenerator) {
		
		Long initialEprimeMetaEnv = findNewEnvInitialState(metaEnv);
		Long initialEprimeEnv = findNewEnvInitialState(env);
		
		HashedMap<Long, Long> statesMapping = new HashedMap<Long, Long>();

		statesMapping.put(initialEprimeMetaEnv, initialEprimeEnv);
		Long initialStateInEPrimeWithOriginalId = updEnvGenerator.mapStateToValuationState(initialEprimeEnv);
		ArrayList<Boolean> initialValuation = new ArrayList<Boolean>(updEnvGenerator.getNewValuation(initialStateInEPrimeWithOriginalId));
		initialValuation.add(true); // beginUpdate -> reconfigure traces fires beginUpdate
		initialValuation.add(false); // beginUpdate -> reconfigure trace does not fire stopOldSpec
		initialValuation.add(false); // beginUpdate -> reconfigure trace does not fire startNewSpec
		initialValuation.add(true); // beginUpdate -> reconfigure traces fires reconfigure
		resultantValuation.put(initialEprimeMetaEnv, initialValuation);
		
		// BFS
		Queue<Long> toVisit = new LinkedList<Long>();
		Long firstState = new Long(initialEprimeMetaEnv);
		toVisit.add(firstState);
		ArrayList<Long> discovered = new ArrayList<Long>();

		while (!toVisit.isEmpty()) {
			Long actualInMetaEnv = toVisit.remove();
			if (!discovered.contains(actualInMetaEnv)) {
				discovered.add(actualInMetaEnv);
				
				Long stateInEPrimeWithOriginalId = updEnvGenerator.mapStateToValuationState(statesMapping.get(actualInMetaEnv));
				ArrayList<Boolean> valuation = new ArrayList<Boolean>(updEnvGenerator.getNewValuation(stateInEPrimeWithOriginalId));
				valuation.add(true); // beginUpdate is true in E'
				valuation.add(isTrueStop(metaEnv, actualInMetaEnv)); 
				valuation.add(IsTrueStart(metaEnv, actualInMetaEnv));
				valuation.add(true); // reconfigure is true in E'
				resultantValuation.put(actualInMetaEnv, valuation);
				
				for (Pair<String, Long> action_toStateInMetaEnv : metaEnv.getTransitions(actualInMetaEnv,MTS.TransitionType.REQUIRED)) {

					toVisit.addAll(nextStatesToVisit(actualInMetaEnv, action_toStateInMetaEnv, env, statesMapping));
				}
				
			}
		}
		
	}
	

	private static Long findNewEnvInitialState(MTS<Long, String> anyEnv) {
		
		Long beginUpdateState = null;
		Long initialState = null;
		for (Pair<String, Long> transition: anyEnv.getTransitions(anyEnv.getInitialState(), MTS.TransitionType.REQUIRED)) {
			
			if(transition.getFirst().equals(UpdateConstants.BEGIN_UPDATE)){
				beginUpdateState = transition.getSecond();
				break;
			}
		}
		
		for (Pair<String, Long> transition: anyEnv.getTransitions(beginUpdateState, MTS.TransitionType.REQUIRED)) {
			
			if(transition.getFirst().equals(UpdateConstants.RECONFIGURE)){
				initialState = transition.getSecond();
				break;
			}
		}
		
		return initialState;
		
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
	
	private static void makeOldActionsUncontrollable(Set<String> controllableActions, HashSet<Long> eParallelCStates, MTS<Long, String> safetyEnv) {
		for (Long state : eParallelCStates) 
		{
			List<Pair<String, Long>> toBeChanged = new ArrayList<Pair<String, Long>>();
			for (Pair<String, Long> action_toState : safetyEnv.getTransitions(state, TransitionType.REQUIRED)) {
				if (controllableActions.contains(action_toState.getFirst())) {
					toBeChanged.add(action_toState);
				}
			}
			for (Pair<String, Long> action_toState : toBeChanged) {
				String action = action_toState.getFirst();
				Long toState = action_toState.getSecond();
				safetyEnv.removeRequired(state, action, toState);
				String actionWithOld = action + UpdateConstants.OLD_LABEL;
				safetyEnv.addAction(actionWithOld);
				safetyEnv.addRequired(state, actionWithOld, toState);
			}
		}
		// add all .old accions to MTS so as to avoid problems while parallel composition
		// I think is useless
		for (String action : controllableActions) {
			if (UpdatingControllersUtils.isNotUpdateAction(action)) {
				safetyEnv.addAction(action + UpdateConstants.OLD_LABEL);
			}
		}
		
	}

	private static Assumptions<Set<Long>> formulasToAssumptions(Set<Set<Long>> states, List<Formula> formulas, FluentStateValuation<Set<Long>> valuation) {

		Assumptions<Set<Long>> assumptions = new Assumptions<Set<Long>>();
		for (Formula formula : formulas) {
			Assume<Set<Long>> assume = new Assume<Set<Long>>();
			for (Set<Long> state : states) {
				valuation.setActualState(state);
				if (formula.evaluate(valuation)) {
					assume.addState(state);
				}
			}
			if (assume.isEmpty()) {
				Logger.getAnonymousLogger().warning("There is no state satisfying formula:" + formula);
			}
			assumptions.addAssume(assume);
		}

		if (assumptions.isEmpty()) {
			Assume<Set<Long>> trueAssume = new Assume<Set<Long>>();
			trueAssume.addStates(states);
			assumptions.addAssume(trueAssume);
		}

		return assumptions;
	}

	private static Guarantees<Set<Long>> formulasToGuarantees(Set<Set<Long>> states, List<Formula> formulas, FluentStateValuation<Set<Long>> valuation) {

		Guarantees<Set<Long>> guarantees = new Guarantees<Set<Long>>();
		for (Formula formula : formulas) {
			Guarantee<Set<Long>> guarantee = new Guarantee<Set<Long>>();
			for (Set<Long> state : states) {
				valuation.setActualState(state);
				if (formula.evaluate(valuation)) {
					guarantee.addState(state);
				}
			}
			if (guarantee.isEmpty()) {
				Logger.getAnonymousLogger().warning("There is no state satisfying formula:" + formula);
			}
			guarantees.addGuarantee(guarantee);
		}

		if (guarantees.isEmpty()) {
			Guarantee<Set<Long>> trueAssume = new Guarantee<Set<Long>>();
			trueAssume.addStates(states);
			guarantees.addGuarantee(trueAssume);
		}

		return guarantees;
	}

	
}

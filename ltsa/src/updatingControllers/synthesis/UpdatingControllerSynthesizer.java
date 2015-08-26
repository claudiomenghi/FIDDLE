package updatingControllers.synthesis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import lts.CompactState;
import lts.LTSOutput;
import updatingControllers.structures.UpdatingControllerCompositeState;
import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.FluentUtils;
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
	public static void generateController(UpdatingControllerCompositeState uccs, LTSOutput output) {
		Set<String> contActions = uccs.getControllableActions();

		// set environment
		MTS<Long, String> oldC = uccs.getOldController();
		MTS<Long, String> oldE = uccs.getOldEnvironment();
		MTS<Long, String> hatE = uccs.getHatEnvironment();
		MTS<Long, String> newE = uccs.getNewEnvironment();
		List<Fluent> propositions = uccs.getUpdProperties();
		UpdatingEnvironmentGenerator updEnvGenerator = new UpdatingEnvironmentGenerator(oldC, oldE, hatE, newE, propositions);
		MTS<Long, String> environment = updEnvGenerator.generateEnvironment(contActions, output);
		uccs.setUpdateEnvironment(environment);

		CompactState env = MTSToAutomataConverter.getInstance().convert(environment, "UPD_CONT_ENVIRONMENT", false);

		Vector<CompactState> machines = new Vector<CompactState>();
//		machines.addAll(CompositionExpression.preProcessSafetyReqs(uccs.getNewGoalDef(), output));

		// add to safetyGoals .old actions
//		UpdatingControllerHandler updateHandler = new UpdatingControllerHandler();
//		Vector<CompactState> withOldActionsMachines = updateHandler.addOldTransitionsToSafetyMachines(machines, contActions);

		machines.add(env);

		uccs.setMachines(machines);

		if (!uccs.debugModeOn() && uccs.getCheckTrace().isEmpty()){
			// set liveness goal
//			uccs.goal = uccs.getNewGoalGR();
//			uccs.makeController = true;

//			TransitionSystemDispatcher.parallelComposition(uccs, output);
			solveControlProblem(environment, uccs.getNewGoalGR(), uccs.getNewGoalDef(), propositions);
			
		} else {
			if (!uccs.debugModeOn()){
				//removed support for debugging
				//updateHandler.checkMappingValue(uccs.getCheckTrace(), output);
			}
		}
	}



	private static void solveControlProblem(MTS<Long, String> environment, GRControllerGoal<String> goal, ControllerGoalDefinition controllerGoalDefinition, List<Fluent> propositions) {
		
		environment = ControllerUtils.removeTopStates(environment, UpdatingControllersUtils.UPDATE_FLUENTS);
		
		ArrayList<Fluent> allPropositions = new ArrayList<Fluent>(UpdatingControllersUtils.UPDATE_FLUENTS);
		allPropositions.addAll(propositions);
		
		FluentUtils fluentUtils = FluentUtils.getInstance();
		FluentStateValuation<Long> valuation = fluentUtils.buildValuation(environment, allPropositions);
		
		
		GRGame<Long> game;
	
		game = new GRGameBuilder<Long, String>().buildGRGameFrom(environment, goal);
		GRRankSystem<Long> system = new GRRankSystem<Long>(game.getStates(), game.getGoal().getGuarantees(), game.getGoal().getAssumptions(), game.getGoal().getFailures());
		PerfectInfoGRGameSolver solver = new PerfectInfoGRGameSolver<Long>(game, system);
		
	
	
		solver.solveGame();
		//ojo que el estado del environment puede tener problemas.  
		if (solver.isWinning(environment.getInitialState())) {
			Strategy<Long, Integer> strategy = solver.buildStrategy();
			//TODO refactor permissive
			GRGameSolver<Long> grSolver = (GRGameSolver<Long>) solver;
			Set<Pair<StrategyState<Long, Integer>, StrategyState<Long, Integer>>> worseRank = grSolver.getWorseRank();
			MTS<StrategyState<Long, Integer>, String> result = GameStrategyToMTSBuilder.getInstance().buildMTSFrom(environment, strategy, worseRank, null);
		
			result.removeUnreachableStates();
		}
	}

}

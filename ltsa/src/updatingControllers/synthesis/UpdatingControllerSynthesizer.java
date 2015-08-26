package updatingControllers.synthesis;

import java.util.List;
import java.util.Set;
import java.util.Vector;

import lts.CompactState;
import lts.CompositionExpression;
import lts.LTSOutput;
import updatingControllers.structures.UpdatingControllerCompositeState;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ar.dc.uba.model.condition.Fluent;
import dispatcher.TransitionSystemDispatcher;

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
		List<Fluent> properties = uccs.getUpdProperties();
		UpdatingEnvironmentGenerator updEnvGenerator = new UpdatingEnvironmentGenerator(oldC, oldE, hatE, newE, properties);
		MTS<Long, String> environment = updEnvGenerator.generateEnvironment(contActions, output);
		uccs.setUpdateEnvironment(environment);

		CompactState env = MTSToAutomataConverter.getInstance().convert(environment, "UPD_CONT_ENVIRONMENT", false);

		// set safety goals
		Vector<CompactState> machines = new Vector<CompactState>();
//		machines.addAll(CompositionExpression.preProcessSafetyReqs(uccs.getNewGoalDef(), output));

		// add to safetyGoals .old actions
		UpdatingControllerHandler updateHandler = new UpdatingControllerHandler();
		Vector<CompactState> withOldActionsMachines = updateHandler.addOldTransitionsToSafetyMachines(machines, contActions);

		withOldActionsMachines.add(env);

		uccs.setMachines(withOldActionsMachines);

		if (!uccs.debugModeOn() && uccs.getCheckTrace().isEmpty()){
			// set liveness goal
			uccs.goal = uccs.getNewGoalGR();
			uccs.makeController = true;

			TransitionSystemDispatcher.parallelComposition(uccs, output);
		} else {
			if (!uccs.debugModeOn()){
				//removed support for debugging
				//				updateHandler.checkMappingValue(uccs.getCheckTrace(), output);
			}
		}
	}
}

package controller;

import java.io.IOException;
import java.util.Set;
import java.util.Vector;

import lts.CompactState;
import lts.CompositeState;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ui.StandardOutput;
import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.MTS.TransitionType;
import ac.ic.doc.mtstools.model.impl.LTSAdapter;
import ac.ic.doc.mtstools.model.impl.LTSSimulationSemantics;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import controller.model.PerfectInfoOppositeGRControlProblem;
import controller.model.ReportingPerfectInfoStateSpaceCuttingGRControlProblem;
import controller.model.gr.GRControllerGoal;

public class OppositeGR1GameSolverTests {

	@DataProvider(name = "oppositeGameTest")
	public Object[][] oppositeSafeGameParameters() {
		return new Object[][] {
				{
					"../ltsa/dist/examples/OppositeGR1/Tests/Base_cases.lts",
					"CASE0",
					"EXP0",
					"C0",
					"G1",
					"../ltsa/dist/examples/OppositeGR1/Tests/Base_cases.CASE0.result" },
				{
					"../ltsa/dist/examples/OppositeGR1/Tests/Base_cases.lts",
					"CASE1",
					"EXP1",
					"C1",
					"G1",
					"../ltsa/dist/examples/OppositeGR1/Tests/Base_cases.CASE1.result" },
				{
					"../ltsa/dist/examples/OppositeGR1/Tests/Base_cases.lts",
					"CASE2",
					"EXP2",
					"C2",
					"G1",
					"../ltsa/dist/examples/OppositeGR1/Tests/Base_cases.CASE2.result" },
				{
					"../ltsa/dist/examples/OppositeGR1/Tests/Base_cases.lts",
					"CASE3",
					"EXP3",
					"C3",
					"G1",
					"../ltsa/dist/examples/OppositeGR1/Tests/Base_cases.CASE3.result" },					
						
		};
	}

	@Test(dataProvider = "oppositeGameTest")
	private void OppositeGR1GameTest(String filename, String mutName,
			String expName, String controllerName, String controllerGoalName,
			String resultFileName) throws Exception, IOException {
		// TODO: check expected throwing null and gamesolver not adding states
		// to winning
		// TODO: this part is not consistent with our theory, but since we're
		// loading our process and losing part of the alphabet
		// we need it to keep the evaluation working, next step should be to
		// extend the process alphabet to the one defined whithin the lts file
		LTS<Long, String> mut = getMut(filename, mutName, controllerName,
				controllerGoalName, resultFileName);
		
		if(mut == null)
			Assert.assertTrue(false, "mut was null");
		LTS<Long, String> exp = getExpected(filename, expName);
		LTSTestHelper.getInstance().removeUnusedActionsFromLTS(mut);
		LTSTestHelper.getInstance().removeUnusedActionsFromLTS(exp);
		MTSAdapter<Long, String> mutAdapter = new MTSAdapter<Long, String>(mut);
		MTSAdapter<Long, String> expAdapter = new MTSAdapter<Long, String>(exp);
		LTSSimulationSemantics simulationSemantics = new LTSSimulationSemantics();
		Assert.assertTrue(simulationSemantics.isARefinement(mutAdapter,
				expAdapter));
		Assert.assertTrue(simulationSemantics.isARefinement(expAdapter,
				mutAdapter));
	}

	protected LTS<Long, String> getMut(String filename, String mutName,
			String controllerName, String controllerGoalName,
			String resultFileName) {
		try {
			LTS<Long, String> env = LTSTestHelper.getInstance().getLTSFromFile(
					filename, mutName);

			//IMPORTANT: to use the assumptionsAsGoals version of the goal 
			GRControllerGoal<String> grControllerGoal = LTSTestHelper
					.getInstance().getGRControllerGoalFromFile(filename,
							controllerName).cloneWithAssumptionsAsGoals();

			Set<LTS<Long, String>> safetyReqs = LTSTestHelper.getInstance()
					.getSafetyProcessesFromFile(filename, controllerName,
							controllerGoalName);

			if (safetyReqs == null)
				System.out.println("no safetyRequirement");
			else {
				CompositeState safetyComposite = new CompositeState(
						"safetyComposite", new Vector<CompactState>());
				for (LTS<Long, String> safetyReq : safetyReqs) {
					safetyComposite.machines.add(MTSToAutomataConverter
							.getInstance().convert(new MTSAdapter(safetyReq),
									"safety_automata"));
				}
				safetyComposite.machines.add(MTSToAutomataConverter
						.getInstance().convert(new MTSAdapter(env),
								"environment"));
				safetyComposite.compose(new StandardOutput());
				env = new LTSAdapter<Long, String>(AutomataToMTSConverter
						.getInstance().convert(safetyComposite.composition),
						TransitionType.POSSIBLE);
			}

			PerfectInfoOppositeGRControlProblem<Long, String> grControlProblem = new PerfectInfoOppositeGRControlProblem<Long, String>(
					env, grControllerGoal);
			
			return grControlProblem.solve();

		} catch (Exception e) {
			System.out.print(e.getStackTrace().toString());
			return null;
		}

	}

	protected LTS<Long, String> getExpected(String filename, String expectedName) {
		try {
			return LTSTestHelper.getInstance().getLTSFromFile(filename,
					expectedName);
		} catch (Exception e) {
			System.out.print(e.getStackTrace().toString());
			return null;
		}
	}
}

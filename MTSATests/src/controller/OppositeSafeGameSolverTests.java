package controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.Vector;

import lts.CompactState;
import lts.CompositeState;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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
import controller.model.ReportingPerfectInfoStateSpaceCuttingGRControlProblem;
import controller.model.gr.GRControllerGoal;

public class OppositeSafeGameSolverTests {

	public static String reportString = "";
	public static boolean firstTime = true;
	
	   @BeforeClass
	   public void setUp() throws Exception {
		   reportString = "";
		   firstTime = true;
	   }
	 
	   @AfterClass
	   public void tearDown() throws Exception {
			PrintWriter writer;
			try {
				writer = new PrintWriter("../ltsa/dist/examples/SafetyCuts/Results/general.result", "UTF-8");
				writer.print(reportString);
				writer.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}		   
	      reportString = "";
	      firstTime = false;
	   }	
	
	@DataProvider(name = "oppositeGameTest")
	public Object[][] oppositeSafeGameParameters() {
		return new Object[][] {
				/* */
				{
					"Augmented Biscotti",
					"../ltsa/dist/examples/SafetyCuts/Tests/Augmented-Biscotti.lts",
					"BISCOTTI", "EXP", "C", "G1",
					"../ltsa/dist/examples/SafetyCuts/Results/Augmented-Biscotti.result" } ,				
				{
					"section-4.2-biscotti",
					"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-4.2-biscotti.lts",
					"ENV",
					"EXP",
					"C",
					"G1",
					"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-4.2-biscotti.result" },

				{
						"section-4.64-bookstore",
						"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-4.64-bookstore.lts",
						"ENV",
						"EXP",
						"C",
						"G1",
						"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-4.64-bookstore.result" },
						
				{
						"section-4.63-autonomousvehicles",
						"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-4.63-autonomousvehicles.lts",
						"PLANT",
						"EXP",
						"C",
						"G1",
						"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-4.63-autonomousvehicles.result" },
				{
						"section-4.62-payandship",
						"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-4.62-payandship.lts",
						"ENV",
						"EXP",
						"C",
						"G1",
						"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-4.62-payandship.result" },
				
				{
						"section-4.1-travelagency",
						"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-4.1-travelagency.lts",
						"ENV",
						"EXP",
						"C",
						"G1",
						"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-4.1-travelagency.result" },
				{
						"section-3.83-bookstore",
						"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-3.83-bookstore.lts",
						"ENV",
						"EXP",
						"C",
						"G1",
						"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-3.83-bookstore.result" },
				{
						"section-3.82-purchaseanddelivery",
						"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-3.82-purchaseanddelivery.lts",
						"ENV",
						"EXP",
						"C",
						"G1",
						"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-3.82-purchaseanddelivery.result" },
				{
						"section-3.84-productioncell-small",
						"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-3.84-productioncell-small.lts",
						"ENV",
						"EXP",
						"C",
						"Objective",
						"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-3.84-productioncell-small.result" },
				{
						"section-3.81-autonomousvehicles",
						"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-3.81-autonomousvehicles.lts",
						"ENV",
						"EXP",
						"C",
						"G1",
						"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-3.81-autonomousvehicles.result" },

				{ 
						"arbiter",
						"../ltsa/dist/examples/SafetyCuts/Tests/arbiter.lts",
						"PROCS", "EXP", "C", "PETERSON",
						"../ltsa/dist/examples/SafetyCuts/Results/arbiter.result" },
				{
						"prod_cell_variant",
						"../ltsa/dist/examples/SafetyCuts/Tests/prod_cell_variant.lts",
						"Plant", "EXP", "C", "G1",
						"../ltsa/dist/examples/SafetyCuts/Results/prod_cell_variant.result" },
				{ 
						"tomAndJerryV12",
						"../ltsa/dist/examples/SafetyCuts/Tests/TomAndJerryV12.lts",
						"Tom_Jerry", "EXP", "C", "G1",
						"../ltsa/dist/examples/SafetyCuts/Results/TomAndJerryV12.result" },

						{
							"section-4.71-productioncell",
							"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-4.71-productioncell.lts",
							"ENV",
							"EXP",
							"C",
							"G1",
							"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-4.71-productioncell.result" },
						{
						"section-4.61-productioncell",
						"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-4.61-productioncell.lts",
						"Plant",
						"EXP",
						"C",
						"G1",
						"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-4.61-productioncell.result" },

						{
						"section-3.1-productioncell",
						"../ltsa/dist/examples/SafetyCuts/Tests/dIppolito/section-3.1-productioncell.lts",
						"ENV",
						"EXP",
						"C",
						"Objective",
						"../ltsa/dist/examples/SafetyCuts/Results/dIppolito/section-3.1-productioncell.result" },
						
		};
	}

	private LTS<Long, String> exp;
	private String testName;
	@Test(dataProvider = "oppositeGameTest")
	private void OppositeSafeGameTest(String testName, String filename, String mutName,
			String expName, String controllerName, String controllerGoalName,
			String resultFileName) throws Exception, IOException {
		this.testName = testName;
		
		exp = getExpected(filename, expName);		

		LTS<Long, String> mut = getMut(filename, mutName, controllerName,
				controllerGoalName, resultFileName);
		Assert.assertTrue(mut != null);
		LTSTestHelper.getInstance().removeUnusedActionsFromLTS(exp);
		LTSTestHelper.getInstance().removeUnusedActionsFromLTS(mut);

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
			GRControllerGoal<String> grControllerGoal = LTSTestHelper
					.getInstance().getGRControllerGoalFromFile(filename,
							controllerName);

			Set<LTS<Long, String>> safetyReqs = LTSTestHelper.getInstance()
					.getSafetyProcessesFromFile(filename, controllerName,
							controllerGoalName);

			CompositeState safetyComposite = new CompositeState(
					"safetyComposite", new Vector<CompactState>());
			if (safetyReqs == null)
				System.out.println("no safetyRequirement");
			else
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
			
			LTS<Long, String> envForController = new LTSAdapter<Long, String>(AutomataToMTSConverter
					.getInstance().convert(safetyComposite.composition),
					TransitionType.POSSIBLE); 

			ReportingPerfectInfoStateSpaceCuttingGRControlProblem<Long, String> grControlProblem = new ReportingPerfectInfoStateSpaceCuttingGRControlProblem<Long, String>(
					testName, env, envForController, grControllerGoal, resultFileName, exp);

			LTS<Long, String> result = grControlProblem.solve();
			reportString += grControlProblem.getReport(firstTime, "&", "|", "\\\\\n");
			if(firstTime)
				firstTime = false;
			return result;
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

package MTSATests.controller;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.Vector;

import ltsa.ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.ltsinput.LTSInput;
import ltsa.ui.FileInput;
import ltsa.ui.StandardOutput;
import ltsa.updatingControllers.UpdateConstants;
import ltsa.updatingControllers.structures.graph.UpdateGraph;
import ltsa.updatingControllers.structures.graph.UpdateNode;
import ltsa.updatingControllers.structures.graph.UpdateTransition;
import ltsa.updatingControllers.synthesis.UpdateGraphGenerator;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import FSP2MTS.ac.ic.doc.mtstools.test.util.TestLTSOuput;
import MTSAClient.ac.ic.doc.mtsa.MTSCompiler;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSTools.ac.ic.doc.mtstools.model.impl.LTSSimulationSemantics;

public class UpdatingControllersTests {

	public static final String ext = ".lts";
	public static final String env = "_env";

	@DataProvider(name = "environmentTest")
	public Object[][] environmentParameters() {
		return new Object[][]{{"/ltsa/dist/examples/ControllerUpdate/Tests/adding_actions_test"},
				{"/ltsa/dist/examples/ControllerUpdate/Tests/removing_actions_test"},
				{"/ltsa/dist/examples/ControllerUpdate/Tests/UAV_test"},
				{"/ltsa/dist/examples/ControllerUpdate/Tests/productionCell_test"},
				{"/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-PowerPlant-Ghezzi2013"},
				{"/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-Railcab-Ghezzi2012"},
				{"/ltsa/dist/examples/ControllerUpdate/2015-FSE-ProductionCell"},
				{"/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
				{"/ltsa/dist/examples/ControllerUpdate/2015-FSE-Workflow"}};
	}

	@Test(dataProvider = "environmentTest")
	private void environmentTest(String filename) throws Exception, IOException {
		MTS<Long, String> expectedEnv = MTSCompiler.getInstance().compileMTS("UpdEnv", getFile(filename + env + ext));
		CompositeState compiled = MTSCompiler.getInstance().compileCompositeState("UpdCont", getFile(filename + ext));

		// Get the environment for update E_u
		MTS<Long, String> obtainedEnv = null;
		for (LabelledTransitionSystem machine : compiled.getMachines()) {
			if (machine.getName().equals("UPD_CONT_ENVIRONMENT")) {
				obtainedEnv = AutomataToMTSConverter.getInstance().convert(machine);
				break;
			}
		}
		if (obtainedEnv == null) {
			System.out.println("No updating environment after compiling");
			fail();
		}

		// Add the .old actions from the obtained environment to the expected in the alphabet
		Set<String> actionsFromCompiled = obtainedEnv.getActions();
		for (String action : actionsFromCompiled) {
			if (action.contains(UpdateConstants.OLD_LABEL) && !expectedEnv.getActions().contains(action)) {
				expectedEnv.addAction(action);
			}
		}

		LTSSimulationSemantics simulationSemantics = new LTSSimulationSemantics();

		assertTrue(simulationSemantics.isARefinement(expectedEnv, obtainedEnv));
		assertTrue(simulationSemantics.isARefinement(obtainedEnv, expectedEnv));
	}

	@DataProvider(name = "checkUpdatingFormula")
	public Object[][] controllersComparisonParameters() {

		return new Object[][]{

				{"/ltsa/dist/examples/ControllerUpdate/Tests/adding_actions_test"},
				{"/ltsa/dist/examples/ControllerUpdate/Tests/removing_actions_test"},
				{"/ltsa/dist/examples/ControllerUpdate/Tests/UAV_test"},
				{"/ltsa/dist/examples/ControllerUpdate/Tests/productionCell_test"},
//				{"/ltsa/dist/examples/ControllerUpdate/Tests/GsubsetjG'_test"},
				{"/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-PowerPlant-Ghezzi2013"},
				{"/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-Railcab-Ghezzi2012"},
				//			    {"/ltsa/dist/examples/ControllerUpdate/2015-FSE-ProductionCell"},
				//			    {"/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
				{"/ltsa/dist/examples/ControllerUpdate/2015-FSE-Workflow"},
				{"/ltsa/dist/examples/ControllerUpdate/2015-Tokyo/colorBallsTransfer"}};
	}

	@Test(dataProvider = "checkUpdatingFormula")
	// This test check that the file in targetA can compose UpdCont and check some formulas defined there
	private void checkUpdatingFormula(String filename) throws Exception {

		CompositeState compiled = MTSCompiler.getInstance().compileCompositeState("UpdCont", getFile(filename + ext));
		LTSOutput output = new TestLTSOuput();

		CompositeState ltlProperty = AssertDefinition.compile(output, "TEST_FINAL_FORMULA");
		compiled.setErrorTrace(new ArrayList<String>());

		Vector<LabelledTransitionSystem> machines = new Vector<LabelledTransitionSystem>();
		machines.add(compiled.getComposition());
		CompositeState cs = new CompositeState(compiled.getName(), machines);
		cs.checkLTL(output, ltlProperty);

		assertTrue(cs.getErrorTrace() == null || cs.getErrorTrace().isEmpty());

	}

	@DataProvider(name = "graphGenerator")
	public Object[][] graphGeneratorFiles() {
		return new Object[][]{{"/ltsa/dist/examples/ControllerUpdate/2015-Tokyo/colorBallsTransfer"}};
	}

	@Test(dataProvider = "graphGenerator")
	public void graphGenerator_producesCorrectGraph(String fileName) throws IOException, ParseException {
		LTSInput input = new FileInput(getFile(fileName + ext));
		LTSCompiler compiler = MTSCompiler.getInstance().getCompiler(input);
		compiler.compile();
		UpdateGraph updateGraph = UpdateGraphGenerator.generateGraph("Graph", compiler, new StandardOutput());

		assertNotNull(updateGraph.getInitialState());
		assertNotNull(updateGraph.getInitialState().getController$environment());
		assertEquals(6, updateGraph.getEdgeCount());
		assertEquals(3, updateGraph.getVertexCount());
		Collection<UpdateNode> vertices = updateGraph.getVertices();
		assertTrue(vertices.contains(updateGraph.getInitialState()));
		for (UpdateNode vertice : vertices) {
			assertNotNull(vertice.getGoalName());
		}
		Collection<UpdateTransition> edges = updateGraph.getEdges();
		for (UpdateTransition edge : edges) {
			assertNotNull(edge.getUpdateController());
		}
	}

	//	@DataProvider(name = "controllersComparisonTest")
	//	public Object[][] controllersComparisonParameters() {
	//
	//		return new Object[][] {
	//
	//		// GC: Ghezzi; TC: endProcedureWhileRunning; OR: GC or TC;
	//		{"GC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-PowerPlant-Ghezzi2013"},
	//		{"OC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-PowerPlant-Ghezzi2013"},
	//		{"TC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-PowerPlant-Ghezzi2013"},
	//		{"TC", "OC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-PowerPlant-Ghezzi2013"},
	//		{"GC", "OC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-PowerPlant-Ghezzi2013"},
	//
	//		// GC: Ghezzi; TC: updateBeforeStopOldSpec; OC: GC or TC;
	//		{"GC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-Railcab-Ghezzi2012"},
	//		{"OC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-Railcab-Ghezzi2012"},
	//		{"TC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-Railcab-Ghezzi2012"},
	//		{"TC", "OC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-Railcab-Ghezzi2012"},
	//		{"GC", "OC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-Railcab-Ghezzi2012"},
	//		{"OC", "GC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-Railcab-Ghezzi2012"},
	//		{"GC", "TC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-Railcab-Ghezzi2012"},
	//		{"TC", "GC", "/ltsa/dist/examples/ControllerUpdate/Ghezzi/2015-FSE-Railcab-Ghezzi2012"},
	//
	//		// LC: LowBeforeUpdate; NGFC: updateBeforeStopOldSpec; NSC: NoScanWhileUpdate; NMC: NoMoveWhileUpdate; GC:
	// Ghezzi
	//		{"LC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"TrueC", "LC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"NGFC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"NSC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"NMC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"GC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"NGFC", "LC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"NSC", "LC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"NMC", "LC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"GC", "LC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"NGFC", "NSC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"NGFC", "NMC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"NGFC", "GC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//		{"NMC", "NSC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-Wildlife"},
	//
	//		// LC: LowBeforeUpdate; NGFC: updateBeforeStopOldSpec; NSC: NoScanWhileUpdate; NMC: NoMoveWhileUpdate; GC:
	// Ghezzi
	//		{"LC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"TrueC", "LC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"NGFC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"NSC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"TrueC", "NSC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"NMC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"NGFC", "LC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"NSC", "LC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"LC", "NSC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"NMC", "LC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"NGFC", "NSC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"NGFC", "NMC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//		{"NMC", "NSC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-WildlifeV2"},
	//
	//		// EC: EmptyController; UFC: updateBeforeStopOldSpec; KOC: KeepOldSpec; CASAP: changeASAP
	//		{"UFC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-ProductionCell"},
	//		{"KOC", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-ProductionCell"},
	//		{"CASAP", "TrueC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-ProductionCell"},
	//		{"UFC", "KOC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-ProductionCell"},
	//		{"UFC", "CASAP", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-ProductionCell"},
	//		{"KOC", "CASAP", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-ProductionCell"},
	//		{"CASAP", "KOC", "/ltsa/dist/examples/ControllerUpdate/2015-FSE-ProductionCell"}
	//
	//		};
	//	}

	//	@Test(dataProvider = "controllersComparisonTest")
	//	// This test check that targetA \subeset targetB
	//	private void controllerComparisonTest(String targetA, String targetB, String filename) throws Exception{
	//		MTS<Long, String> compiledA = MTSCompiler.getInstance().compileMTS(targetA, getFile(filename));
	//		MTS<Long, String> compiledB = MTSCompiler.getInstance().compileMTS(targetB, getFile(filename));
	//
	//		LTSSimulationSemantics simulationSemantics = new LTSSimulationSemantics();
	//		boolean result = simulationSemantics.isARefinement(compiledB, compiledA);
	//
	//		assertTrue(result);
	//
	//	}

	private File getFile(String testFileName) throws IOException {
		String ltsaPath = new File(".").getCanonicalPath();
		File testFile = new File(ltsaPath + testFileName);
		return testFile;
	}
}
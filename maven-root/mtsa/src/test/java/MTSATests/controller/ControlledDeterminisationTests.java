package MTSATests.controller;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import MTSAClient.ac.ic.doc.mtsa.MTSCompiler;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import static MTSTools.ac.ic.doc.mtstools.model.MTS.TransitionType.*;
import MTSTools.ac.ic.doc.mtstools.model.impl.LTSSimulationSemantics;
import MTSTools.ac.ic.doc.mtstools.model.predicates.IsDeterministicMTSPredicate;
import static org.junit.Assert.*;

public class ControlledDeterminisationTests {
	
	
	@Test
	public void testSimpleControlledDet() throws Exception {
		String testPath = "controlled-det-1.lts";
		MTS<Long, String> compileMTS = MTSCompiler.getInstance().compileMTS("DET", getFile(testPath));
		IsDeterministicMTSPredicate<Long, String> pred = new IsDeterministicMTSPredicate<Long, String>(POSSIBLE);
		assertTrue(pred.evaluate(compileMTS));
		assertTrue(compileMTS.getActions().contains("-1"));
		System.out.println(compileMTS.getStates().size());
	}
	
	@Test
	public void testServicesExampleForLegalControllers() throws Exception {
		String testPath = "2013-services-legal.lts";
		MTS<Long, String> compileMTS = MTSCompiler.getInstance().compileMTS("DET_PLANT", getFile(testPath));
		IsDeterministicMTSPredicate<Long, String> pred = new IsDeterministicMTSPredicate<Long, String>(POSSIBLE);
		assertTrue(pred.evaluate(compileMTS));
		assertTrue(compileMTS.getActions().contains("-1"));
	}

	public File getFile(String testFileName) throws IOException {
		String ltsaPath = new File("../").getCanonicalPath();
		File testFile = new File(ltsaPath + "/ltsa/dist/examples/" + testFileName);
		return testFile;
	}
}

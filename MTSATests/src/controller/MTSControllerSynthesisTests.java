package controller;

import static ac.ic.doc.mtsa.MTSCompiler.getInstance;
import static org.junit.Assert.assertTrue;
import lts.CompositeState;

import org.junit.Test;

import control.util.ControlConstants;

import util.TestConstants;
import ac.ic.doc.mtstools.test.util.TestLTSOuput;

public class MTSControllerSynthesisTests {
	
	@Test
	public void testAll() throws Exception {
		TestLTSOuput testLTSOuput = new TestLTSOuput();
		CompositeState model = getInstance().compileCompositeState("C_All", TestConstants.fileFrom("mts-control.lts"), testLTSOuput);
		String name = model.getComposition().name;
		assertTrue("There is no controller for C", !name.contains(ControlConstants.NO_CONTROLLER));
		assertTrue("The answer is not All", testLTSOuput.toString().contains("All implementations of C_All can be controlled"));
	}

	@Test
	public void testSome() throws Exception {
		TestLTSOuput testLTSOuput = new TestLTSOuput();
		CompositeState model = getInstance().compileCompositeState("C_Some", TestConstants.fileFrom("mts-control.lts"), testLTSOuput);
		String name = model.getComposition().name;
		assertTrue("There is no controller for C", !name.contains(ControlConstants.NO_CONTROLLER));
		assertTrue("The answer is not Some", testLTSOuput.toString().contains("Some implementations of C_Some can be controlled and some cannot."));
	}
	
	@Test
	public void testNone() throws Exception {
		TestLTSOuput testLTSOuput = new TestLTSOuput();
		CompositeState model = getInstance().compileCompositeState("C_None", TestConstants.fileFrom("mts-control.lts"), testLTSOuput);
		String name = model.getComposition().name;
		assertTrue("There is no controller for C", name.contains(ControlConstants.NO_CONTROLLER));
		assertTrue("The answer is not All", testLTSOuput.toString().contains("No implementation of C_None can be controlled"));
	}

}

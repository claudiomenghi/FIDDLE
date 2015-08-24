package ac.ic.doc.mtstools.model.impl;

import lts.CompositeState;
import lts.Symbol;
import ac.ic.doc.mtstools.test.util.LTSATestUtils;
import ac.ic.doc.mtstools.test.util.MTSTestBase;
import dispatcher.TransitionSystemDispatcher;

public class PlusCAAndCROperationTest extends MTSTestBase {
	
	public void testComponeConError() throws Exception {
		String sourceString = "A = (a?->A).\r\n" + 
				"		B = (a->B).\r\n" + 
				"		||AB = (A +cr B).\r\n";
		CompositeState composite = LTSATestUtils.buildAutomataFromSource(sourceString);
		composite.setCompositionType(Symbol.PLUS_CA);
		TransitionSystemDispatcher.applyComposition(composite, ltsOutput);
		
	}


}

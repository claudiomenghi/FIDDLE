package ac.ic.doc.mtstools.model.impl;

import lts.CompactState;
import lts.CompositeState;
import lts.util.MTSUtils;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.test.util.LTSATestUtils;
import ac.ic.doc.mtstools.test.util.MTSTestBase;
import ac.ic.doc.mtstools.test.util.TestLTSOuput;
import ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;

public class MTSPropertyTest extends MTSTestBase { 

	public void testPropertyA() throws Exception {
		String sourceString = "property A = (a->A)+{b,c}.\r\n";
		CompositeState buildCompositeState = LTSATestUtils.buildCompositeState(sourceString, new TestLTSOuput());
		MTS<?, ?> mts = AutomataToMTSConverter.getInstance().convert((CompactState) buildCompositeState.getComposition());
		System.out.println(mts);
		boolean isMTS = MTSUtils.isMTSRepresentation(buildCompositeState);
		assertFalse(isMTS);
	}
	
}

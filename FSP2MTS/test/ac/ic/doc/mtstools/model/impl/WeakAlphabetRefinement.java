package ac.ic.doc.mtstools.model.impl;

import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.SemanticType;
import ac.ic.doc.mtstools.test.util.MTSTestBase;
import ac.ic.doc.mtstools.test.util.MTSTestUtils;


public class WeakAlphabetRefinement extends MTSTestBase {
	
	
	public void testWeakAlphabet1() throws Exception {
		String sourceString = "M = ( m -> STOP | l -> STOP | _tau? -> l -> STOP)\\{_tau}.\r\n";
		MTS<Long, String> mtsM = MTSTestUtils.buildMTSFrom(sourceString, ltsOutput);
		sourceString = "I = ( m -> STOP | a -> l -> STOP).\r\n";
		MTS<Long, String> mtsI = MTSTestUtils.buildMTSFrom(sourceString, ltsOutput);
		
		assertTrue(SemanticType.WEAK_ALPHABET.getRefinement().isARefinement(mtsM, mtsI));
		
	}

}

package ac.ic.doc.mtstools.model.impl;

import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.MTS.TransitionType;
import ac.ic.doc.mtstools.test.util.MTSTestBase;
import ac.ic.doc.mtstools.test.util.MTSTestUtils;
import ac.ic.doc.mtstools.test.util.TestLTSOuput;

public class RemoveUnreachableStatesTest extends MTSTestBase {

	public void testRemoveStates() throws Exception {
		String sourceString = "pessimistic A = (a->b->A | a?->A2), A2 = (c->v->g->A | m->A2).\r\n"; 
		MTS<Long, String> mts = MTSTestUtils.buildMTSFrom(sourceString, new TestLTSOuput());
		assertNotNull(mts);
		assertFalse(mts.getTransitions(mts.getInitialState(), TransitionType.POSSIBLE).isEmpty());
	}
}

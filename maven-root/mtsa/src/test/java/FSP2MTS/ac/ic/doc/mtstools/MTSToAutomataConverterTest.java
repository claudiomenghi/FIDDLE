package FSP2MTS.ac.ic.doc.mtstools;

import ltsa.ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.util.MTSUtils;
import ltsa.lts.util.collections.MyList;
import FSP2MTS.ac.ic.doc.mtstools.test.util.MTSTestBase;
import MTSTools.ac.ic.doc.mtstools.model.MTS;

public class MTSToAutomataConverterTest extends MTSTestBase {


	public void testMTSUnMTSAEstadoPorA() throws Exception {
		MTS<Long, String> mts = this.buildBasicMTS();
		
		MTSToAutomataConverter converter = new MTSToAutomataConverter();
		LabelledTransitionSystem automata = (LabelledTransitionSystem) converter.convert(mts, "dipi");
		assertNotNull(automata);
		
		String[] alphabet = automata.getAlphabet();
		assertEquals(alphabet[0], TAU_ACTION);
		assertEquals(alphabet[1], TAU_MAYBE_ACTION);
		assertEquals(alphabet[2], A_ACTION);
		assertEquals(alphabet[3], A_MAYBE_ACTION);
		
		MyList transitions = automata.getTransitions(MTSUtils.encode(0));
		assertEquals(0, transitions.getFrom());
		assertEquals(A_ACTION, alphabet[transitions.getAction()]);
		assertEquals(0, MTSUtils.decode(transitions.getTo()));
		
		assertEquals(1, automata.getStates().length);
	}

}

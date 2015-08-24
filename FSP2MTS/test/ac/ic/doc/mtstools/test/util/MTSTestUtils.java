package ac.ic.doc.mtstools.test.util;

import java.util.Collections;
import java.util.Set;

import junit.framework.TestCase;
import lts.CompactState;
import lts.CompositeState;
import lts.LTSOutput;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.impl.WeakSemantics;
import ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;

public class MTSTestUtils extends TestCase {
	
	public static <State, Action> void areEquivalent(MTS<State, Action> originalMTS, MTS<State, Action> finalMTS, Set<Action> invisibleActions) {
		WeakSemantics weak = new WeakSemantics(invisibleActions);
		WeakSemantics strong = new WeakSemantics(Collections.EMPTY_SET);
		
		assertTrue("WeakSemantics: The first argument is not a refinement of the second.", weak.isARefinement(originalMTS, finalMTS));
		assertTrue("WeakSemantics: The second argument is not a refinement of the first.", weak.isARefinement(finalMTS, originalMTS));

		assertTrue("StrongSemantics: The first argument is not a refinement of the second.", strong.isARefinement(originalMTS, finalMTS));
		assertTrue("StrongSemantics: The second argument is not a refinement of the first.", strong.isARefinement(finalMTS, originalMTS));
	}

	public static MTS<Long, String> buildMTSFrom(String sourceString, LTSOutput ltsOuput) throws Exception {
		CompositeState compositeOriginal = LTSATestUtils.buildCompositeState(sourceString, ltsOuput);
		MTS<Long, String> originalMTS = AutomataToMTSConverter.getInstance().convert((CompactState) compositeOriginal.getComposition());
		return originalMTS;
	}

}

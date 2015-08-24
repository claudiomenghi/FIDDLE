package lts.chart.util;

import ac.ic.doc.mtstools.model.MTS;
import ar.dc.uba.model.language.Symbol;
import ar.dc.uba.model.lsc.TriggeredScenario;
import ar.dc.uba.model.structure.SynthesizedState;
import ar.dc.uba.synthesis.SynthesisVisitor;

/**
 * Encapsulates the call to the synthesis algorithm.
 * 
 * @author gsibay
 *
 */
public class MTSSynthesiserFacade {

	static private MTSSynthesiserFacade instance = new MTSSynthesiserFacade();
	static public MTSSynthesiserFacade getInstance() { return instance; }

	private MTSSynthesiserFacade() {}

	public MTS<SynthesizedState, Symbol> synthesise(TriggeredScenario triggeredScenario) {
		return triggeredScenario.acceptSynthesisVisitor(SynthesisVisitor.getInstance());
	}
	
}

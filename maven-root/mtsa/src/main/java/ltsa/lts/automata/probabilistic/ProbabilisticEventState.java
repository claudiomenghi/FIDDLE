package ltsa.lts.automata.probabilistic;

import java.math.BigDecimal;

import ltsa.lts.automata.lts.state.LTSTransitionList;

public class ProbabilisticEventState extends LTSTransitionList {
	private int bundle;
	private BigDecimal prob;
	private LTSTransitionList probTr;
	// probTr: probabilistic transitions on same bundle
	// nondet: other bundles with same event
	// list: all other bundles with other events
	
	public void setProbTr(LTSTransitionList probTr) {
		this.probTr = probTr;
	}

	public LTSTransitionList getProbTr() {
		return probTr;
	}

	public ProbabilisticEventState(int e, int i) {
		super(e, i);
	}

	public ProbabilisticEventState(int e, int i, BigDecimal prob, int bundle) {
		super(e, i);
		this.prob= prob;
		this.bundle= bundle;
	}
	
	public void setBundle(int bundle) {
		this.bundle= bundle;
	}
	
	public void setProbability(BigDecimal prob) {
		this.prob= prob;
	}
	
	public int getBundle() {
		return bundle;
	}
	
	public BigDecimal getProbability() {
		return prob;
	}
	
	public LTSTransitionList getBundleTransitions()
	{
	  return probTr;
	}
}

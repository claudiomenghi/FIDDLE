package ltsa.lts.util.collections;

import java.math.BigDecimal;


public class MyProbListEntry extends MyTransitionListEntry {
	int bundle;
	BigDecimal prob;

	MyProbListEntry(int from, byte[] to, int action) {
		super(from, to, action);
	}

	MyProbListEntry(int from, byte[] to, int action, int bundle, BigDecimal prob) {
		super(from, to, action);
		this.bundle= bundle;
		this.prob= prob;
	}
}
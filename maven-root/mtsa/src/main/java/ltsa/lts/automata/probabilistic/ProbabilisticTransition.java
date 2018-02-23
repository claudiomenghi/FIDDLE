package ltsa.lts.automata.probabilistic;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ltsa.lts.automata.automaton.transition.Transition;
import ltsa.lts.parser.Symbol;
import ltsa.lts.util.LTSUtils;

public class ProbabilisticTransition extends Transition {

	public static final int NO_BUNDLE = -1;
	public static final int BUNDLE_ERROR = -2;

	private static int lastProbBundle = NO_BUNDLE;
	
	private int probBundle;
	private BigDecimal prob;


	private static Map<String, Map<String, Integer>> composedBundles = new HashMap<>();

	ProbabilisticTransition(int from, Symbol event, int to) {
		super(from, event, to);
	}

	ProbabilisticTransition(int from, Symbol event, int to, BigDecimal prob) {
		super(from, event, to);
		this.prob = prob;
	}

	public ProbabilisticTransition(int from, Symbol event, int to, BigDecimal prob,
			int probBundle) {
		super(from, event, to);
		this.prob = prob;
		this.probBundle = probBundle;
	}

	public void setProbability(BigDecimal prob) {
		this.prob = prob;
	}

	public BigDecimal getProbability() {
		return prob;
	}

	public void setBundle(int bundle) {
		this.probBundle = bundle;
	}

	public int getBundle() {
		return probBundle;
	}

	public static int getLastProbBundle() {
		return lastProbBundle;
	}

	public static int getNextProbBundle() {
		return ++lastProbBundle;
	}

	public static void setLastProbBundle(int bundle) {
		lastProbBundle = bundle;
	}

	@Override
	public String toString() {
		return Integer.toString(this.getFrom()) + " --{" + this.getEvent()
				+ "," + probBundle + "} " + prob.toString() + "--> "
				+ this.getTo();
	}

	public static int composeBundles(int[] sourceStates, int[] bundles) {
		int[] sortedBundles = LTSUtils.myclone(bundles);
		int composedBundle;
		Arrays.sort(sortedBundles);
		String bundlesStr = Arrays.toString(sortedBundles);
		String stateStr = Arrays.toString(sourceStates);
		Map<String, Integer> bundlesForStates = composedBundles.get(stateStr);
		if (bundlesForStates == null) {
			bundlesForStates = new HashMap<>();
			composedBundles.put(stateStr, bundlesForStates);
		}

		Integer bundle = bundlesForStates.get(bundlesStr);
		if (bundle == null) {
			composedBundle = ++lastProbBundle;
			bundlesForStates.put(bundlesStr, composedBundle);
		} else {
			composedBundle = bundle.intValue();
		}

		return composedBundle;
	}

	public static BigDecimal composeProbs(BigDecimal[] probs) {
		BigDecimal prob = BigDecimal.ONE;
		for (BigDecimal srcProb : probs) {
			if (srcProb != null) {
				// may be null if internal / not shared
				prob = prob.multiply(srcProb);
			}
		}

		return prob;
	}
	
	public int getProbBundle() {
		return probBundle;
	}
}

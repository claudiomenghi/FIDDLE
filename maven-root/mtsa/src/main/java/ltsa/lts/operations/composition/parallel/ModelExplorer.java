package ltsa.lts.operations.composition.parallel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import ltsa.lts.Diagnostics;
import ltsa.lts.animator.ModelExplorerContext;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.probabilistic.ProbabilisticEventState;
import ltsa.lts.automata.probabilistic.ProbabilisticTransition;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.util.LTSUtils;

public class ModelExplorer {
	public static boolean isEND(ModelExplorerContext ctx, int[] state) {
		if (!ctx.canTerminate) {
			return false;
		}
		for (int i = 0; i < ctx.Nmach; i++) {
			if (ctx.sm[i].getEndOfSequenceIndex() != state[i]) {
				return false;
			}
		}
		return true;
	}

	public static boolean isFinal(ModelExplorerContext ctx, int[] state) {
		for (int i = 0; i < ctx.Nmach; i++) {
			if (ctx.sm[i].getFinalStateIndexes().contains(state[i])) {
				return true;
			}
		}
		return false;
	}

	public static List<int[]> eligibleTransitions(ModelExplorerContext ctx,
			int[] state) {

		// first check number of possible successors
		for (int i = 0; i < ctx.Nmach; i++) {
			// for each machine
			// cloning the state
			int[] next = LTSUtils.myclone(state);
			
			if (ctx.sm[i].getFinalStateIndexes().contains(state[i])) {

				LabelledTransitionSystem lts=ctx.sm[i];
				
				// find the transition labeled with the end action
				LTSTransitionList head = lts.getTransitions(state[i]);
				while (head != null && head.getEvent() != ctx.endEvent) {
					head = head.getList();
				}
				if (head != null) {
//					System.out.println("MATCHED EVENT INDEX: "+actionMap.get(LTLf2LTS.endSymbol.getValue()));
					next[head.getMachine()] = head.getNext();
					next[ctx.Nmach] = head.getEvent();
				}
				ArrayList<int[]> nextTransitions = new ArrayList<>();
				nextTransitions.add(next);
				return nextTransitions;
			}
		}

		List<int[]> asteriskTransitions = null;
		if (ctx.partial != null) {
			if (ctx.asteriskEvent > 0
					&& LTSTransitionList
							.hasEvent(
									ctx.sm[ctx.Nmach - 1].getStates()[state[ctx.Nmach - 1]],
									ctx.asteriskEvent)) {
				// do nothing
			} else {
				List<int[]> parTrans = ctx.partial.transitions(state);
				if (parTrans != null)
					return parTrans;
			}
		}
		int[] ac = LTSUtils.myclone(ctx.actionCount);
		LTSTransitionList[] trs = new LTSTransitionList[ctx.actionCount.length];
		int nsucc = 0; // count of feasible successor transitions
		int highs = 0; // eligible high priority actions

		// first check number of possible successors
		for (int i = 0; i < ctx.Nmach; i++) {
			// for each machine
			LTSTransitionList p = ctx.sm[i].getStates()[state[i]];
			while (p != null) {
				// for each transition
				LTSTransitionList tr = p;
				tr.setPath(trs[tr.getEvent()]);
				trs[tr.getEvent()] = tr;
				ac[tr.getEvent()]--;
				if (tr.getEvent() != 0 && ac[tr.getEvent()] == 0) {
					nsucc++;
					// ignoring tau, this transition is possible
					// bugfix 26-mar-04 to handle asterisk + priority
					if (ctx.highAction != null
							&& ctx.highAction.get(tr.getEvent())
							&& tr.getEvent() != ctx.asteriskEvent) {
						++highs;
					}
				}
				p = p.getList();
			}
		}
		if (nsucc == 0 && trs[0] == null) {
			return null; // DEADLOCK - no successor states
		}
		int actionNo = 1;
		List<int[]> transitions = new ArrayList<>(8);
		// we include tau if it is high priority or its low and there are no
		// high priority transitions
		if (trs[0] != null) {
			boolean highTau = (ctx.highAction != null && ctx.highAction.get(0));
			if (highTau || highs == 0)
				ModelExplorer.computeTauTransitions(ctx, trs[0], state,
						transitions);
			if (highTau) {
				++highs;
			}
		}
		while (nsucc > 0) { // do this loop once per successor state
			nsucc--;
			// find number of action
			while (ac[actionNo] > 0)
				actionNo++;

			// if (!ModelExplorer.isFinal(ctx, state)
			// || (ctx.actionName[actionNo].equals(LTLf2LTS.endSymbol
			// .getValue()) && ctx.actionName[actionNo]
			// .equals(LTLf2LTS.initSymbol.getValue()))) {

			// now compute the state for this action if not excluded tock
			if (highs <= 0 || ctx.highAction.get(actionNo)
					|| actionNo == ctx.acceptEvent) {
				LTSTransitionList tr = trs[actionNo];
				boolean nonDeterministic = false;
				boolean probabilistic = false;
				while (tr != null) { // test for non determinism or
										// probabilistic transitions
					// tr.path holds all tr (EventStates) that synchronise
					// to make this transition
					if (tr.getNondet() != null) {
						nonDeterministic = true;
					}
					if (tr instanceof ProbabilisticEventState) {
						ProbabilisticEventState probTr = (ProbabilisticEventState) tr;
						// if (probTr.probTr != null)
						if (probTr.getProbability().compareTo(BigDecimal.ZERO) != 0
								|| probTr.getProbTr() != null)
							probabilistic = true;
					}

					if (nonDeterministic || probabilistic)
						break;

					tr = tr.getPath();
				}
				tr = trs[actionNo];
				if (!nonDeterministic && !probabilistic) {
					int[] next = LTSUtils.myclone(state);
					next[ctx.Nmach] = actionNo;
					while (tr != null) {
						next[tr.getMachine()] = tr.getNext();
						tr = tr.getPath();
					}
					if (actionNo != ctx.asteriskEvent)
						transitions.add(next);
					else {
						asteriskTransitions = new ArrayList<>(1);
						asteriskTransitions.add(next);
					}
				} else if (!probabilistic) {
					if (actionNo != ctx.asteriskEvent) {
						computeNonDetTransitions(ctx, tr, state, transitions);
					} else {
						computeNonDetTransitions(ctx, tr, state,
								asteriskTransitions = new ArrayList<>(4));
					}
				} else {
					// probabilistic (and possibly nondeterministic)
					// transitions
					computeProbabilisticTransitions(ctx, state, tr, state,
							null, null, transitions);
				}

			}
			++ac[actionNo];

		}
		if (ctx.asteriskEvent < 0)
			return transitions;
		else
			return mergeAsterisk(ctx, transitions, asteriskTransitions);

	}

	private static void computeTauTransitions(ModelExplorerContext ctx,
			LTSTransitionList first, int[] state, List<int[]> v) {
		LTSTransitionList down = first;
		while (down != null) {
			LTSTransitionList across = down;
			while (across != null) {
				int[] next = LTSUtils.myclone(state);
				next[across.getMachine()] = across.getNext();
				next[ctx.Nmach] = 0; // tau
				v.add(next);
				across = across.getNondet();
			}
			down = down.getPath();
		}
	}

	private static void computeNonENDTransitions(ModelExplorerContext ctx,
			LTSTransitionList first, int[] state, List<int[]> v) {
		LTSTransitionList tr = first;
		while (tr != null) {
			int[] next = LTSUtils.myclone(state);
			next[tr.getMachine()] = tr.getNext();
			if (first.getPath() != null) {
				// generate the tree of possible nondet combinations.
				computeNonDetTransitions(ctx, first.getPath(), next, v);
			} else {
				next[ctx.Nmach] = first.getEvent();
				v.add(next);
			}
			tr = tr.getNondet();
		}
	}

	private static void computeNonDetTransitions(ModelExplorerContext ctx,
			LTSTransitionList first, int[] state, List<int[]> v) {
		LTSTransitionList tr = first;
		while (tr != null) {
			int[] next = LTSUtils.myclone(state);
			next[tr.getMachine()] = tr.getNext();
			if (first.getPath() != null) {
				// generate the tree of possible nondet combinations.
				computeNonDetTransitions(ctx, first.getPath(), next, v);
			} else {
				next[ctx.Nmach] = first.getEvent();
				v.add(next);
			}
			tr = tr.getNondet();
		}
	}

	/**
	 * Computes probabilistic (and potentially nondeterministic) transitions
	 * 
	 * @param ctx
	 * @param first
	 *            holds the first EventState on the chain of possible
	 *            synchronisations
	 * @param state
	 * @param v
	 */
	private static void computeProbabilisticTransitions(
			ModelExplorerContext ctx, int[] sourceStates,
			LTSTransitionList first, int[] state, int[] bundles,
			BigDecimal[] probs, List v) {

		if (!(first instanceof ProbabilisticEventState)) {
			Diagnostics.fatal("Probabilistic transitions expected");
		}

		ProbabilisticEventState tr = (ProbabilisticEventState) first;
		// outer loop nondeterministic, inner loop probabilistic
		while (tr != null) {
			ProbabilisticEventState probTr = tr;
			while (probTr != null) {
				if (bundles == null) {
					bundles = new int[ctx.Nmach];
					for (int i = 0; i < ctx.Nmach; i++)
						bundles[i] = ProbabilisticTransition.NO_BUNDLE;
				}

				if (probs == null)
					probs = new BigDecimal[ctx.Nmach];

				int[] next = LTSUtils.myclone(state);
				next[tr.getMachine()] = probTr.getNext();
				bundles[tr.getMachine()] = probTr.getBundle();
				probs[tr.getMachine()] = probTr.getProbability();

				if (first.getPath() != null) {
					computeProbabilisticTransitions(ctx, sourceStates,
							first.getPath(), next, bundles, probs, v);
				} else {
					// last machine
					ProbabilisticEligibleTransition probNext = new ProbabilisticEligibleTransition();
					next[ctx.Nmach] = first.getEvent();
					probNext.next = next;
					probNext.sourceBundles = LTSUtils.myclone(bundles);
					probNext.sourceProbs = LTSUtils.myclone(probs);
					probNext.sourceStates = LTSUtils.myclone(sourceStates);
					v.add(probNext);
				}

				probTr = (ProbabilisticEventState) probTr.getProbTr();
			}

			tr = (ProbabilisticEventState) tr.getNondet();
		}
	}

	private static List<int[]> mergeAsterisk(ModelExplorerContext ctx,
			List<int[]> transitions, List<int[]> asteriskTransitions) {
		if (transitions == null || asteriskTransitions == null)
			return transitions;
		if (transitions.size() == 0)
			return null;
		int[] asteriskTransition;
		if (asteriskTransitions.size() == 1) {
			asteriskTransition = (int[]) asteriskTransitions.get(0);
			Iterator<int[]> e = transitions.iterator();
			while (e.hasNext()) {
				int[] next = (int[]) e.next();
				if (!ctx.visible.get(next[ctx.Nmach])) {
					// fragile, assumes property is last machine!!
					next[ctx.Nmach - 1] = asteriskTransition[ctx.Nmach - 1];
				}
			}
			return transitions;
		} else {
			Iterator<int[]> a = asteriskTransitions.iterator();
			List<int[]> newTransitions = new ArrayList<>();
			while (a.hasNext()) {
				asteriskTransition = (int[]) a.next();
				Iterator<int[]> e = transitions.iterator();
				while (e.hasNext()) {
					int[] next = (int[]) e.next();
					if (!ctx.visible.get(next[ctx.Nmach])) {
						// fragile, assumes property is last machine!!
						next[ctx.Nmach - 1] = asteriskTransition[ctx.Nmach - 1];
					}
					newTransitions.add(LTSUtils.myclone(next));
				}
			}
			return newTransitions;
		}
	}
}

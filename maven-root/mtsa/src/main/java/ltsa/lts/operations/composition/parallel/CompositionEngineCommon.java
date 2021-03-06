package ltsa.lts.operations.composition.parallel;

import java.util.List;
import java.util.Iterator;

import ltsa.lts.Diagnostics;
import ltsa.lts.animator.ModelExplorerContext;
import ltsa.lts.animator.StateCodec;
import ltsa.lts.automata.probabilistic.ProbabilisticTransition;
import ltsa.lts.util.collections.StateMap;

public class CompositionEngineCommon {

	private CompositionEngineCommon() {

	}

	/**
	 * for lack of a better name :(
	 */
	public static void processTransitions(StateCodec coder,
			ModelExplorerContext ctx, List<int[]> transitions,
			StateMap analysed, int[] state) {
		Iterator<int[]> e = transitions.iterator();
		while (e.hasNext()) {

			Object next = e.next();
			int[] nextState = null;
			ProbabilisticEligibleTransition nextProbTr = null;
			if (next instanceof int[]) {
				nextState = (int[]) next;
			} else if (next instanceof ProbabilisticEligibleTransition) {
				nextProbTr = (ProbabilisticEligibleTransition) next;
				nextState = nextProbTr.next;
			} else {
				Diagnostics
						.fatal("Nondeterministic or probabilistic transition expected");
			}

			byte[] code = coder.encode(nextState);
			// TODO compTrans is only nondeterministic, need to create some
			// probabilistic transitions
			if (next instanceof int[]) {
				ctx.compTrans.add(ctx.stateCount - 1, code,
						nextState[ctx.Nmach]);
			} else {
				ctx.compTrans.add(ctx.stateCount - 1, code,
						nextState[ctx.Nmach], ProbabilisticTransition
								.composeBundles(nextProbTr.sourceStates,
										nextProbTr.sourceBundles),
						ProbabilisticTransition
								.composeProbs(nextProbTr.sourceProbs));
			}

			if (code == null) {
				int i = 0;
				while (nextState[i] >= 0) {
					i++;
				}

				ctx.violated[i] = true;
			} else if (!analysed.contains(code)) {
				analysed.add(code);
			}
		}

	}
}

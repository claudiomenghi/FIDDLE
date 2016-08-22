package ltsa.lts.automata.lts.state.factory;

import ltsa.lts.automata.automaton.transition.Transition;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.probabilistic.ProbabilisticEventState;
import ltsa.lts.automata.probabilistic.ProbabilisticTransition;

public class EventStateFactory {
	
	public static LTSTransitionList createEventState(int event, Transition t) {
		if (t instanceof ProbabilisticTransition) {
			ProbabilisticTransition pTr = (ProbabilisticTransition) t;
			return new ProbabilisticEventState(event, pTr.getTo(), pTr.getProbability(),
					pTr.getProbBundle());
		} else {
			return new LTSTransitionList(event, t.getTo());
		}
	}
}


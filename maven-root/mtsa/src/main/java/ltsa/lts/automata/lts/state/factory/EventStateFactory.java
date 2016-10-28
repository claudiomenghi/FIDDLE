package ltsa.lts.automata.lts.state.factory;

import ltsa.lts.automata.automaton.transition.Transition;
import ltsa.lts.automata.lts.state.LTSTransitionList;

public class EventStateFactory {

	public static LTSTransitionList createEventState(int event, Transition t) {

		return new LTSTransitionList(event, t.getTo());

	}
}

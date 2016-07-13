package ltsa.lts.lts;

import ltsa.lts.lts.EventState;
import ltsa.lts.lts.ProbabilisticEventState;
import ltsa.lts.lts.ProbabilisticTransition;
import ltsa.lts.lts.Transition;

public class EventStateFactory {
	
	public static EventState createEventState(int event, Transition t) {
		if (t instanceof ProbabilisticTransition) {
			ProbabilisticTransition pTr = (ProbabilisticTransition) t;
			return new ProbabilisticEventState(event, pTr.getTo(), pTr.getProbability(),
					pTr.getProbBundle());
		} else {
			return new EventState(event, t.getTo());
		}
	}
}


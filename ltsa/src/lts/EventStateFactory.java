package lts;

public class EventStateFactory {
	public static EventState createEventState(int event, Transition t) {
		if (t instanceof ProbabilisticTransition) {
			ProbabilisticTransition pTr= (ProbabilisticTransition) t;
			return new ProbabilisticEventState(event, pTr.to, pTr.prob, pTr.probBundle);
		} else {
			return new EventState(event, t.to);
		}
	}
}

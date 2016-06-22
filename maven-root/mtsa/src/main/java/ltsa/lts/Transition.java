package ltsa.lts;

import com.google.common.base.Preconditions;

/**
 * contains a transition of the LTS.
 * 
 *
 */
public class Transition {

	private final int from;
	private final int to;
	private final Symbol event;

	Transition(int from, Symbol event, int to) {
		Preconditions.checkNotNull(event,
				"The event to be considered cannot be null");
		this.from = from;
		this.to = to;
		this.event = event;
	}

	public int getFrom() {
		return from;
	}

	public int getTo() {
		return to;
	}

	public Symbol getEvent() {
		return event;
	}

	public String toString() {
		return "" + from + " " + event + " " + to;
	}
}

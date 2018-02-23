package ltsa.lts.automata.automaton.transition;

import ltsa.lts.parser.Symbol;

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

	/**
	 * creates a new transition
	 * 
	 * @param from
	 *            the source state
	 * @param event
	 *            the event that labels the transition
	 * @param to
	 *            the destination state
	 * @throws NullPointerException
	 *             if the event is null
	 */
	public Transition(int from, Symbol event, int to) {
		Preconditions.checkNotNull(event,
				"The event to be considered cannot be null");
		this.from = from;
		this.to = to;
		this.event = event;
	}

	public int getFrom() {
		return this.from;
	}

	public int getTo() {
		return this.to;
	}

	public Symbol getEvent() {
		return this.event;
	}

	@Override
	public String toString() {
		return this.from + " " + this.event + " " + this.to;
	}
}

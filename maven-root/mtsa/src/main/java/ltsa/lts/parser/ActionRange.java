package ltsa.lts.parser;

import java.util.Stack;

import ltsa.lts.Diagnostics;
import ltsa.lts.csp.Range;

/**
 * -- evaluate [low..high] labels
 */
public class ActionRange extends ActionLabels {

	Stack rlow;
	Stack rhigh;

	public ActionRange(Stack low, Stack high) {
		this.rlow = low;
		this.rhigh = high;
	}

	public ActionRange(Range r) {
		rlow = r.low;
		rhigh = r.high;
	}

	protected String computeName() {
		return String.valueOf(current);
	}

	protected int current, high, low;

	protected void initialise() {
		low = Expression.evaluate(rlow, locals, globals).intValue();
		high = Expression.evaluate(rhigh, locals, globals).intValue();
		if (low > high)
			Diagnostics.fatal("Range not defined", (Symbol) rlow.peek());
		current = low;
	}

	protected void next() {
		++current;
	}

	public boolean hasMoreNames() {
		return (current <= high);
	}

	protected ActionLabels make() {
		return new ActionRange(rlow, rhigh);
	}

}
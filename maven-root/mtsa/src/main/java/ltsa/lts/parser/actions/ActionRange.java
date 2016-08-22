package ltsa.lts.parser.actions;

import java.util.Stack;

import ltsa.lts.Diagnostics;
import ltsa.lts.csp.Range;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;

/**
 * -- evaluate [low..high] labels
 */
public class ActionRange extends ActionLabels {

	Stack<Symbol> rlow;
	Stack<Symbol> rhigh;

	protected int current, high, low;

	
	public ActionRange(Stack<Symbol> low, Stack<Symbol> high) {
		this.rlow = low;
		this.rhigh = high;
	}

	public ActionRange(Range r) {
		rlow = r.low;
		rhigh = r.high;
	}

	@Override
	protected String computeName() {
		return String.valueOf(current);
	}

	@Override
	protected void initialise() {
		low = Expression.evaluate(rlow, locals, globals).intValue();
		high = Expression.evaluate(rhigh, locals, globals).intValue();
		if (low > high)
			Diagnostics.fatal("Range not defined", (Symbol) rlow.peek());
		current = low;
	}

	@Override
	protected void next() {
		++current;
	}

	@Override
	public boolean hasMoreNames() {
		return (current <= high);
	}

	@Override
	protected ActionLabels make() {
		return new ActionRange(rlow, rhigh);
	}

}
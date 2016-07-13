package ltsa.lts.parser;

import java.util.Stack;

/**
 * -- evaluate [expr] labels
 */
public class ActionExpr extends ActionLabels {

	protected Stack expr;

	public ActionExpr(Stack expr) {
		this.expr = expr;
	}

	protected String computeName() {
		Value v = Expression.getValue(expr, locals, globals);
		return v.toString();
	}

	protected boolean consumed;

	protected void initialise() {
		consumed = false;
	}

	protected void next() {
		consumed = true;
	}

	public boolean hasMoreNames() {
		return !consumed;
	}

	protected ActionLabels make() {
		return new ActionExpr(expr);
	}

}
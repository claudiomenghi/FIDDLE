package ltsa.lts.parser;

import java.util.Stack;

/**
 * -- evaluate [expr] labels
 */
public class ActionExpr extends ActionLabels {

	protected Stack expr;

	protected boolean consumed;

	public ActionExpr(Stack expr) {
		this.expr = expr;
	}

	@Override
	protected String computeName() {
		Value v = Expression.getValue(expr, locals, globals);
		return v.toString();
	}

	@Override
	protected void initialise() {
		consumed = false;
	}

	@Override
	protected void next() {
		consumed = true;
	}

	@Override
	public boolean hasMoreNames() {
		return !consumed;
	}

	@Override
	protected ActionLabels make() {
		return new ActionExpr(expr);
	}

}
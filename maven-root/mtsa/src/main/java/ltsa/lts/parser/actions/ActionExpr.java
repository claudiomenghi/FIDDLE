package ltsa.lts.parser.actions;

import java.util.Stack;

import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;

/**
 * -- evaluate [expr] labels
 */
public class ActionExpr extends ActionLabels {

	protected Stack<Symbol> expr;

	protected boolean consumed;

	public ActionExpr(Stack<Symbol> expr) {
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
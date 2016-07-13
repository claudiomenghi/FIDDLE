package ltsa.lts.csp;

import java.util.Hashtable;
import java.util.Stack;

import ltsa.lts.lts.StateMachine;
import ltsa.lts.parser.ActionLabels;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;

/* ----------------------------------------------------------------------- */
public class ChoiceElement extends Declaration {
	private Stack<Symbol> guard;
	public ActionLabels action;
	public StateExpr stateExpr;

	public Stack<Symbol> getGuard() {
		return guard;
	}

	public void setGuard(Stack<Symbol> guard) {
		this.guard = guard;
	}

	private void add(int from, Hashtable locals, StateMachine m,
			ActionLabels action) {
		action.initContext(locals, m.getConstants());
		while (action.hasMoreNames()) {
			String s = action.nextName();
			Symbol e = new Symbol(Symbol.IDENTIFIER, s);
			if (!m.getAlphabet().contains(s))
				m.addEvent(s);
			stateExpr.endTransition(from, e, locals, m);
		}
		action.clearContext();
	}

	public void addTransition(int from, Hashtable locals, StateMachine m) {
		if (guard == null
				|| Expression.evaluate(guard, locals, m.getConstants())
						.intValue() != 0) {
			if (action != null) {
				add(from, locals, m, action);
			}
		}
	}

	public ChoiceElement myclone() {
		ChoiceElement ce = new ChoiceElement();
		ce.guard = guard;
		if (action != null)
			ce.action = action.myclone();
		if (stateExpr != null)
			ce.stateExpr = stateExpr.myclone();
		return ce;
	}
}
package ltsa.lts;

import java.util.Hashtable;
import java.util.Stack;


/* ----------------------------------------------------------------------- */
public class ChoiceElement extends Declaration {
	public Stack guard;
	public ActionLabels action;
	public StateExpr stateExpr;

	private void add(int from, Hashtable locals, StateMachine m,
			ActionLabels action) {
		action.initContext(locals, m.constants);
		while (action.hasMoreNames()) {
			String s = action.nextName();
			Symbol e = new Symbol(Symbol.IDENTIFIER, s);
			if (!m.getAlphabet().contains(s))
				m.addEvent(s);
			stateExpr.endTransition(from, e, locals, m);
		}
		action.clearContext();
	}

	private void add(int from, Hashtable locals, StateMachine m, String s) {
		Symbol e = new Symbol(Symbol.IDENTIFIER, s);
		if (!m.getAlphabet().contains(s))
			m.addEvent(s);
		stateExpr.endTransition(from, e, locals, m);
	}

	public void addTransition(int from, Hashtable locals, StateMachine m) {
		if (guard == null
				|| Expression.evaluate(guard, locals, m.constants).intValue() != 0) {
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
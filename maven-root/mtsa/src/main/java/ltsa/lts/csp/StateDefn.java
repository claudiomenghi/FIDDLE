package ltsa.lts.csp;

import java.util.Hashtable;

import ltsa.lts.Diagnostics;
import ltsa.lts.lts.StateMachine;
import ltsa.lts.lts.Transition;
import ltsa.lts.ltscomposition.CompactState;
import ltsa.lts.parser.ActionLabels;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;

/* ----------------------------------------------------------------------- */

public class StateDefn extends Declaration {
	public Symbol name;
	public boolean accept = false;
	public ActionLabels range; // use label with no name
	public StateExpr stateExpr;

	private void check_put(String s, StateMachine m) {
		if (m.getStates().contains(s)) {
			Diagnostics.fatal("duplicate definition -" + name, name);
		} else {
			m.addState(s);
		}
	}

	@Override
	public void explicitStates(StateMachine m) {
		if (range == null) {
			String s = name.toString();
			if (s.equals("STOP") || s.equals("ERROR") || s.equals("END"))
				Diagnostics.fatal("reserved local process name -" + name, name);
			check_put(s, m);
		} else {
			Hashtable locals = new Hashtable();
			range.initContext(locals, m.getConstants());
			while (range.hasMoreNames()) {
				check_put(name.toString() + "." + range.nextName(), m);
			}
			range.clearContext();
		}
	}

	private void crunchAlias(StateExpr st, String n, Hashtable locals,
			StateMachine m) {
		String s = st.evalName(locals, m);
		Integer i;
		if (m.getStates().contains(s)) {
			i = m.getStateIndex(s);
		} else {
			if (s.equals("STOP")) {
				m.addState("STOP");
				i = m.getStateIndex("STOP");
			} else if (s.equals("ERROR")) {
				m.addState("ERROR");
				i = m.getStateIndex("END");
			} else if (s.equals("END")) {
				m.addState("END");
				i = m.getStateIndex("END");
			} else {
				m.addState("ERROR");
				i = m.getStateIndex("END");
				Diagnostics.warning(s + " defined to be ERROR",
						"definition not found- " + s, st.name);
			}
		}
		CompactState mach = null;
		if (st.processes != null) {
			mach = st.makeInserts(locals, m);
		}
		if (mach != null) {
			m.preAddSequential(m.getStateIndex(n), i, mach);
		} else {
			m.getAliases().put(m.getStateIndex(n), i);
		}
	}

	@Override
	public void crunch(StateMachine m) {
		if (stateExpr.name == null && stateExpr.boolexpr == null)
			return;
		Hashtable locals = new Hashtable();
		if (range == null)
			crunchit(m, locals, stateExpr, name.toString());
		else {
			range.initContext(locals, m.getConstants());
			while (range.hasMoreNames()) {
				String s = "" + name + "." + range.nextName();
				crunchit(m, locals, stateExpr, s);
			}
			range.clearContext();
		}
	}

	private void crunchit(StateMachine m, Hashtable locals, StateExpr st,
			String s) {
		if (st.name != null)
			crunchAlias(st, s, locals, m);
		else if (st.boolexpr != null) {
			if (Expression.evaluate(st.boolexpr, locals, m.getConstants())
					.intValue() != 0)
				st = st.thenpart;
			else
				st = st.elsepart;
			if (st != null)
				crunchit(m, locals, st, s);
		}
	}

	@Override
	public void transition(StateMachine m) {
		if (stateExpr.name != null) {
			return;
		}
		// this is an alias definition
		Hashtable locals = new Hashtable();
		int from;
		if (range == null) {
			from = m.getStateIndex("" + name);
			stateExpr.firstTransition(from, locals, m);
			if (accept) {
				if (!m.getAlphabet().contains("@"))
					m.addEvent("@");
				Symbol e = new Symbol(Symbol.IDENTIFIER, "@");
				m.addTransition(new Transition(from, e, from));
			}
		} else {
			range.initContext(locals, m.getConstants());
			while (range.hasMoreNames()) {
				from = m.getStateIndex("" + name + "." + range.nextName());
				stateExpr.firstTransition(from, locals, m);
			}
			range.clearContext();
		}
	}

	public StateDefn myclone() {
		StateDefn sd = new StateDefn();
		sd.name = name;
		sd.accept = accept;
		if (range != null)
			sd.range = range.myclone();
		if (stateExpr != null)
			sd.stateExpr = stateExpr.myclone();
		return sd;
	}

}

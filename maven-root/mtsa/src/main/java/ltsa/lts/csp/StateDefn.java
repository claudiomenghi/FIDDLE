package ltsa.lts.csp;

import java.util.Hashtable;

import com.google.common.base.Preconditions;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.automaton.StateMachine;
import ltsa.lts.automata.automaton.transition.Transition;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.parser.actions.ActionLabels;

/* ----------------------------------------------------------------------- */

public class StateDefn extends Declaration {
	private final Symbol name;

	public boolean accept = false;
	public ActionLabels range; // use label with no name
	private StateExpr stateExpr;

	private boolean isFinal = false;

	private static final String ERRORCONST = "ERROR";

	public StateDefn(Symbol name) {
		Preconditions.checkNotNull(name, "The name cannot be null");
		this.name = name;
	}

	@Override
	public void explicitStates(StateMachine m) {
		if (range == null) {
			String s = name.toString();
			if ("STOP".equals(s) || ERRORCONST.equals(s) || "END".equals(s)) {
				Diagnostics.fatal("reserved local process name -" + name, name);
			}
			checkPut(s, m);
		} else {
			Hashtable<String, Value> locals = new Hashtable<>();
			range.initContext(locals, m.getConstants());
			while (range.hasMoreNames()) {
				checkPut(name.toString() + "." + range.nextName(), m);
			}
			range.clearContext();
		}
	}

	@Override
	public void crunch(StateMachine m) {
		if (stateExpr == null
				|| (stateExpr.name == null && stateExpr.boolexpr == null))
			return;
		Hashtable<String, Value> locals = new Hashtable<>();
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

	@Override
	public void transition(StateMachine machine) {
		if (stateExpr == null || stateExpr.name != null) {
			return;
		}
		// this is an alias definition
		Hashtable<String, Value> locals = new Hashtable<>();
		int from;
		if (range == null) {
			from = machine.getStateIndex("" + name);
			stateExpr.firstTransition(from, locals, machine);
			if (accept) {
				if (!machine.getAlphabet().contains("@")) {
					machine.addEvent("@");
				}
				Symbol e = new Symbol(Symbol.IDENTIFIER, "@");
				machine.addTransition(new Transition(from, e, from));
			}
		} else {
			range.initContext(locals, machine.getConstants());
			while (range.hasMoreNames()) {
				from = machine
						.getStateIndex("" + name + "." + range.nextName());
				stateExpr.firstTransition(from, locals, machine);
			}
			range.clearContext();
		}
	}

	public StateDefn myclone() {
		StateDefn sd = new StateDefn(name);
		sd.accept = accept;
		if (range != null)
			sd.range = range.myclone();
		if (stateExpr != null)
			sd.stateExpr = stateExpr.myclone();
		return sd;
	}

	private void checkPut(String s, StateMachine m) {
		if (m.getStates().contains(s)) {
			Diagnostics.fatal("duplicate definition -" + name, name);
		} else {
			m.addState(s);
			if (this.isFinal) {
				m.addFinalState(s);
			}
		}
	}

	private void crunchAlias(StateExpr st, String n,
			Hashtable<String, Value> locals, StateMachine m) {
		String s = st.evalName(locals, m);
		Integer i;
		if (m.getStates().contains(s)) {
			i = m.getStateIndex(s);
		} else {
			if (s.equals("STOP")) {
				m.addState("STOP");
				i = m.getStateIndex("STOP");
			} else if (ERRORCONST.equals(s)) {
				m.addState(ERRORCONST);
				i = m.getStateIndex(ERRORCONST);
			} else if (s.equals("END")) {
				m.addState("END");
				i = m.getStateIndex("END");
			} else {
				m.addState(ERRORCONST);
				i = m.getStateIndex(ERRORCONST);
				Diagnostics.warning(s + " defined to be ERROR",
						"definition not found- " + s, st.name);
			}
		}
		LabelledTransitionSystem mach = null;
		if (st.processes != null) {
			mach = st.makeInserts(locals, m);
		}
		if (mach != null) {
			m.preAddSequential(m.getStateIndex(n), i, mach);
		} else {
			m.getAliases().put(m.getStateIndex(n), i);
		}
	}

	private void crunchit(StateMachine m, Hashtable<String, Value> locals,
			StateExpr st, String s) {
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

	public Symbol getName() {
		return name;
	}

	public void setStateExpr(StateExpr stateExpr) {
		this.stateExpr = stateExpr;
	}

	protected StateExpr getStateExpr() {
		return stateExpr;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}
}

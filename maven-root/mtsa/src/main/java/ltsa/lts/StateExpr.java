package ltsa.lts;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

public class StateExpr extends Declaration {
	// if name !=null then no choices
	Vector<SeqProcessRef> processes;
	public Symbol name;
	public Vector expr; // vector of expressions stacks, one for each subscript
	public ActionLabels actions;
	public Vector<ChoiceElement> choices;
	public Stack boolexpr;
	public StateExpr thenpart;
	public StateExpr elsepart;

	public void addSeqProcessRef(SeqProcessRef sp) {
		if (processes == null)
			processes = new Vector<SeqProcessRef>();
		processes.addElement(sp);
	}

	public CompactState makeInserts(Hashtable locals, StateMachine m) {
		Vector<CompactState> seqs = new Vector<CompactState>();
		Enumeration<SeqProcessRef> e = processes.elements();
		while (e.hasMoreElements()) {
			SeqProcessRef sp = e.nextElement();
			CompactState mach = sp.instantiate(locals, m.constants);
			if (!mach.isEnd())
				seqs.addElement(mach);
		}
		if (seqs.size() > 0)
			return CompactState.sequentialCompose(seqs);
		return null;
	}

	public Integer instantiate(Integer to, Hashtable locals, StateMachine m) {
		if (processes == null)
			return to;
		CompactState seqmach = makeInserts(locals, m);
		if (seqmach == null)
			return to;
		Integer start = m.stateLabel.interval(seqmach.maxStates);
		seqmach.offsetSeq(start.intValue(), to.intValue());
		m.addSequential(start, seqmach);
		return start;
	}

	public void firstTransition(int from, Hashtable locals, StateMachine m) {
		if (boolexpr != null) {
			if (Expression.evaluate(boolexpr, locals, m.constants).intValue() != 0) {
				if (thenpart.name == null)
					thenpart.firstTransition(from, locals, m);
			} else {
				if (elsepart.name == null)
					elsepart.firstTransition(from, locals, m);
			}
		} else {
			addTransitions(from, locals, m);
		}
	}

	public void addTransitions(int from, Hashtable locals, StateMachine m) {
		if (actions != null) {
			actions.initContext(locals, m.constants);
			while (actions.hasMoreNames()) {
				actions.nextName();
				addTransition(from, locals, m);
			}
			actions.clearContext();
		} else {
			addTransition(from, locals, m);
		}
	}

	public void addTransition(int from, Hashtable locals, StateMachine m) {
		Enumeration<ChoiceElement> e = choices.elements();
		while (e.hasMoreElements()) {
			ChoiceElement d = e.nextElement();
			d.addTransition(from, locals, m);
		}
	}

	public void endProbabilisticTransition(int from, Symbol event,
			Hashtable locals, StateMachine m, BigDecimal prob, int bundle) {
		// TODO for now this is kept extremely simple. No conditions, no
		// anything
		// TODO this will fail for implicit ERROR states
		if (boolexpr != null) {
			if (Expression.evaluate(boolexpr, locals, m.constants).intValue() != 0)
				thenpart.endProbabilisticTransition(from, event, locals, m,
						prob, bundle);
			else
				elsepart.endProbabilisticTransition(from, event, locals, m,
						prob, bundle);
		} else {
			Integer to;
			if (name != null) {
				if (m.getStates().contains(evalName(locals, m))) {
					to = m.getStateIndex(evalName(locals, m));
				} else {
					if (evalName(locals, m).equals("STOP")) {
						m.addState("STOP");
						to = m.getStateIndex("STOP");

					} else if (evalName(locals, m).equals("ERROR")) {
						m.addState("ERROR");
						to = m.getStateIndex("ERROR");
					} else if (evalName(locals, m).equals("END")) {
						m.addState("END");
						to = m.getStateIndex("END");
					} else {
						m.addState("ERROR");
						to = m.getStateIndex("ERROR");
						Diagnostics.warning(evalName(locals, m)
								+ " defined to be ERROR",
								"definition not found- " + evalName(locals, m),
								name);
					}
				}
				to = instantiate(to, locals, m);
				// m.transitions.addElement(new
				// Transition(from,event,to.intValue()));
				m.addTransition(new ProbabilisticTransition(from, event, to
						.intValue(), prob, bundle));
			} else {
				to = m.stateLabel.label();
				m.addTransition(new ProbabilisticTransition(from, event, to
						.intValue(), prob, bundle));
				addTransition(to.intValue(), locals, m);
			}
		}
	}

	public void endTransition(int from, Symbol event, Hashtable locals,
			StateMachine m) {
		if (boolexpr != null) {
			if (Expression.evaluate(boolexpr, locals, m.constants).intValue() != 0)
				thenpart.endTransition(from, event, locals, m);
			else
				elsepart.endTransition(from, event, locals, m);
		} else {
			Integer to;
			if (name != null) {
				if (m.getStates().contains(evalName(locals, m))) {
					to = m.getStateIndex(evalName(locals, m));
				} else {
					if (evalName(locals, m).equals("STOP")) {
						m.addState("STOP");
						to = m.getStateIndex("STOP");
					} else if (evalName(locals, m).equals("ERROR")) {
						m.addState("ERROR");
						to = m.getStateIndex("ERROR");
					} else if (evalName(locals, m).equals("END")) {
						m.addState("END");
						to = m.getStateIndex("END");
					} else {
						m.addState(evalName(locals, m));
						to = m.getStateIndex(evalName(locals, m));
						Diagnostics.warning(evalName(locals, m)
								+ " defined to be ERROR",
								"definition not found- " + evalName(locals, m),
								name);
					}
				}
				to = instantiate(to, locals, m);
				m.addTransition(new Transition(from, event, to.intValue()));
			} else {
				to = m.stateLabel.label();
				m.addTransition(new Transition(from, event, to.intValue()));
				addTransitions(to.intValue(), locals, m);
			}
		}
	}

	public String evalName(Hashtable locals, StateMachine m) {
		if (expr == null)
			return name.toString();
		else {
			Enumeration e = expr.elements();
			String s = name.toString();
			while (e.hasMoreElements()) {
				Stack x = (Stack) e.nextElement();
				s = s + "." + Expression.getValue(x, locals, m.constants);
			}
			return s;
		}
	}

	public StateExpr myclone() {
		StateExpr se = new StateExpr();
		se.processes = processes;
		se.name = name;
		se.expr = expr; // expressions are cloned when used
		if (choices != null) {
			se.choices = new Vector<>();
			Enumeration<ChoiceElement> e = choices.elements();
			while (e.hasMoreElements())
				se.choices.addElement(e.nextElement().myclone());
		}
		se.boolexpr = boolexpr;
		if (thenpart != null)
			se.thenpart = thenpart.myclone();
		if (elsepart != null)
			se.elsepart = elsepart.myclone();
		return se;
	}

}
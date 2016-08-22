package ltsa.lts.csp;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.automaton.StateMachine;
import ltsa.lts.automata.automaton.transition.Transition;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.probabilistic.ProbabilisticTransition;
import ltsa.lts.operations.composition.sequential.SequentialMergeEngine;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.parser.actions.ActionLabels;

public class StateExpr extends Declaration {
	// if name !=null then no choices
	Vector<SeqProcessRef> processes;
	public Symbol name;
	public Vector<Stack<Symbol>> expr; // vector of expressions stacks, one for
										// each subscript
	public ActionLabels actions;
	public Vector<ChoiceElement> choices;
	public Stack<Symbol> boolexpr;
	public StateExpr thenpart;
	public StateExpr elsepart;

	public void addSeqProcessRef(SeqProcessRef sp) {
		if (processes == null) {
			processes = new Vector<>();
		}
		processes.addElement(sp);
	}

	public LabelledTransitionSystem makeInserts(Hashtable<String, Value> locals,
			StateMachine m) {
		Vector<LabelledTransitionSystem> seqs = new Vector<>();
		Enumeration<SeqProcessRef> e = processes.elements();
		while (e.hasMoreElements()) {
			SeqProcessRef sp = e.nextElement();
			LabelledTransitionSystem mach = sp.instantiate(locals, m.getConstants());
			if (!mach.isEnd()) {
				seqs.addElement(mach);
			}
		}
		if (seqs.size() > 0) {
			return new SequentialMergeEngine().apply(seqs);
		}
		return null;
	}

	private Integer instantiate(Integer to, Hashtable<String, Value> locals,
			StateMachine m) {
		if (processes == null)
			return to;
		LabelledTransitionSystem seqmach = makeInserts(locals, m);
		if (seqmach == null)
			return to;
		Integer start = m.getStateLabel().interval(seqmach.getMaxStates());
		seqmach.offsetSeq(start.intValue(), to.intValue());
		m.addSequential(start, seqmach);
		return start;
	}

	public void firstTransition(int from, Hashtable<String, Value> locals,
			StateMachine stateMachine) {
		if (boolexpr != null) {
			if (Expression.evaluate(boolexpr, locals,
					stateMachine.getConstants()).intValue() != 0) {
				if (thenpart.name == null)
					thenpart.firstTransition(from, locals, stateMachine);
			} else {
				if (elsepart.name == null)
					elsepart.firstTransition(from, locals, stateMachine);
			}
		} else {
			addTransitions(from, locals, stateMachine);
		}
	}

	private void addTransitions(int from, Hashtable<String, Value> locals,
			StateMachine stateMachine) {
		if (actions != null) {
			actions.initContext(locals, stateMachine.getConstants());
			while (actions.hasMoreNames()) {
				actions.nextName();
				addTransition(from, locals, stateMachine);
			}
			actions.clearContext();
		} else {
			addTransition(from, locals, stateMachine);
		}
	}

	private void addTransition(int from, Hashtable<String, Value> locals,
			StateMachine m) {
		Enumeration<ChoiceElement> e = choices.elements();
		while (e.hasMoreElements()) {
			ChoiceElement d = e.nextElement();
			d.addTransition(from, locals, m);
		}
	}

	public void endProbabilisticTransition(int from, Symbol event,
			Hashtable<String, Value> locals, StateMachine m, BigDecimal prob,
			int bundle) {
		// TODO for now this is kept extremely simple. No conditions, no
		// anything
		// TODO this will fail for implicit ERROR states
		if (boolexpr != null) {
			if (Expression.evaluate(boolexpr, locals, m.getConstants())
					.intValue() != 0)
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
				to = m.getStateLabel().label();
				m.addTransition(new ProbabilisticTransition(from, event, to
						.intValue(), prob, bundle));
				addTransition(to.intValue(), locals, m);
			}
		}
	}

	public void endTransition(int from, Symbol event,
			Hashtable<String, Value> locals, StateMachine m) {
		if (boolexpr != null) {
			if (Expression.evaluate(boolexpr, locals, m.getConstants())
					.intValue() != 0)
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
				to = m.getStateLabel().label();
				m.addTransition(new Transition(from, event, to.intValue()));
				addTransitions(to.intValue(), locals, m);
			}
		}
	}

	public String evalName(Hashtable<String, Value> locals, StateMachine m) {
		if (expr == null)
			return name.toString();
		else {
			Enumeration<Stack<Symbol>> e = expr.elements();
			String s = name.toString();
			while (e.hasMoreElements()) {
				Stack<Symbol> x = e.nextElement();
				s = s + "." + Expression.getValue(x, locals, m.getConstants());
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
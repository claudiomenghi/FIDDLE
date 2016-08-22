package ltsa.lts.csp;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.automaton.StateMachine;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;

/* ----------------------------------------------------------------------- */

public class SeqProcessRef {
	Symbol name;
	Vector<Stack<Symbol>> actualParams;

	public static LTSOutput output;

	private boolean forPreconditionChecking;

	public SeqProcessRef(Symbol n, Vector<Stack<Symbol>> params,
			boolean forPreconditionChecking) {
		name = n;
		actualParams = params;
		this.forPreconditionChecking = forPreconditionChecking;
	}

	public LabelledTransitionSystem instantiate(
			Hashtable<String, Value> locals, Hashtable constants) {
		// compute parameters
		Vector<Value> actuals = paramValues(locals, constants);
		String refname = (actuals == null) ? name.toString() : name.toString()
				+ StateMachine.paramString(actuals);
		// have we already compiled it?
		LabelledTransitionSystem mach = LTSCompiler.compiled.get(refname);
		if (mach == null) {
			// we have not got one so first see if its a defined process
			ProcessSpec p = LTSCompiler.getSpec(name.toString());
			if (p != null) {
				p = p.myclone();
				if (actualParams != null) { // check that parameter arity is
											// correct
					if (actualParams.size() != p.parameters.size()) {
						Diagnostics.fatal(
								"actuals do not match formal parameters", name);
					}
				}
				StateMachine one = new StateMachine(p, actuals);
				mach = one.makeCompactState();
				
				output.outln("-- compiled:" + mach.getName());
			}
		}
		if (mach == null) {
			CompositionExpression ce = LTSCompiler.composites.get(name
					.toString());
			if (ce != null) {
				CompositeState cs = ce.compose(actuals);
				mach = cs.create(output);
			}
		}
		if (mach != null) {
			LTSCompiler.compiled.put(mach.getName(), mach); // add to compiled
			
			
			// processes
			if (!mach.isSequential()) {
				Diagnostics.fatal("process is not sequential - " + name, name);
			}
			return mach.myclone();
		}
		Diagnostics.fatal("process definition not found- " + name, name);
		return null;
	}

	private Vector<Value> paramValues(Hashtable<String, Value> locals,
			Hashtable constants) {
		if (actualParams == null) {
			return null;
		}
		Enumeration<Stack<Symbol>> e = actualParams.elements();
		Vector<Value> v = new Vector<>();
		while (e.hasMoreElements()) {
			Stack<Symbol> stk = e.nextElement();
			v.addElement(Expression.getValue(stk, locals, constants));
		}
		return v;
	}

}

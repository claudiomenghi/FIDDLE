package ltsa.lts;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import ltsa.lts.operations.compiler.LTSCompiler;

/* ----------------------------------------------------------------------- */

public class SeqProcessRef {
	Symbol name;
	Vector actualParams;

	public static LTSOutput output;

	public SeqProcessRef(Symbol n, Vector params) {
		name = n;
		actualParams = params;
	}

	CompactState instantiate(Hashtable locals, Hashtable constants) {
		// compute parameters
		Vector<Value> actuals = paramValues(locals, constants);
		String refname = (actuals == null) ? name.toString() : name.toString()
				+ StateMachine.paramString(actuals);
		// have we already compiled it?
		CompactState mach = LTSCompiler.compiled.get(refname);
		if (mach == null) {
			// we have not got one so first see if its a defined process
			ProcessSpec p = (ProcessSpec) LTSCompiler.processes.get(name
					.toString());
			if (p != null) {
				p = p.myclone();
				if (actualParams != null) { // check that parameter arity is
											// correct
					if (actualParams.size() != p.parameters.size())
						Diagnostics.fatal(
								"actuals do not match formal parameters", name);
				}
				StateMachine one = new StateMachine(p, actuals);
				mach = one.makeCompactState();
				output.outln("-- compiled:" + mach.name);
			}
		}
		if (mach == null) {
			CompositionExpression ce = (CompositionExpression) LTSCompiler.composites
					.get(name.toString());
			if (ce != null) {
				CompositeState cs = ce.compose(actuals);
				mach = cs.create(output);
			}
		}
		if (mach != null) {
			LTSCompiler.compiled.put(mach.name, mach); // add to compiled
														// processes
			if (!mach.isSequential())
				Diagnostics.fatal("process is not sequential - " + name, name);
			return mach.myclone();
		}
		Diagnostics.fatal("process definition not found- " + name, name);
		return null;
	}

	private Vector<Value> paramValues(Hashtable locals, Hashtable constants) {
		if (actualParams == null)
			return null;
		Enumeration e = actualParams.elements();
		Vector<Value> v = new Vector<Value>();
		while (e.hasMoreElements()) {
			Stack stk = (Stack) e.nextElement();
			v.addElement(Expression.getValue(stk, locals, constants));
		}
		return v;
	}

}


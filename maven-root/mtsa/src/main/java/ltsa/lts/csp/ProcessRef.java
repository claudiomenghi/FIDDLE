package ltsa.lts.csp;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.automaton.StateMachine;
import ltsa.lts.automata.lts.state.AutCompactState;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.PostconditionDefinitionManager;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;

public class ProcessRef {
	public Symbol name;
	public Vector<Stack<Symbol>> actualParams; // Vector of expressions stacks
	boolean forceCompilation = false;
	boolean passBackClone = true;

	private final PostconditionDefinitionManager postManager;
	public ProcessRef(PostconditionDefinitionManager postManager) {
		this.postManager=postManager;
	}

	public ProcessRef(PostconditionDefinitionManager postManager, boolean forceCompilation, boolean passBackClone) {
		this.forceCompilation = forceCompilation;
		this.passBackClone = passBackClone;
		this.postManager=postManager;
	}

	public void instantiate(CompositionExpression c, Vector machines,
			LTSOutput output, Hashtable<String, Value> locals) {
		// compute parameters
		Vector<Value> actuals = paramValues(locals, c);
		String refname = (actuals == null) ? name.toString() : name.toString()
				+ StateMachine.paramString(actuals);
		// have we already compiled it?
		LabelledTransitionSystem labeledTransitionSystem= c.compiledProcesses.get(refname);
		if (labeledTransitionSystem != null) {
			if (this.passBackClone) {
				machines.addElement(labeledTransitionSystem.myclone());
			}
			return;
		}
		// we have not got one so first see if its a process
		ProcessSpec p = c.processes.get(name.toString());
		if (p != null) {
			if (actualParams != null) { // check that parameter arity is correct
				if (actualParams.size() != p.parameters.size())
					Diagnostics.fatal("actuals do not match formal parameters",
							name);
			}
			if (!p.imported()) {
				StateMachine one = new StateMachine(p, actuals);
				labeledTransitionSystem = one.makeCompactState();
			} else {
				labeledTransitionSystem = new AutCompactState(p.getSymbol(), p.importFile);
			}

			if (this.passBackClone) {
				machines.addElement(labeledTransitionSystem.myclone()); // pass back clone
			}
			c.compiledProcesses.put(labeledTransitionSystem.getName(), labeledTransitionSystem); // add to compiled
			// processes
			if (!p.imported())
				c.output.outln("Compiled: " + labeledTransitionSystem.getName());
			else
				c.output.outln("Imported: " + labeledTransitionSystem.getName());
			return;
		}
		// it could be a constraint
		labeledTransitionSystem = ltsa.lts.ltl.AssertDefinition.compileConstraint(output, name,
				refname, actuals);
		if (labeledTransitionSystem != null) {
			if (this.passBackClone) {
				machines.addElement(labeledTransitionSystem.myclone()); // pass back clone
			}
			c.compiledProcesses.put(labeledTransitionSystem.getName(), labeledTransitionSystem); // add to compiled
			// processes
			return;
		}


		
		// it must be a composition
		CompositionExpression ce = (CompositionExpression) c.getComposites()
				.get(name.toString());
		if (ce == null)
			Diagnostics.fatal("definition not found- " + name, name);
		if (actualParams != null) { // check that parameter arity is correct
			if (actualParams.size() != ce.parameters.size()
					&& !ce.makeControlStack) // we allow control stacks with or
												// without parameters
				Diagnostics.fatal("actuals do not match formal parameters",
						name);
		}
		CompositeState cs;
		if (ce == c) {
			@SuppressWarnings("unchecked")
			Hashtable<String, Value> save = (Hashtable<String, Value>) c.constants
					.clone();
			cs = ce.compose(actuals);
			c.constants = save;
		} else
			cs = ce.compose(actuals);
		// don't compose if not necessary, maintain as a list of machines
		if (!this.forceCompilation  && cs.compositionNotRequired()) {
			for (LabelledTransitionSystem m : cs.getMachines()) {
				labeledTransitionSystem = m;
				labeledTransitionSystem.setName( cs.getName() + "." + labeledTransitionSystem.getName());
			}
			machines.addElement(cs); // flatten later if correct
		} else {
			labeledTransitionSystem = cs.create(output);
			c.compiledProcesses.put(labeledTransitionSystem.getName(), labeledTransitionSystem); // add to compiled
			// processes
			c.output.outln("Compiled: " + labeledTransitionSystem.getName());
			if (this.passBackClone) {
				machines.addElement(labeledTransitionSystem.myclone()); // pass back clone
			}
		}
	}

	private Vector<Value> paramValues(Hashtable<String, Value> locals,
			CompositionExpression c) {
		if (actualParams == null)
			return null;
		Vector<Value> v = new Vector<Value>();
		for (Stack<Symbol> stk : actualParams) {
			v.addElement(Expression.getValue(stk, locals, c.constants));
		}
		return v;
	}

	@Override
	public String toString() {
		return this.name.toString() + " - " + this.getClass();
	}

	public PostconditionDefinitionManager getPostManager() {
		return postManager;
	}
}
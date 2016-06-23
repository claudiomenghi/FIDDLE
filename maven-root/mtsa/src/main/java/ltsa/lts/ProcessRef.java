package ltsa.lts;

import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Vector;

import ltsa.dispatcher.TransitionSystemDispatcher;
import ltsa.lts.chart.TriggeredScenarioDefinition;
import ltsa.lts.chart.util.TriggeredScenarioTransformationException;
import ltsa.lts.distribution.DistributionDefinition;

public class ProcessRef {
	public Symbol name;
	public Vector<Stack<Symbol>> actualParams; // Vector of expressions stacks
	boolean forceCompilation = false;
	boolean passBackClone = true;

	public ProcessRef() {
	}

	public ProcessRef(boolean forceCompilation, boolean passBackClone) {
		this.forceCompilation = forceCompilation;
		this.passBackClone = passBackClone;
	}

	public void instantiate(CompositionExpression c, Vector machines,
			LTSOutput output, Hashtable<String, Value> locals) {
		// compute parameters
		Vector<Value> actuals = paramValues(locals, c);
		String refname = (actuals == null) ? name.toString() : name.toString()
				+ StateMachine.paramString(actuals);
		// have we already compiled it?
		CompactState mach = c.compiledProcesses.get(refname);
		if (mach != null) {
			if (this.passBackClone) {
				machines.addElement(mach.myclone());
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
				mach = one.makeCompactState();
			} else {
				mach = new AutCompactState(p.name, p.importFile);
			}

			if (this.passBackClone) {
				machines.addElement(mach.myclone()); // pass back clone
			}
			c.compiledProcesses.put(mach.name, mach); // add to compiled
			// processes
			if (!p.imported())
				c.output.outln("Compiled: " + mach.name);
			else
				c.output.outln("Imported: " + mach.name);
			return;
		}
		// it could be a constraint
		mach = ltsa.lts.ltl.AssertDefinition.compileConstraint(output, name,
				refname, actuals);
		if (mach != null) {
			if (this.passBackClone) {
				machines.addElement(mach.myclone()); // pass back clone
			}
			c.compiledProcesses.put(mach.name, mach); // add to compiled
			// processes
			return;
		}

		// it could be a triggered scenario
		if (TriggeredScenarioDefinition.contains(name)) {
			try {
				mach = TriggeredScenarioDefinition.getDefinition(name)
						.synthesise(output);
				if (this.passBackClone) {
					machines.addElement(mach.myclone()); // pass back clone
				}
				c.compiledProcesses.put(mach.getName(), mach); // add to
				// compiled
				// processes
			} catch (TriggeredScenarioTransformationException e) {
				throw new RuntimeException(e);
			}
			return;
		}

		// it could be a component from a distribution
		if (DistributionDefinition.contains(name)) {

			DistributionDefinition distributionDefinition = DistributionDefinition
					.getDistributionDefinitionContainingComponent(name);
			Symbol systemModelId = distributionDefinition.getSystemModel();

			// system model is a reference to a process
			ProcessRef systemModelProcessRef = new ProcessRef(true, false);
			systemModelProcessRef.name = systemModelId;

			// this should compile the process (if it was not already compiled)
			systemModelProcessRef.instantiate(c, machines, output, locals);

			// get the system model
			CompactState systemModel = c.compiledProcesses.get(systemModelId
					.getName());

			// try to distribute
			Collection<CompactState> distributedComponents = new LinkedList<CompactState>();
			boolean isDistributionSuccessful = TransitionSystemDispatcher
					.tryDistribution(systemModel, distributionDefinition,
							output, distributedComponents);

			// Add the distributed components as compiled
			for (CompactState compactState : distributedComponents) {
				if (this.passBackClone
						&& compactState.getName().equals(name.getName())) {
					// the machine is only the one with the requested name
					machines.addElement(compactState.myclone()); // pass back
																	// clone
				}
				c.compiledProcesses.put(compactState.getName(), compactState); // add
																				// to
																				// compiled
																				// process
			}
			if (!isDistributionSuccessful) {
				Diagnostics.fatal("Model " + systemModelId.getName()
						+ " could not be distributed.", systemModelId);
			}
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
			Hashtable<String, Value> save = (Hashtable<String, Value>) c.constants
					.clone();
			cs = ce.compose(actuals);
			c.constants = save;
		} else
			cs = ce.compose(actuals);
		// don't compose if not necessary, maintain as a list of machines
		if (!this.forceCompilation && cs.compositionNotRequired()) {
			for (CompactState m : cs.machines) {
				mach = m;
				mach.name = cs.name + "." + mach.name;
			}
			machines.addElement(cs); // flatten later if correct
		} else {
			mach = cs.create(output);
			c.compiledProcesses.put(mach.name, mach); // add to compiled
			// processes
			c.output.outln("Compiled: " + mach.name);
			if (this.passBackClone) {
				machines.addElement(mach.myclone()); // pass back clone
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
}
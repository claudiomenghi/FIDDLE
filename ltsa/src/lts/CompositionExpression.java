package lts;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Vector;

import lts.chart.TriggeredScenarioDefinition;
import lts.chart.util.TriggeredScenarioTransformationException;
import lts.distribution.DistributionDefinition;
import lts.ltl.AssertDefinition;
import lts.util.MTSUtils;

import org.apache.commons.lang.Validate;

import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import control.ControllerGoalDefinition;
import control.util.GoalDefToControllerGoal;
import dispatcher.TransitionSystemDispatcher;

/* -----------------------------------------------------------------------*/
class CompositeBody {
	// single process reference P
	ProcessRef singleton;
	// list of CompositeBodies ( P || Q)
	Vector<CompositeBody> procRefs;
	// conditional if Bexp then P else Q
	Stack<Symbol> boolexpr;
	CompositeBody thenpart; // overloaded as body of replicator
	CompositeBody elsepart;
	// forall[i:R][j:S].
	ActionLabels range; // used to store forall range/ranges
	// the following are only applied to singletons & procRefs (...)
	ActionLabels prefix; // a:
	ActionLabels accessSet; // S::
	Vector<RelabelDefn> relabelDefns; // list of relabelling defns

	// private Vector accessors = null; //never used?
	// private Relation relabels = null;

	/**
	 * This method fills the <i>machines</i> with the compiled version of the
	 * processes referenced in the composition expression <i>c</i>. In addition
	 * to handling conditionals and applying accessors it also applies
	 * relabelling if necessary.
	 */
	void compose(CompositionExpression c, Vector machines,
			Hashtable<String, Value> locals) {
		Vector<String> accessors = accessSet == null ? null : accessSet
				.getActions(locals, c.constants);
		Relation relabels = RelabelDefn.getRelabels(relabelDefns, c.constants,
				locals);
		// conditional compostion
		if (boolexpr != null) {
			if (Expression.evaluate(boolexpr, locals, c.constants).intValue() != 0)
				thenpart.compose(c, machines, locals);
			else if (elsepart != null)
				elsepart.compose(c, machines, locals);
		} else if (range != null) {
			// replicated composition
			range.initContext(locals, c.constants);
			while (range.hasMoreNames()) {
				range.nextName();
				thenpart.compose(c, machines, locals);
			}
			range.clearContext();
		} else {
			// singleton or list
			Vector tempMachines = getPrefixedMachines(c, locals);
			// apply accessors
			if (accessors != null)
				for (Object o : tempMachines) {
					if (o instanceof CompactState) {
						CompactState mach = (CompactState) o;
						mach.addAccess(accessors);
					} else {
						CompositeState cs = (CompositeState) o;
						cs.addAccess(accessors);
					}
				}
			// apply relabels
			if (relabels != null)
				for (int i = 0; i < tempMachines.size(); ++i) {
					Object o = tempMachines.elementAt(i);
					if (o instanceof CompactState) {
						CompactState mach = (CompactState) o;
						mach.relabel(relabels);
					} else {
						CompositeState cs = (CompositeState) o;
						CompactState mm = cs.relabel(relabels, c.output);
						if (mm != null)
							tempMachines.setElementAt(mm, i);
					}
				}
			// add tempMachines to machines
			machines.addAll(tempMachines);
		}
	}

	/**
	 * Relabels the set of referenced processes in <i>c<\i>.
	 */
	private Vector getPrefixedMachines(CompositionExpression c,
			Hashtable<String, Value> locals) {
		if (prefix == null) {
			return getMachines(c, locals);
		} else {
			Vector pvm = new Vector();
			prefix.initContext(locals, c.constants);
			while (prefix.hasMoreNames()) {
				String px = prefix.nextName();
				Vector vm = getMachines(c, locals);
				for (Object o : vm) {
					if (o instanceof CompactState) {
						CompactState m = (CompactState) o;
						m.prefixLabels(px);
						pvm.addElement(m);
					} else {
						CompositeState cs = (CompositeState) o;
						cs.prefixLabels(px);
						pvm.addElement(cs);
					}
				}
			}
			prefix.clearContext();
			return pvm;
		}
	}

	/**
	 * Computes the set of referenced processes in <i>c<\i>.
	 */
	private Vector getMachines(CompositionExpression c,
			Hashtable<String, Value> locals) {
		Vector vm = new Vector();
		if (singleton != null) {
			singleton.instantiate(c, vm, c.output, locals);
		} else if (procRefs != null) {
			for (CompositeBody cb : procRefs) {
				cb.compose(c, vm, locals);
			}
		}
		return vm;
	}

}

public class CompositionExpression {
	Symbol name;
	CompositeBody body;
	Hashtable<String, Value> constants;
	Hashtable<String, Value> init_constants = new Hashtable<String, Value>(); // constant
																				// table
	Vector<String> parameters = new Vector<String>(); // position of names in
														// constants
	Hashtable<String, ProcessSpec> processes; // table of process definitions
	Hashtable<String, CompactState> compiledProcesses; // table of compiled
														// definitions
	private Hashtable composites; // table of composite definitions
	protected LTSOutput output; // a bit of a hack
	boolean priorityIsLow = true;
	LabelSet priorityActions; // priority action set
	LabelSet alphaHidden; // Concealment
	boolean exposeNotHide = false;
	boolean makeDeterministic = false;
	boolean makeMinimal = false;
	boolean makeProperty = false;
	boolean makeCompose = false;
	boolean makeOptimistic = false;
	boolean makePessimistic = false;
	boolean makeClousure = false;
	boolean makeAbstract = false;
	int compositionType = -1;
	boolean makeController = false;
	boolean makeSyncController = false;
	boolean makeMDP = false;
	boolean makeEnactment = false;
	boolean checkCompatible = false;
	boolean isStarEnv = false;
	boolean isPlant = false;
	boolean isControlledDet = false;
	boolean makeControlStack = false;
	public Symbol goal;
	public Vector<Symbol> controlStackEnvironments;
	public Symbol enactmentControlled;
	
	/**
	 * If the isComponent flag is true, then this ProcessSpec represents a
	 * composition of several component processes. The component process can be
	 * built with the componentAlphabet.
	 */
	private boolean makeComponent = false;

	/**
	 * this alphabet is one of the component. It must be a subset of the process
	 * alphabet.
	 */
	private LabelSet componentAlphabet;

	public boolean isMakeComponent() {
		return makeComponent;
	}

	public void setMakeComponent(boolean makeComponent) {
		this.makeComponent = makeComponent;
	}

	public LabelSet getComponentAlphabet() {
		return componentAlphabet;
	}

	public void setComponentAlphabet(LabelSet componentAlphabet) {
		this.componentAlphabet = componentAlphabet;
	}

	protected CompositeState compose(Vector<Value> actuals) {
		Vector machines = new Vector(); // list of instantiated machines
		Hashtable<String, Value> locals = new Hashtable<String, Value>();
		constants = (Hashtable<String, Value>) init_constants.clone();
		// Vector references; // list of parsed process references
		if (actuals != null)
			doParams(actuals);
		if (!makeControlStack) // there is no body
			body.compose(this, machines, locals);

		Vector<CompactState> flatmachines = new Vector<CompactState>();
		for (Object o : machines) { // machines contains a mixture of two
									// unrelated classes!
			if (o instanceof CompactState)
				flatmachines.addElement((CompactState) o);
			else {
				CompositeState cs = (CompositeState) o;
				// if (MTSUtils.isMTSRepresentation(cs)) {
				// MTSA always works with the composed model.
				// There is no On-The-Fly model checking algorithm for MTSs yet
				// assert (cs.getCompositionType() != -1);
				CompositeState toCompose = cs.clone();
				TransitionSystemDispatcher.applyComposition(toCompose, output);
				flatmachines.addElement(toCompose.getComposition());
				// } else {
				// for (Enumeration ee = cs.machines.elements();
				// ee.hasMoreElements();) {
				// flatmachines.addElement(ee.nextElement());
				// }
				// }

			}
		}
		String refname = (actuals == null || makeControlStack) ? name
				.toString() : name.toString()
				+ StateMachine.paramString(actuals);
		CompositeState c = new CompositeState(refname, flatmachines);
		c.priorityIsLow = priorityIsLow;
		c.priorityLabels = computeAlphabet(priorityActions);
		if (MTSUtils.isMTSRepresentation(c) && c.priorityLabels != null
				&& !c.priorityLabels.isEmpty()) {
			throw new RuntimeException(
					"Priorities over MTS are not definde yet.");
		}

		if (makeControlStack) {
			if (actuals != null) {
				if (actuals.size() != 1)
					throw new RuntimeException(
							"Control stack references may take only one integer parameter indicating a selected tier.");
				try {
					c.controlStackSpecificTier = Integer.parseInt(actuals
							.get(0).toString());
				} catch (NumberFormatException why) {
					throw new RuntimeException(
							"Control stack references may take only one integer parameter indicating a selected tier.");
				}
			}
			c.controlStackEnvironments = new Hashtable<String, Object>();
			for (Symbol env : controlStackEnvironments) {
				ProcessRef proc = new ProcessRef();
				proc.name = env;
				Vector result = new Vector(); // another mixture of
												// compactstate/compositestate
				proc.instantiate(this, result, output, null);
				Object cs = result.get(0);
				c.controlStackEnvironments.put(env.toString(), cs);
			}
		}

		if (makeEnactment)
		{
	    LabelSet labelSet = (LabelSet) LabelSet.getConstants().get(enactmentControlled.toString());
	    if (labelSet==null)
	      Diagnostics.fatal("Controllable actions set '"+enactmentControlled.toString()+"' not defined.");
	    c.enactmentControlled = labelSet.getActions(null);
		}
		
		c.hidden = computeAlphabet(alphaHidden);
		c.exposeNotHide = exposeNotHide;
		c.makeDeterministic = makeDeterministic;
		c.makeOptimistic = makeOptimistic;
		c.makePessimistic = makePessimistic;
		c.makeMinimal = makeMinimal;
		c.makeCompose = makeCompose;
		c.makeClousure = makeClousure;
		c.makeAbstract = makeAbstract;
		c.makeMDP = makeMDP;
		c.makeEnactment = makeEnactment;
		c.makeController = makeController;
		c.makeSyncController = makeSyncController;
		c.checkCompatible = checkCompatible;
		c.isStarEnv = isStarEnv;
		c.isPlant = isPlant;
		c.isControlledDet = isControlledDet;
		c.makeControlStack = makeControlStack;
		c.setCompositionType(compositionType);
		c.setMakeComponent(makeComponent);
		if (makeProperty) {
			c.makeDeterministic = true;
			c.isProperty = true;
		}
		if (c.makeController || c.checkCompatible || c.isPlant
				|| c.isControlledDet || c.makeSyncController) {
			this.buildAndSetGoal(c);
		}
		c.setComponentAlphabet(computeAlphabet(this.getComponentAlphabet()));
		return c;
	}

	private void buildAndSetGoal(CompositeState c) {

		ControllerGoalDefinition pendingGoal = ControllerGoalDefinition
				.getDefinition(goal);
        c.env = c.machines.get(0);
		c.machines.addAll(CompositionExpression.preProcessSafetyReqs(
				pendingGoal, output));
		c.goal = GoalDefToControllerGoal.getInstance().buildControllerGoal(
				pendingGoal);
	}

	public static Collection<CompactState> preProcessSafetyReqs(
			ControllerGoalDefinition goal, LTSOutput output) {
		ControllerGoalDefinition pendingGoal = ControllerGoalDefinition
				.getDefinition(goal.getName());
		Collection<CompactState> safetyReqs = new HashSet<CompactState>();
		for (Symbol safetyDef : pendingGoal.getSafetyDefinitions()) {
			ProcessSpec p = LTSCompiler.processes.get(safetyDef
					.getName());
			CompactState cs = AssertDefinition.compileConstraint(output,
					safetyDef.getName());
			if (p != null) {
				StateMachine one = new StateMachine(p);
				CompactState c = one.makeCompactState();
				CompactState c2 = c;
				safetyReqs.add(c2);
			} else if (cs != null) {
				Validate.notNull(cs, "LTL PROPERTY: " + safetyDef.getName()
						+ " not defined.");
				MTS<Long, String> convert = AutomataToMTSConverter
						.getInstance().convert(cs);
				convert.removeAction("@" + safetyDef.getName());
				cs = MTSToAutomataConverter.getInstance().convert(convert,
						safetyDef.getName());
				safetyReqs.add(cs);
			} else {
				CompositionExpression ce = LTSCompiler.getComposite(safetyDef.getName());
				if (ce == null) {
					StringBuffer sb = new StringBuffer();
					sb.append("Safety property ").append(safetyDef.getName())
							.append(" is not defined.");
					Diagnostics.fatal(sb.toString());
				}
				CompositeState compile = ce.compose(null);
				compile.compose(output);

				MTS<Long, String> convert = AutomataToMTSConverter
						.getInstance().convert(compile.composition);
				convert.removeAction("@" + compile.name); // get rid of those
															// horrible @s
				
				CompactState convert2 = MTSToAutomataConverter
						.getInstance().convert(convert,
								safetyDef.getName(), false);

				safetyReqs.add(convert2);
				// for (Iterator it = compile.getMachines().iterator();
				// it.hasNext();) {
				// safetyReqs.add((CompactState) it.next());
				// }
			}
		}
		return safetyReqs;
	}

	private void doParams(Vector<Value> actuals) {
		Enumeration<Value> a = actuals.elements();
		Enumeration<String> f = parameters.elements();
		while (a.hasMoreElements() && f.hasMoreElements())
			constants.put(f.nextElement(), a.nextElement());
	}

	private Vector<String> computeAlphabet(LabelSet a) {
		if (a == null)
			return null;
		return a.getActions(constants);
	}

	void setComposites(Hashtable composites) {
		this.composites = composites;
	}

	Hashtable getComposites() {
		return composites;
	}

	public Symbol getName() {
		return name;
	}

	public void setName(Symbol name) {
		this.name = name;
	}
}

class ProcessRef {
	Symbol name;
	Vector<Stack<Symbol>> actualParams; // Vector of expressions stacks
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
		mach = lts.ltl.AssertDefinition.compileConstraint(output, name,
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
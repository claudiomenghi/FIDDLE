package ltsa.lts.csp;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.chart.TriggeredScenarioDefinition;
import ltsa.lts.chart.util.TriggeredScenarioTransformationException;
import ltsa.lts.distribution.DistributionDefinition;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.lts.StateMachine;
import ltsa.lts.ltscomposition.CompactState;
import ltsa.lts.ltscomposition.CompositeState;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.LTSOutput;
import ltsa.lts.parser.LabelSet;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.util.MTSUtils;

import org.apache.commons.lang.Validate;

import MTSTools.ac.ic.doc.mtstools.model.MTS;
import ltsa.ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ltsa.ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ltsa.control.ControllerGoalDefinition;
import ltsa.control.util.GoalDefToControllerGoal;
import ltsa.dispatcher.TransitionSystemDispatcher;



public class CompositionExpression {
	public Symbol name;
	public CompositeBody body;
	public Hashtable<String, Value> constants;
	public Hashtable<String, Value> init_constants = new Hashtable<String, Value>(); // constant
																				// table
	public Vector<String> parameters = new Vector<String>(); // position of names in
														// constants
	public Hashtable<String, ProcessSpec> processes; // table of process definitions
	public Hashtable<String, CompactState> compiledProcesses; // table of compiled
														// definitions
	private Hashtable composites; // table of composite definitions
	public LTSOutput output; // a bit of a hack
	public boolean priorityIsLow = true;
	public LabelSet priorityActions; // priority action set
	public LabelSet alphaHidden; // Concealment
	public boolean exposeNotHide = false;
	public boolean makeDeterministic = false;
	public boolean makeMinimal = false;
	public boolean makeProperty = false;
	public boolean makeCompose = false;
	public boolean makeOptimistic = false;
	public boolean makePessimistic = false;
	public boolean makeClousure = false;
	public boolean makeAbstract = false;
	public int compositionType = -1;
	public boolean makeController = false;
	public boolean makeSyncController = false;
	public boolean makeMDP = false;
	public boolean makeEnactment = false;
	public boolean checkCompatible = false;
	public boolean isStarEnv = false;
	public boolean isPlant = false;
	public boolean isControlledDet = false;
	public boolean makeControlStack = false;
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

	public CompositeState compose(Vector<Value> actuals) {
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
					.getValue());
			CompactState cs = AssertDefinition.compileConstraint(output,
					safetyDef.getValue());
			if (p != null) {
				StateMachine one = new StateMachine(p);
				CompactState c = one.makeCompactState();
				CompactState c2 = c;
				safetyReqs.add(c2);
			} else if (cs != null) {
				Validate.notNull(cs, "LTL PROPERTY: " + safetyDef.getValue()
						+ " not defined.");
				MTS<Long, String> convert = AutomataToMTSConverter
						.getInstance().convert(cs);
				convert.removeAction("@" + safetyDef.getValue());
				cs = MTSToAutomataConverter.getInstance().convert(convert,
						safetyDef.getValue());
				safetyReqs.add(cs);
			} else {
				CompositionExpression ce = LTSCompiler.getComposite(safetyDef.getValue());
				if (ce == null) {
					StringBuffer sb = new StringBuffer();
					sb.append("Safety property ").append(safetyDef.getValue())
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
								safetyDef.getValue(), false);

				safetyReqs.add(convert2);
				
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

	public void setComposites(Hashtable composites) {
		this.composites = composites;
	}

	public Hashtable getComposites() {
		return composites;
	}

	public Symbol getName() {
		return name;
	}

	public void setName(Symbol name) {
		this.name = name;
	}
}


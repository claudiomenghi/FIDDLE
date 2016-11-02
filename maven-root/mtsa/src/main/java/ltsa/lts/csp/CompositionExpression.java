package ltsa.lts.csp;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import ltsa.ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ltsa.ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ltsa.control.ControllerGoalDefinition;
import ltsa.control.util.GoalDefToControllerGoal;
import ltsa.dispatcher.TransitionSystemDispatcher;
import ltsa.lts.Diagnostics;
import ltsa.lts.automata.automaton.StateMachine;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.PostconditionDefinitionManager;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.parser.actions.LabelSet;
import ltsa.lts.util.MTSUtils;

import org.apache.commons.lang.Validate;

import MTSTools.ac.ic.doc.mtstools.model.MTS;

public class CompositionExpression {
	public Symbol name;
	public CompositeBody body;
	public Hashtable<String, Value> constants;
	public Hashtable<String, Value> initConstants = new Hashtable<>(); // constant
																		// table
	public Vector<String> parameters = new Vector<>(); // position of names in
														// constants
	public Hashtable<String, ProcessSpec> processes; // table of process
														// definitions
	/**
	 * table of compiled
	 */
	public Hashtable<String, LabelledTransitionSystem> compiledProcesses;

	/**
	 * table of composite definitions
	 */
	private Hashtable composites;
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

	private final PostconditionDefinitionManager postManager;

	public CompositionExpression(PostconditionDefinitionManager postManager) {
		this.postManager = postManager;

	}

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

	@SuppressWarnings("unchecked")
	public CompositeState compose(Vector<Value> actuals) {

		Vector<LabelledTransitionSystem> machines = new Vector<>(); // list of
																	// instantiated
		// machines
		Hashtable<String, Value> locals = new Hashtable<>();
		constants = (Hashtable<String, Value>) initConstants.clone();
		// Vector references; // list of parsed process references
		if (actuals != null) {
			doParams(actuals);
		}
		if (!makeControlStack) { // there is no body
			body.compose(this, machines, locals);
		}
		Vector<LabelledTransitionSystem> flatmachines = new Vector<>();

		Map<String, LabelledTransitionSystem> mapNameMachine = new HashMap<>();
		// machines contains a mixture of two unrelated classes!
		for (Object o : machines) {
			if (o instanceof LabelledTransitionSystem) {
				LabelledTransitionSystem tmp = (LabelledTransitionSystem) o;
				flatmachines.addElement(tmp);
				mapNameMachine.put(tmp.getName(), tmp);
			} else {
				CompositeState cs = (CompositeState) o;
				// MTSA always works with the composed model.
				// There is no On-The-Fly model checking algorithm for MTSs yet
				CompositeState toCompose = cs.clone();
				TransitionSystemDispatcher.applyComposition(toCompose, output);
				LabelledTransitionSystem composition = toCompose
						.getComposition();
				flatmachines.addElement(composition);
				mapNameMachine.put(composition.getName(), composition);

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
				ProcessRef proc = new ProcessRef(this.postManager);
				proc.name = env;
				// another mixture of compactstate compositestate
				Vector result = new Vector<>();
				proc.instantiate(this, result, output, null);
				Object cs = result.get(0);
				c.controlStackEnvironments.put(env.toString(), cs);
			}
		}

		if (makeEnactment) {
			LabelSet labelSet = (LabelSet) LabelSet.getConstants().get(
					enactmentControlled.toString());
			if (labelSet == null)
				Diagnostics.fatal("Controllable actions set '"
						+ enactmentControlled.toString() + "' not defined.");
			c.enactmentControlled = labelSet.getActions(null);
		}

		c.setHidden(computeAlphabet(alphaHidden));
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
		c.setEnv(c.getMachines().get(0));
		c.getMachines()
				.addAll(CompositionExpression.preProcessSafetyReqs(pendingGoal,
						output));
		c.goal = GoalDefToControllerGoal.getInstance().buildControllerGoal(
				pendingGoal);
	}

	public static Collection<LabelledTransitionSystem> preProcessSafetyReqs(
			ControllerGoalDefinition goal, LTSOutput output) {
		ControllerGoalDefinition pendingGoal = ControllerGoalDefinition
				.getDefinition(goal.getName());
		Collection<LabelledTransitionSystem> safetyReqs = new HashSet<>();
		for (Symbol safetyDef : pendingGoal.getSafetyDefinitions()) {
			ProcessSpec p = LTSCompiler.getSpec(safetyDef.getValue());
			LabelledTransitionSystem cs = AssertDefinition.compileConstraint(
					output, safetyDef.getValue());
			if (p != null) {
				StateMachine one = new StateMachine(p);
				LabelledTransitionSystem c = one.makeCompactState();
				LabelledTransitionSystem c2 = c;
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
				CompositionExpression ce = LTSCompiler.getComposite(safetyDef
						.getValue());
				if (ce == null) {
					StringBuffer sb = new StringBuffer();
					sb.append("Safety property ").append(safetyDef.getValue())
							.append(" is not defined.");
					Diagnostics.fatal(sb.toString());
				}
				CompositeState compile = ce.compose(null);
				compile.compose(output);

				MTS<Long, String> convert = AutomataToMTSConverter
						.getInstance().convert(compile.getComposition());
				convert.removeAction("@" + compile.getName()); // get rid of
																// those
																// horrible @s

				LabelledTransitionSystem convert2 = MTSToAutomataConverter
						.getInstance().convert(convert, safetyDef.getValue(),
								false);

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

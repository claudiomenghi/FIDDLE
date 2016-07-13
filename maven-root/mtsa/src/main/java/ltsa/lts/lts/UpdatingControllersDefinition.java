package ltsa.lts.lts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.csp.CompositionExpression;
import ltsa.lts.ltscomposition.CompositeState;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.LabelSet;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.updatingControllers.UpdateConstants;
import ltsa.updatingControllers.structures.UpdatingControllerCompositeState;
import ltsa.updatingControllers.synthesis.UpdatingControllersUtils;
import MTSSynthesis.ar.dc.uba.model.condition.Fluent;
import ltsa.control.ControllerGoalDefinition;
import MTSSynthesis.controller.model.gr.GRControllerGoal;

public class UpdatingControllersDefinition extends CompositionExpression {

	private Symbol oldController;
	private Symbol oldEnvironment;
	private Symbol newEnvironment;
	private Symbol oldGoal;
	private Symbol newGoal;
	private List<Symbol> safety;
	private Boolean nonblocking;
	private List<Symbol> oldPropositions;
	private List<Symbol> newPropositions;
	private Boolean debugMode;
	private ArrayList<Symbol> checkTrace;

	public UpdatingControllersDefinition(Symbol current) {
		super();
		super.setName(current);
		oldController = new Symbol();
		oldEnvironment = new Symbol();
		newEnvironment = new Symbol();
		safety = new ArrayList<>();
		nonblocking = false;
		oldPropositions = new ArrayList<>();
		newPropositions = new ArrayList<>();
		debugMode = false;
		checkTrace = new ArrayList<>();
	}

	public Set<String> generateUpdatingControllableActions(ControllerGoalDefinition oldGoalDef,
	                                                       ControllerGoalDefinition newGoalDef) {
		Set<String> oldControllableActions = compileSet(oldGoalDef.getControllableActionSet());
		Set<String> newControllableActions = compileSet(newGoalDef.getControllableActionSet());
		Set<String> controllable = new HashSet<>();
		controllable.addAll(oldControllableActions);
		controllable.addAll(newControllableActions);
		controllable.add(UpdateConstants.STOP_OLD_SPEC);
		controllable.add(UpdateConstants.START_NEW_SPEC);
		controllable.add(UpdateConstants.RECONFIGURE);
		return controllable;
	}

	@Override
	public CompositeState compose(Vector<Value> actuals) {
		CompositionExpression oldController = LTSCompiler.getComposite(this.getOldController().toString());
		CompositeState oldC = oldController.compose(null);

		CompositionExpression oldEnvironment = LTSCompiler.getComposite(this.getOldEnvironment().toString());
		CompositeState oldE = oldEnvironment.compose(null);

		CompositionExpression newEnvironment = LTSCompiler.getComposite(this.getNewEnvironment().toString());
		CompositeState newE = newEnvironment.compose(null);

		// GOAL
		ControllerGoalDefinition oldGoalDef = ControllerGoalDefinition.getDefinition(this.getOldGoal());
		ControllerGoalDefinition newGoalDef = ControllerGoalDefinition.getDefinition(this.getNewGoal());
		Set<String> controllableSet = this.generateUpdatingControllableActions(oldGoalDef, newGoalDef);
		Symbol controllableSetSymbol = LTSCompiler.saveControllableSet(controllableSet, this.getName().getName());
		GRControllerGoal<String> grGoal = UpdatingControllersUtils.generateGRUpdateGoal(this, oldGoalDef, newGoalDef,
			controllableSet);
		ControllerGoalDefinition newGoal = UpdatingControllersUtils.generateSafetyGoalDef(this, oldGoalDef,
			newGoalDef, controllableSetSymbol, output);

		// MAPPING PROPOSITIONS
		List<Fluent> oldPropositions = UpdatingControllersUtils.compileFluents(this.getOldPropositions());
		List<Fluent> newPropositions = UpdatingControllersUtils.compileFluents(this.getNewPropositions());

		// DEBUG
		Boolean debugMode = this.debugModelOn();
		List<String> checkTrace = UpdatingControllersUtils.compileCheckTraces(this.getCheckTrace());

		UpdatingControllerCompositeState ucce = new UpdatingControllerCompositeState(oldC, oldE, newE, newGoal,
				grGoal, oldPropositions, newPropositions, debugMode, checkTrace, name.getName());
		return ucce;
	}


	private HashSet<String> compileSet(Symbol setSymbol) {
		Hashtable<?, ?> constants = LabelSet.getConstants();
		LabelSet labelSet = (LabelSet) constants.get(setSymbol.toString());
		if (labelSet == null) {
			Diagnostics.fatal("Set not defined.");
		}
		Vector<String> actions = labelSet.getActions(null);
		return new HashSet<String>(actions);
	}

	public void setOldController(ArrayList<Symbol> oldController) {
		this.oldController = oldController.get(0);
	}

	public void setOldEnvironment(ArrayList<Symbol> oldEnvironment) {
		this.oldEnvironment = oldEnvironment.get(0);
	}
	

	public void setNewEnvironment(ArrayList<Symbol> newEnvironment) {
		this.newEnvironment = newEnvironment.get(0);
	}

	public void addSafety(Symbol safety) {
		this.safety.add(safety);
	}

	public void setNonblocking() {
		this.nonblocking = true;
	}

//	public void setUpdFluents(List<Symbol> updFluents) {
//		this.updFluents = updFluents;
//	}
	
	public void setOldPropositions(List<Symbol> input) {
		this.oldPropositions = input;
		
	}
	
	public void setNewPropositions(List<Symbol> input) {
		this.newPropositions = input;
		
	}

	public void setDebugMode() {
		debugMode = true;
	}

	public void setCheckTrace(ArrayList<Symbol> traceToCheck) {
		checkTrace = traceToCheck;
	}

	public Symbol getOldController() {
		return oldController;
	}

	public Symbol getOldEnvironment() {
		return oldEnvironment;
	}
	

	public Symbol getNewEnvironment() {
		return newEnvironment;
	}

	public List<Symbol> getSafety() {
		return safety;
	}

	public Boolean isNonblocking() {
		return nonblocking;
	}

	
	public List<Symbol> getOldPropositions() {
		return oldPropositions;
	}
		
	public List<Symbol> getNewPropositions() {
		return newPropositions;
	}
	

	public Boolean debugModelOn() {
		return debugMode;
	}

	public List<Symbol> getCheckTrace() {
		return checkTrace;
	}

	public Symbol getNewGoal() {
		return newGoal;
	}

	public void setNewGoal(Symbol newGoal) {
		this.newGoal = newGoal;
	}

	public Symbol getOldGoal() {
		return oldGoal;
	}

	public void setOldGoal(Symbol oldGoal) {
		this.oldGoal = oldGoal;
	}



}
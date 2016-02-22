package ltsa.updatingControllers.structures;

import java.util.List;
import java.util.Set;
import java.util.Vector;

import ltsa.lts.CompactState;
import ltsa.lts.CompositeState;
import ltsa.lts.Symbol;
import ltsa.lts.util.MTSUtils;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSSynthesis.ar.dc.uba.model.condition.Fluent;
import ltsa.control.ControllerGoalDefinition;
import MTSSynthesis.controller.model.gr.GRControllerGoal;

public class UpdatingControllerCompositeState extends CompositeState {

	private CompositeState oldController;
	private CompositeState oldEnvironment;
	private CompositeState hatEnvironment;
	private CompositeState newEnvironment;
	private ControllerGoalDefinition newGoalDef;
	private GRControllerGoal<String> newGoalGR;
	private Set<String> controllableActions;
	private List<Fluent> updProperties;
	private Boolean debugMode;
	private List<String> checkTrace;
	private MTS<Long, String> updateEnvironment;

	public UpdatingControllerCompositeState(CompositeState oldController, CompositeState oldEnvironment,
						CompositeState hatEnvironment, CompositeState newEnvironment, ControllerGoalDefinition newGoal,
						GRControllerGoal<String> newGoalGR, List<Fluent> updFluents, Boolean debug, 
						List<String> checkTrace, String name) {
		super.setMachines(new Vector<CompactState>());
		this.oldController = oldController;
		this.oldEnvironment = oldEnvironment;
		this.hatEnvironment = hatEnvironment;
		this.newEnvironment = newEnvironment;
		this.newGoalDef = newGoal;
		this.newGoalGR = newGoalGR;
		this.controllableActions = this.newGoalGR.getControllableActions();
		this.updProperties = updFluents;
		this.debugMode = debug;
		this.checkTrace = checkTrace;
		super.setCompositionType(Symbol.UPDATING_CONTROLLER);
		super.name = name;
	}

	public MTS<Long, String> getUpdateController() {
		return MTSUtils.getMTSComposition(this);
	}

	public MTS<Long, String> getOldController() {
		return MTSUtils.getMTSComposition(oldController);
	}

	public MTS<Long, String> getOldEnvironment() {
		return MTSUtils.getMTSComposition(oldEnvironment);
	}
	
	public MTS<Long, String> getHatEnvironment() {
		return MTSUtils.getMTSComposition(hatEnvironment);
	}

	public MTS<Long, String> getNewEnvironment() {
		return MTSUtils.getMTSComposition(newEnvironment);
	}

	public Set<String> getControllableActions() {
		return controllableActions;
	}

	public List<Fluent> getUpdProperties() {
		return updProperties;
	}

	public Boolean debugModeOn() {
		return debugMode;
	}

	public List<String> getCheckTrace() {
		return checkTrace;
	}

	public ControllerGoalDefinition getNewGoalDef() {
		return newGoalDef;
	}

	public GRControllerGoal<String> getNewGoalGR() {
		return newGoalGR;
	}

	public MTS<Long, String> getUpdateEnvironment() {
		return updateEnvironment;
	}

	public void setUpdateEnvironment(MTS<Long, String> updateEnvironment) {
		this.updateEnvironment = updateEnvironment;
	}

	@Override
	public UpdatingControllerCompositeState clone() {
		UpdatingControllerCompositeState clone = new UpdatingControllerCompositeState(
				oldController, oldEnvironment, hatEnvironment, newEnvironment, newGoalDef,
				newGoalGR, updProperties, debugMode, checkTrace, name);
		clone.setCompositionType(getCompositionType());
		clone.makeAbstract = makeAbstract;
		clone.makeClousure = makeClousure;
		clone.makeCompose = makeCompose;
		clone.makeDeterministic = makeDeterministic;
		clone.makeMinimal = makeMinimal;
		clone.makeControlStack = makeControlStack;
		clone.makeOptimistic = makeOptimistic;
		clone.makePessimistic = makePessimistic;
		clone.makeController = makeController;
		clone.setMakeComponent(isMakeComponent());
		clone.setComponentAlphabet(getComponentAlphabet());
		clone.goal = goal;
		clone.controlStackEnvironments = controlStackEnvironments;
		clone.controlStackSpecificTier = controlStackSpecificTier;
		clone.isProbabilistic = isProbabilistic;
		return clone;
	}
}
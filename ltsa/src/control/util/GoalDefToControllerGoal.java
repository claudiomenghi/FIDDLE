package control.util;

import ar.dc.uba.model.condition.Fluent;
import control.ControllerGoalDefinition;
import controller.model.ControllerGoal;
import controller.model.gr.GRControllerGoal;
import lts.Diagnostics;
import lts.LabelSet;
import lts.Symbol;
import lts.chart.util.FormulaUtils;
import lts.ltl.AssertDefinition;
import lts.ltl.FormulaFactory;
import lts.ltl.PredicateDefinition;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

public class GoalDefToControllerGoal {
	private static GoalDefToControllerGoal instance = new GoalDefToControllerGoal();
	public static GoalDefToControllerGoal getInstance() {return instance;}
	
	public GRControllerGoal<String> buildControllerGoal(ControllerGoalDefinition goalDef) {
		GRControllerGoal<String> result = new GRControllerGoal<String>();
		result.setPermissive(goalDef.isPermissive());
		this.buildSubGoals(result, goalDef);
		result.setNonBlocking(goalDef.isNonBlocking());
		result.setExceptionHandling(goalDef.isExceptionHandling());
		this.buildControllableActionSet(result, goalDef);
		result.setLazyness(goalDef.getLazyness());
		result.setNonTransient(goalDef.isNonTransient());
        result.setReachability(goalDef.isReachability());
        if(goalDef.isTestLatency()){
            result.setTestLatency(goalDef.getMaxSchedulers(), goalDef.getMaxControllers());
        }
		return result;
	}

	@SuppressWarnings("unchecked")
	private void buildControllableActionSet(ControllerGoal<String> goal, ControllerGoalDefinition goalDef) {

		Hashtable<?, ?> constants = LabelSet.getConstants();
		Symbol controllableActionSet = goalDef.getControllableActionSet();
		LabelSet labelSet = (LabelSet) constants.get(controllableActionSet.toString());
		if (labelSet==null) {
			Diagnostics.fatal("Controllable actions set not defined.");
		}
		Vector<String> actions = labelSet.getActions(null);
		goal.addAllControllableActions(new HashSet<String>(actions));
	}

	private void buildSubGoals(ControllerGoal<String> result, ControllerGoalDefinition goalDef) {
		Set<Fluent> involvedFluents = new HashSet<Fluent>();
		
		Set<Fluent> fluentsInFaults = new HashSet<Fluent>();
		//Convert faults to Set<Formula> 
		for (lts.Symbol faultDefinition : goalDef.getFaultsDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(faultDefinition.getName());
			if (def!=null){
				result.addFault(FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), fluentsInFaults));
			} else {
				Diagnostics.fatal("Assertion not defined [" + faultDefinition.getName() + "].");
			}
		}
		involvedFluents.addAll(fluentsInFaults);
		result.addAllFluentsInFaults(fluentsInFaults);

		//Convert assumptions to Set<Formula> 
		for (lts.Symbol assumeDefinition : goalDef.getAssumeDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(assumeDefinition.getName());
			if (def!=null){
				result.addAssume(FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), involvedFluents));
			} else {
				Diagnostics.fatal("Assertion not defined [" + assumeDefinition.getName() + "].");
			}
		}

		//Convert guarantees to Set<Formula> 
		for (lts.Symbol guaranteeDefinition : goalDef.getGuaranteeDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(guaranteeDefinition.getName());
			if (def!=null){
				result.addGuarantee(FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), involvedFluents));
			} else {
			  PredicateDefinition fdef = PredicateDefinition.get(guaranteeDefinition.getName());
			  if (fdef != null)
			    result.addGuarantee(FormulaUtils.adaptFormulaAndCreateFluents(new FormulaFactory().make
					    (guaranteeDefinition), involvedFluents));
			  else
				  //Diagnostics.fatal("Assertion not defined [" + guaranteeDefinition.getName() + "].");
			    Diagnostics.fatal("Fluent/assertion not defined [" + guaranteeDefinition.getName() + "].");
			}
		}
		
		Set<Fluent> concurrencyFluents = new HashSet<Fluent>();
		//Convert faults to Set<Formula> 
		for (lts.Symbol concurrencyDefinition : goalDef.getConcurrencyDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(concurrencyDefinition.getName());
			if (def!=null){
				FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), concurrencyFluents);
			} else {
				Diagnostics.fatal("Assertion not defined [" + concurrencyDefinition.getName() + "].");
			}
		}
		result.addAllConcurrencyFluents(concurrencyFluents);
		involvedFluents.addAll(concurrencyFluents);
		

		Set<Fluent> activityFluents = new HashSet<Fluent>();
		//Convert faults to Set<Formula> 
		for (lts.Symbol activityDefinition : goalDef.getActivityDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(activityDefinition.getName());
			if (def!=null){
				FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), activityFluents);
			} else {
				Diagnostics.fatal("Assertion not defined [" + activityDefinition.getName() + "].");
			}
		}
		result.addAllActivityFluents(activityFluents);
		involvedFluents.addAll(activityFluents);
		
		
		
		result.addAllFluents(involvedFluents);
		
		
		
	}
}
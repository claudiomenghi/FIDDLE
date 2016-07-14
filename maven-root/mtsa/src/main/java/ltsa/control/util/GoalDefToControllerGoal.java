package ltsa.control.util;

import MTSSynthesis.ar.dc.uba.model.condition.Fluent;
import ltsa.control.ControllerGoalDefinition;
import MTSSynthesis.controller.model.ControllerGoal;
import MTSSynthesis.controller.model.gr.GRControllerGoal;
import ltsa.lts.Diagnostics;
import ltsa.lts.chart.util.FormulaUtils;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.FormulaFactory;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.parser.LabelSet;
import ltsa.lts.parser.Symbol;

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
		for (Symbol faultDefinition : goalDef.getFaultsDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(faultDefinition.getValue());
			if (def!=null){
				result.addFault(FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), fluentsInFaults));
			} else {
				Diagnostics.fatal("Assertion not defined [" + faultDefinition.getValue() + "].");
			}
		}
		involvedFluents.addAll(fluentsInFaults);
		result.addAllFluentsInFaults(fluentsInFaults);

		//Convert assumptions to Set<Formula> 
		for (ltsa.lts.parser.Symbol assumeDefinition : goalDef.getAssumeDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(assumeDefinition.getValue());
			if (def!=null){
				result.addAssume(FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), involvedFluents));
			} else {
				Diagnostics.fatal("Assertion not defined [" + assumeDefinition.getValue() + "].");
			}
		}

		//Convert guarantees to Set<Formula> 
		for (ltsa.lts.parser.Symbol guaranteeDefinition : goalDef.getGuaranteeDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(guaranteeDefinition.getValue());
			if (def!=null){
				result.addGuarantee(FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), involvedFluents));
			} else {
			  PredicateDefinition fdef = PredicateDefinition.get(guaranteeDefinition.getValue());
			  if (fdef != null)
			    result.addGuarantee(FormulaUtils.adaptFormulaAndCreateFluents(new FormulaFactory().make
					    (guaranteeDefinition), involvedFluents));
			  else
				  //Diagnostics.fatal("Assertion not defined [" + guaranteeDefinition.getName() + "].");
			    Diagnostics.fatal("Fluent/assertion not defined [" + guaranteeDefinition.getValue() + "].");
			}
		}
		
		Set<Fluent> concurrencyFluents = new HashSet<Fluent>();
		//Convert faults to Set<Formula> 
		for (ltsa.lts.parser.Symbol concurrencyDefinition : goalDef.getConcurrencyDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(concurrencyDefinition.getValue());
			if (def!=null){
				FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), concurrencyFluents);
			} else {
				Diagnostics.fatal("Assertion not defined [" + concurrencyDefinition.getValue() + "].");
			}
		}
		result.addAllConcurrencyFluents(concurrencyFluents);
		involvedFluents.addAll(concurrencyFluents);
		

		Set<Fluent> activityFluents = new HashSet<Fluent>();
		//Convert faults to Set<Formula> 
		for (ltsa.lts.parser.Symbol activityDefinition : goalDef.getActivityDefinitions()) {
			AssertDefinition def = AssertDefinition.getDefinition(activityDefinition.getValue());
			if (def!=null){
				FormulaUtils.adaptFormulaAndCreateFluents(def.getFormula(true), activityFluents);
			} else {
				Diagnostics.fatal("Assertion not defined [" + activityDefinition.getValue() + "].");
			}
		}
		result.addAllActivityFluents(activityFluents);
		involvedFluents.addAll(activityFluents);
		
		
		
		result.addAllFluents(involvedFluents);
		
		
		
	}
}
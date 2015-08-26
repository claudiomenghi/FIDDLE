package controller.model;

import java.util.List;
import java.util.Set;

import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.Formula;

/**
/*
 * DIPI: Needs check. 
 * This interface it's mixed. it allows the user to work with asumes and guarantees
 * This interface it's mixed. it allows the user to work with assumes and guarantees
 * while he's working with fluents and formulas. 
 * 
 * @author dipi
 * @author ndippolito
 *
 */
//DIPI: rethinks needed. This interface may be similar to game stuff. Faults and assume will not be present for every controller.  
public interface ControllerGoal<Action> {

	public abstract boolean addAssume(Formula assume);

	public abstract boolean addGuarantee(Formula guarantee);

	public abstract boolean addAllFluents(Set<Fluent> involvedFluents);
	public abstract boolean addSafety(Formula safetyFormula);
	public abstract boolean addFault(Formula fault);
	
	public abstract boolean addAllFluents(Set<Fluent> involvedFluents);
	public abstract boolean addAllConcurrencyFluents(Set<Fluent> concurrencyFluents);
	public abstract boolean addAllActivityFluents(Set<Fluent> activityFluents);
	public abstract boolean addAllFluentsInFaults(Set<Fluent> fluentsInFaults);
	public abstract boolean addAllSafetyFluents(Set<Fluent> safetyFluents);
	
	public abstract Set<Fluent> getConcurrencyFluents(); 
	
	public abstract boolean addAllActivityFluents(Set<Fluent> activityFluents);
	
	public abstract Set<Fluent> getActivityFluents(); 
	public abstract Set<Fluent> getFluentsInFaults();

	public abstract Set<Fluent> getFluents();

	public abstract Set<Fluent> getFluentsInFaults();

	public abstract List<Formula> getFaults();

	public abstract List<Formula> getAssumptions();

	public abstract List<Formula> getGuarantees();

	public abstract Set<Action> getControllableActions();

	public abstract void addAllControllableActions(Set<Action> controllableActions);

	public abstract void addFault(Formula adaptFormulaAndCreateFluents);

	public abstract void addAllFluentsInFaults(Set<Fluent> fluentsInFaults);

	public abstract Set<Action> getControllableActions();
	
}
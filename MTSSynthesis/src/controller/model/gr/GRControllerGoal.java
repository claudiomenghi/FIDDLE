package controller.model.gr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.Formula;
import controller.model.ControllerGoal;

public class GRControllerGoal<Action> implements ControllerGoal<Action>,Cloneable{
	private List<Formula> faults;
	private List<Formula> assumptions;
	private List<Formula> guarantees;
	private List<Formula> safety;
	
	private Set<Action> controllableActions;

	private Set<Fluent> fluents;
	private Set<Fluent> fluentsInFaults;
	private Set<Fluent> concurrencyFluents;
	private Set<Fluent> activityFluents;
	private Set<Fluent> safetyFluents;

	private boolean nonTransient;
	private boolean testLatency;
	private boolean isPermissive;
	private boolean isNonBlocking;
	private boolean exceptionHandling;
	private boolean reachability;
	
	private Integer lazyness;
	private Integer maxControllers;
	private Integer maxSchedulers;
	
	public GRControllerGoal() {
		this.faults = new ArrayList<Formula>();
		this.assumptions = new ArrayList<Formula>();
		this.guarantees = new ArrayList<Formula>();
		this.safety = new ArrayList<Formula>();
		this.faults = new ArrayList<Formula>();
		this.controllableActions = new HashSet<Action>();
		this.fluents = new HashSet<Fluent>();
		this.concurrencyFluents = new HashSet<Fluent>();
		this.activityFluents = new HashSet<Fluent>();
		this.fluentsInFaults = new HashSet<Fluent>();
		this.safetyFluents = new HashSet<Fluent>();
		this.reachability = false;
		this.testLatency = false;
		this.lazyness = 0;
		this.maxControllers = 0;
		this.maxSchedulers = 0;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		GRControllerGoal<Action> clone = new GRControllerGoal<Action>();
		clone.faults = this.faults;
		clone.assumptions = this.assumptions;
		clone.guarantees = this.guarantees;
		clone.safety = this.safety;
		clone.fluents = this.fluents;
		clone.controllableActions = this.controllableActions;
		clone.concurrencyFluents = this.concurrencyFluents;
		clone.activityFluents = this.activityFluents;
		clone.lazyness = this.lazyness;
		clone.nonTransient = this.nonTransient;
		clone.testLatency = this.testLatency;
		clone.maxControllers = this.maxControllers;
		clone.maxSchedulers = this.maxSchedulers;
		clone.reachability = this.reachability;
		return clone;
	}
	
	public GRControllerGoal<Action> cloneWithAssumptionsAsGoals(){
		GRControllerGoal<Action> reducedGoal;
		try {
			reducedGoal = ((GRControllerGoal<Action>)this.clone());
			reducedGoal.guarantees = reducedGoal.assumptions;
			reducedGoal.assumptions = new ArrayList<Formula>();
			reducedGoal.assumptions.add(Formula.TRUE_FORMULA);
			
		} catch (CloneNotSupportedException e) {
			reducedGoal = this;
			e.printStackTrace();
		}		
		return reducedGoal;
	}
	
	public boolean isNonTransient() {
		return nonTransient;
	}
	
	public void setNonTransient(boolean nonTransient) {
		this.nonTransient = nonTransient;
	}
	
	public void setReachability(boolean reachability) {
		this.reachability = reachability;
	}
	
	public boolean isReachability() {
		return reachability;
	}
	
	public void setTestLatency(Integer maxSchedulers, Integer maxControllers) {
		this.testLatency = true;
		this.maxControllers = maxControllers;
		this.maxSchedulers = maxSchedulers;
	}
	
	public boolean isTestLatency() {
		return testLatency;
	}
	
	public Integer getMaxControllers() {
		return maxControllers;
	}
	
	public Integer getMaxSchedulers() {
		return maxSchedulers;
	}
	
	public boolean isNonBlocking() {
		return isNonBlocking;
	}

	public void setNonBlocking(boolean isNonBlocking) {
		this.isNonBlocking = isNonBlocking;
	}

	/* (non-Javadoc)
	 * @see controller.model.gr.ControllerGoal#addAssume(ar.dc.uba.model.condition.Formula)
	 */
	public boolean addAssume(Formula assume) {
		return this.assumptions.add(assume);
	}

	/* (non-Javadoc)
	 * @see controller.model.gr.ControllerGoal#addGuarantee(ar.dc.uba.model.condition.Formula)
	 */
	public boolean addGuarantee(Formula guarantee) {
		return this.guarantees.add(guarantee);
	}

	/* (non-Javadoc)
	 * @see controller.model.gr.ControllerGoal#addAllFluents(java.util.Set)
	 */
	public boolean addAllFluents(Set<Fluent> involvedFluents) {
		return this.fluents.addAll(involvedFluents);
	}

	/* (non-Javadoc)
	 * @see controller.model.gr.ControllerGoal#getFluents()
	 */
	public Set<Fluent> getFluents() {
		return this.fluents;
	}

	public boolean addAllActivityFluents(Set<Fluent> fluents){
		return this.activityFluents.addAll(fluents);
	}
	
	public Set<Fluent> getActivityFluents() {
		return activityFluents;
	}
	
	
	public boolean addAllConcurrencyFluents(Set<Fluent> concurrencyFluents) {
		return this.concurrencyFluents.addAll(concurrencyFluents);
	}
	
	public Set<Fluent> getConcurrencyFluents() {
		return this.concurrencyFluents;
	}
	
	/* (non-Javadoc)
	 * @see controller.model.gr.ControllerGoal#getAssumptions()
	 */
	public List<Formula> getAssumptions() {
		return assumptions;
	}

	/* (non-Javadoc)
	 * @see controller.model.gr.ControllerGoal#getGuarantees()
	 */
	public List<Formula> getGuarantees() {
		return guarantees;
	}

	/* (non-Javadoc)
	 * @see controller.model.gr.ControllerGoal#getControllableActions()
	 */
	public Set<Action> getControllableActions() {
		return controllableActions;
	}

	/* (non-Javadoc)
	 * @see controller.model.gr.ControllerGoal#addAllControllableActions(java.util.Set)
	 */
	public void addAllControllableActions(Set<Action> controllableActions) {
		this.controllableActions.addAll(controllableActions);
	}

	@Override
	public boolean addFault(Formula faultFormula) {
		return this.faults.add(faultFormula);
	}

	@Override
	public List<Formula> getFaults() {
		return this.faults;
	}

	@Override
	public boolean addAllFluentsInFaults(Set<Fluent> fluentsInFaults) {
		return this.fluentsInFaults.addAll(fluentsInFaults);
	}

	@Override
	public Set<Fluent> getFluentsInFaults() {
		return this.fluentsInFaults;
	}

	public boolean isPermissive() {
		return isPermissive;
	}

	public void setPermissive(boolean isPermissive) {
		this.isPermissive = isPermissive;
	}


	public boolean isExceptionHandling() {
		return exceptionHandling;
	}


	public void setExceptionHandling(boolean exceptionHandling) {
		this.exceptionHandling = exceptionHandling;
	}

	public Integer getLazyness() {
		return lazyness;
	}
	
	public void setLazyness(Integer lazyness) {
		this.lazyness = lazyness;
	}

	@Override
	public boolean addAllSafetyFluents(Set<Fluent> safetyFluents) {
		return this.safetyFluents.addAll(safetyFluents);
	}

	@Override
	public boolean addSafety(Formula safetyFormula) {
		return this.safety.add(safetyFormula);
	}
	
	public List<Formula> getSafety() {
		return safety;
	}
	
	public Set<Fluent> getSafetyFluents() {
		return this.safetyFluents;
	}
	
}

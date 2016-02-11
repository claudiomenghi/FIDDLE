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
	private Set<Fluent> fluents;
	private Set<Action> controllableActions;
	private Set<Fluent> fluentsInFaults;
	private boolean isPermissive;
	private boolean isNonBlocking;
	private boolean exceptionHandling;
	private Set<Fluent> concurrencyFluents;
	private Set<Fluent> activityFluents;
	private Integer lazyness;
	private boolean nonTransient;
	private boolean testLatency;
	private Integer maxControllers;
	private Integer maxSchedulers;
	private boolean reachability;
	
	
	public GRControllerGoal() {
		this.faults = new ArrayList<Formula>();
		this.assumptions = new ArrayList<Formula>();
		this.guarantees = new ArrayList<Formula>();
		this.fluents = new HashSet<Fluent>();
		this.controllableActions = new HashSet<Action>();
		this.concurrencyFluents = new HashSet<Fluent>();
		this.activityFluents = new HashSet<Fluent>();
		this.lazyness = 0;
		this.testLatency = false;
		this.maxControllers = 0;
		this.maxSchedulers = 0;
		this.reachability = false;

	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		GRControllerGoal<Action> clone = new GRControllerGoal<Action>();
		clone.faults = this.faults;
		clone.assumptions = this.assumptions;
		clone.guarantees = this.guarantees;
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
			// TODO Auto-generated catch block
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

	public void setControllableActions(Set<Action> controllableActions)
	{
		this.controllableActions = controllableActions;
	}

	/* (non-Javadoc)
	 * @see controller.model.gr.ControllerGoal#addAllControllableActions(java.util.Set)
	 */
	public void addAllControllableActions(Set<Action> controllableActions) {
		this.controllableActions.addAll(controllableActions);
	}

	@Override
	public void addFault(Formula faultFormula) {
		this.faults.add(faultFormula);
	}

	@Override
	public List<Formula> getFaults() {
		return this.faults;
	}

	@Override
	public void addAllFluentsInFaults(Set<Fluent> fluentsInFaults) {
		this.fluentsInFaults = fluentsInFaults;
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

	public GRControllerGoal<String> copy()
	{
		GRControllerGoal<String> copy = new GRControllerGoal<String>();
		copy.isPermissive = this.isPermissive;
		copy.isNonBlocking = this.isNonBlocking;
		copy.exceptionHandling = this.exceptionHandling;
		copy.nonTransient = this.nonTransient;
		copy.lazyness = this.lazyness;
		copy.faults = copyListFormula(this.faults);
		copy.assumptions = copyListFormula(this.assumptions);
		copy.guarantees = copyListFormula(this.guarantees);
		copy.fluents = copySetaFluent(this.fluents);
		copy.fluentsInFaults = copySetaFluent(this.fluentsInFaults);
		copy.concurrencyFluents = copySetaFluent(this.concurrencyFluents);

		if (this.controllableActions == null)
			copy.controllableActions = null;
		else
		{
			Set<String> copyControllableActions = new HashSet<String>();
			for (Action anAction : this.controllableActions)
				copyControllableActions.add(anAction.toString());
			copy.controllableActions = copyControllableActions;
		}

		return copy;
	}

	private List<Formula> copyListFormula(List<Formula> list)
	{
		if (list == null)
			return null;

		List<Formula> copy = new ArrayList<Formula>();
		for (Formula aFormula : list)
			copy.add(aFormula);
		return copy;
	}
	private Set<Fluent> copySetaFluent(Set<Fluent> set)
	{
		if (set == null)
			return null;

		Set<Fluent> copy = new HashSet<Fluent>();
		for (Fluent aFluent : set)
			copy.add(aFluent);
		return copy;
	}
}

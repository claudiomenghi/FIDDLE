package controller.gr.time;

import java.util.Set;

import ac.ic.doc.commons.relations.Pair;

public class EnvScheduler<S,A> extends GenericChooser<S,A,Pair<S,S>> {

	Set<A> controllableActions; 
	Set<A> uncontrollableActions;
	
	public EnvScheduler(Set<A> controllableActions, Set<A> uncontrollableActions) {
		super();
		this.controllableActions = controllableActions;
		this.uncontrollableActions = uncontrollableActions;
	}
	
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof EnvScheduler<?, ?>){
			EnvScheduler<?, ?> gc = (EnvScheduler<?, ?>) obj;
			return this.controllableActions.equals(gc.controllableActions) 
					&& this.uncontrollableActions.equals(gc.uncontrollableActions)
					&& super.equals(gc);
		}
		else
			return false;	
	}
	

	@Override
	protected Set<A> getUncontrollableActions() {
		return uncontrollableActions;
	}

	@Override
	protected Set<A> getControllableActions() {
		return controllableActions;
	}

}

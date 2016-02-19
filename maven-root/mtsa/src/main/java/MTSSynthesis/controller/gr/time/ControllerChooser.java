package MTSSynthesis.controller.gr.time;

import java.util.Set;

public class ControllerChooser<S,A> extends GenericChooser<S,A,S> {

	Set<A> controllableActions; 
	Set<A> uncontrollableActions;
	
	public ControllerChooser(Set<A> controllableActions, Set<A> uncontrollableActions) {
		super();
		this.controllableActions = controllableActions;
		this.uncontrollableActions = uncontrollableActions;
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

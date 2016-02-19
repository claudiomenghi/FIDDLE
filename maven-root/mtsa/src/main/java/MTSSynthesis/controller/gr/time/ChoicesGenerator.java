package MTSSynthesis.controller.gr.time;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import MTSSynthesis.controller.gr.time.model.Choice;
import MTSSynthesis.controller.gr.time.model.ChoiceType;
import MTSTools.ac.ic.doc.commons.relations.BinaryRelation;
import MTSTools.ac.ic.doc.commons.relations.Pair;
import MTSTools.ac.ic.doc.mtstools.model.LTS;

public abstract class ChoicesGenerator<S,A> {
	Map<S, Set<A>> controllable;
	Map<S, Set<A>> uncontrollable;
	Map<S, Set<A>> ends;
	Set<A> uncontrollableActions;
	Set<A> controllableActions;
	Set<A> endActions;
	Map<S,List<Choice<A>>> choices;

	protected ChoicesGenerator(){}
	
	public ChoicesGenerator(LTS<S, A> environment, Set<A> controllableActions) {
		init(environment, controllableActions);
	}

	protected void init(LTS<S, A> environment, Set<A> controllableActions) {
		this.controllable = new HashMap<S, Set<A>>();
		this.uncontrollable = new HashMap<S, Set<A>>();
		this.ends = new HashMap<S, Set<A>>();
		this.uncontrollableActions = new HashSet<A>();
		this.endActions = new HashSet<A>();
		this.controllableActions = controllableActions;
		classifyTransitions(environment, controllableActions);
	}

	private void classifyTransitions(LTS<S, A> environment,	Set<A> controllableActions) {
		for(S state : environment.getStates()){
			identifyTransitionType(environment.getTransitions(state), controllableActions, state);
		}
	}
	
	private void identifyTransitionType(BinaryRelation<A, S> stateTransitions, Set<A> cActions, S state) {
		Set<A> controllable = new HashSet<A>();
		Set<A> uncontrollable = new HashSet<A>();
		Set<A> ends = new HashSet<A>();
		for(Pair<A, S> transition : stateTransitions){
			A label = transition.getFirst();
			SchedulerUtils<A> su = new SchedulerUtils<A>();
			ChoiceType type = su.getChoiceType(label, cActions);
			if(type.equals(ChoiceType.CONTROLLABLE))
				controllable.add(label);
			else if(type.equals(ChoiceType.UNCONTROLLABLE)){
				uncontrollable.add(label);
				uncontrollableActions.add(label);
			}else if(type.equals(ChoiceType.ENDS)){
				ends.add(label);
				endActions.add(label);
			}else 
				throw new RuntimeException();
		}
		this.controllable.put(state,controllable);
		this.uncontrollable.put(state,uncontrollable);
		this.ends.put(state,ends);
	}
	
	protected abstract List<Choice<A>> getStateChoices(S st);
	
	protected Map<S , List<Choice<A>>> getChoices(){
		Map<S , List<Choice<A>>>  result = new HashMap<S , List<Choice<A>>>();
		for (S st : controllable.keySet()) {
			List<Choice<A>> choices = getStateChoices(st);
			result.put(st, choices);
		}
		return result;
	}
	
	public Set<A> getUncontrollableActions() {
		return uncontrollableActions;
	}

	public Set<A> getEndActions() {
		return endActions;
	}
	
	@Override
	public String toString() {
		return choices.toString();
	}
}

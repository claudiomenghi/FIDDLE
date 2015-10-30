package controller.gr.time;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang.math.RandomUtils;

import controller.gr.time.model.ActivityDefinitions;
import controller.gr.time.model.Choice;
import controller.gr.time.model.EnvScheduler;
import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.LTS;

public class SchedulerGenerator<S,A> {
	LTS<S,A> environment;
	Set<GenericChooser<S, A, Pair<S, S>>> generated;
	Set<A> controllableActions;
	Set<A> uncontrollableActions;
	Set<A> endActions;
	Map<S , List<Choice<A>>> choices;
	int limit;
	Long maximum;
	GenericChooser<S, A, Pair<S,S>> result;
	Set<GenericChooser<S, A, Pair<S,S>>> lasts;

	
	Set<A> uncontrollableChoices;
	ActivityDefinitions<A> activityDefinitions;
	
	protected SchedulerGenerator() {/*-_-*/}
	public SchedulerGenerator(LTS<S,A> environment, Set<A> controllableActions, ActivityDefinitions<A> activityDefinitions) {
		init(environment, controllableActions, activityDefinitions);
	}

	protected void init(LTS<S, A> environment, Set<A> controllableActions,ActivityDefinitions<A> activityDefinitions) {
		this.environment = environment;
		this.generated = new HashSet<GenericChooser<S, A, Pair<S,S>>>();
		this.controllableActions = controllableActions;
		SchedulerChoicesGenerator<S,A> choiceGenerator = getChoiceGenerator(environment, controllableActions);
		this.uncontrollableActions = choiceGenerator.getUncontrollableActions();
		this.endActions = choiceGenerator.getEndActions();
		this.choices = choiceGenerator.getChoices();
		this.limit = 2048;
		this.maximum = null;
		this.activityDefinitions = activityDefinitions;
	}

	protected SchedulerChoicesGenerator<S, A> getChoiceGenerator(LTS<S, A> environment, Set<A> controllableActions) {
		return new SchedulerChoicesGenerator<S,A>(environment, controllableActions);
	}
	
	public Long getEstimation(){
		if(this.maximum == null){
			Long acum = 1L;
			for (S k : choices.keySet()) {
				int size = choices.get(k).size();
				if(size > 0){
					acum *= size;
				}
			}
			this.maximum = acum;
		}
		return this.maximum;
	}
	
	
	public Long goodEstimation(){
		if(this.maximum == null){
			
		}
		return this.maximum;
	}
	
	
	public Set<GenericChooser<S, A, Pair<S,S>>> next(int cant){
		int i = 0;
		this.lasts  = new HashSet<GenericChooser<S, A, Pair<S,S>>>();
		if(get()!=null){
			this.lasts.add(get());
			i++;
		}
		while(next()!=null && i<cant){
			this.lasts.add(get());
			i++;
		}
		return this.lasts;
	}
	
	public GenericChooser<S, A, Pair<S,S>> get() {
		return result;
	}

	public Set<GenericChooser<S, A, Pair<S,S>>> getLasts(){
		return this.lasts;
	}
	
	public GenericChooser<S, A, Pair<S,S>> next(){
		int i = 0;
		result = null; 
		while(i < limit){
			result = new EnvScheduler<S,A>(controllableActions,uncontrollableActions);
			chooseActions(result,environment.getInitialState(), new HashSet<S>());
			if(generated.contains(result)){
				i++;
				result = null;
			}else{
				generated.add(result);
				return result;
			}
		}
		return result;
	}

	public Set<GenericChooser<S, A, Pair<S,S>>> getGenerated() {
		return generated;
	}
	
	private void chooseActions(GenericChooser<S, A, Pair<S, S>> scheduler, S initial, Set<S> visited) {
		Set<S> added = new HashSet<S>();
		Queue<S> pending  = new LinkedList<S>();
		Set<A> uncontrollableChoices = new HashSet<A>();
		pending.add(initial);
		added.add(initial);
		while(!pending.isEmpty()){
			S state = pending.poll();
			List<Choice<A>> choices = getChoices(state, uncontrollableChoices);
			if(!choices.isEmpty()){
				Choice<A> choice = choices.get(RandomUtils.nextInt(choices.size()));
				//Add uncontrollableChoices to be consistent. 
				for (A label : uncontrollableActions) {
					if(choice.getAlternative().contains(label) || choice.getChoice().contains(label)){
						uncontrollableChoices.add(label);
					}
				}
				scheduler.setChoice(state, choice);
				addSuccesors(state, added, pending, choice);
			}
		}
		scheduler.setUncontrollableChoices(uncontrollableChoices);
	}

	private void addSuccesors(S state, Set<S> added, Queue<S> pending,
			Choice<A> choice) {
		for (A label : choice.getAvailableLabels()) {
			for(S succ : environment.getTransitions(state).getImage(label)){
				if(!added.contains(succ)){
					pending.add(succ);
					added.add(succ);
				}
			}
		}
	}
	
	private List<Choice<A>> getChoices(S state, Set<A> uncontrollableChoices) {
		List<Choice<A>> filteredChoices = new ArrayList<Choice<A>>();
		for(Choice<A> choice : choices.get(state)){
			if(isCompatible(choice, uncontrollableChoices)){
				filteredChoices.add(choice);
			}
		}
		return filteredChoices;
	}

	private boolean isCompatible(Choice<A> choice, Set<A> uncontrollableChoices) {
		return compatibleLabel(uncontrollableChoices, choice.getChoice()) 
				&& compatibleLabel(uncontrollableChoices, choice.getAlternative());
	}

	private boolean compatibleLabel(Set<A> uncontrollableChoices, Set<A> labels) {
		for(A label: labels){
			if(uncontrollableActions.contains(label) && activityDefinitions.hasRelatedActions(label)){
				for(A related: activityDefinitions.getRelatedActions(label)){
					if(!related.equals(label) && uncontrollableChoices.contains(related)){
						return false;
					}
				}
			}
		}
		return true;
	}
}


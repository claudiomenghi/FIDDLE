package controller.gr.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang.math.RandomUtils;

import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.LTS;
import controller.gr.time.model.ActivityDefinitions;
import controller.gr.time.model.Choice;
import controller.gr.time.model.EnvScheduler;

public class SchedulerGeneratorLight<S,A> implements SkeletonBuilder<S,A,Pair<S,S>>{
	LTS<S,A> environment;
	Set<Map<S, Integer>> generated;
	Set<A> controllableActions;
	Set<A> uncontrollableActions;
	Set<A> endActions;
	Map<S , List<Choice<A>>> choices;
	int limit;
	Long maximum;
	Map<S,Integer> result;
	Set<Map<S,Integer>> lasts;

	
	Set<A> uncontrollableChoices;
	ActivityDefinitions<A> activityDefinitions;
	
	protected SchedulerGeneratorLight() {/*-_-*/}
	public SchedulerGeneratorLight(LTS<S,A> environment, Set<A> controllableActions, ActivityDefinitions<A> activityDefinitions) {
		init(environment, controllableActions, activityDefinitions);
	}

	protected void init(LTS<S, A> environment, Set<A> controllableActions,ActivityDefinitions<A> activityDefinitions) {
		this.environment = environment;
		this.generated = new HashSet<Map<S, Integer>>();
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
	
	
	public Set<Map<S,Integer>> next(int cant){
		int i = 0;
		this.lasts  = new HashSet<Map<S,Integer>>();
		if(result!=null){
			this.lasts.add(result);
			i++;
		}
		while(next()!=null && i<cant){
			this.lasts.add(result);
			i++;
		}
		return this.lasts;
	}
	
	public GenericChooser<S, A, Pair<S,S>> get() {
		return build(result);
	}

	public Set<Map<S, Integer>> getLasts(){
		return this.lasts;
	}
	
	public Map<S,Integer> next(){
		int i = 0;
		result = null; 
		while(i < limit){
			Map<S,Integer> skeleton = new HashMap<S,Integer>();
			chooseActions(skeleton,environment.getInitialState(), new HashSet<S>());
			if(generated.contains(skeleton)){
				i++;
			}else{
				generated.add(skeleton);
				result = skeleton;
				break;
			}
		}
		return result;
	}
	
	public GenericChooser<S, A, Pair<S, S>> build(Map<S, Integer> skeleton) {
		EnvScheduler<S,A> result = new EnvScheduler<S, A>(controllableActions, uncontrollableActions);
		for (S s: skeleton.keySet()) {
			result.setChoice(s, choices.get(s).get(skeleton.get(s)));
		}
		return result;
	}

	public Set<Map<S,Integer>> getGenerated() {
		return generated;
	}
	
	private void chooseActions(Map<S, Integer> skeleton, S initial, Set<S> visited) {
		Set<S> added = new HashSet<S>();
		Queue<S> pending  = new LinkedList<S>();
		Set<A> uncontrollableChoices = new HashSet<A>();
		pending.add(initial);
		added.add(initial);
		while(!pending.isEmpty()){
			S state = pending.poll();
			List<Integer> virtual_ids = getChoices(state, uncontrollableChoices);
			if(!virtual_ids.isEmpty()){
				int virtual_id = RandomUtils.nextInt(virtual_ids.size());
				int real_id = virtual_ids.get(virtual_id);
				Choice<A> choice = this.choices.get(state).get(real_id);
				//Add uncontrollableChoices to be consistent. 
				for (A label : uncontrollableActions) {
					if(choice.getAlternative().contains(label) || choice.getChoice().contains(label)){
						uncontrollableChoices.add(label);
					}
				}
				skeleton.put(state, real_id);
				addSuccesors(state, added, pending, choice);
			}
		}
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
	
	private List<Integer> getChoices(S state, Set<A> uncontrollableChoices) {
		List<Integer> filteredChoices = new ArrayList<Integer>();
		List<Choice<A>> actualChoices = choices.get(state);
		for(int i=0; i < actualChoices.size(); i++){
			if(isCompatible(actualChoices.get(i), uncontrollableChoices)){
				filteredChoices.add(i);
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


package MTSSynthesis.controller.gr.time;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang.math.RandomUtils;

import MTSTools.ac.ic.doc.mtstools.model.LTS;
import MTSSynthesis.controller.gr.time.model.Choice;

public class ControllerGenerator<S,A> {
	LTS<S,A> controller;
	Set<GenericChooser<S, A, S>> generated;
	Set<A> controllableActions;
	Set<A> uncontrollableActions;
	Set<A> endActions;
	Map<S , List<Choice<A>>> choices;
	int limit;
	ControllerChooser<S, A> result;
	Set<ControllerChooser<S,A>> lasts;
	
	public ControllerGenerator(LTS<S,A> controller, Set<A> controllableActions, Set<S> finalStates) {
		this.controller = controller;
		this.generated = new HashSet<GenericChooser<S, A, S>>();
		this.controllableActions = controllableActions;
		ControllerChoicesGenerator<S,A> choiceGenerator = new ControllerChoicesGenerator<S,A>(controller, controllableActions, finalStates);
		this.uncontrollableActions = choiceGenerator.getUncontrollableActions();
		this.endActions = choiceGenerator.getEndActions();
		this.choices = choiceGenerator.getChoices();
		this.limit = 2048;
	}

	
	public Set<ControllerChooser<S,A>> getLasts(){
		return this.lasts;
	}
	
	public Set<ControllerChooser<S,A>> next(int cant){
		int i = 0;
		this.lasts  = new HashSet<ControllerChooser<S,A>>();
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
	
	public ControllerChooser<S,A> next(){
		int i = 0;
		result = null; 
		while(i < limit){
			result = new ControllerChooser<S,A>(controllableActions,uncontrollableActions);
			chooseActions(result,controller.getInitialState(), new HashSet<S>());
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
	
	public ControllerChooser<S, A> get() {
		return result;
	}

	public Set<GenericChooser<S, A, S>> getGenerated() {
		return generated;
	}
	
	private void chooseActions(ControllerChooser<S, A> scheduler, S initial, Set<S> visited) {
		Set<S> added = new HashSet<S>();
		Queue<S> pending  = new LinkedList<S>();
		pending.add(initial);
		added.add(initial);
		while(!pending.isEmpty()){
			S state = pending.poll();
			List<Choice<A>> choices = getChoices(state);
			if(!choices.isEmpty()){
				Choice<A> choice = choices.get(RandomUtils.nextInt(choices.size()));
				scheduler.setChoice(state, choice);
				addSuccesors(state, added, pending, choice);
			}
		}
	}

	private void addSuccesors(S state, Set<S> added, Queue<S> pending,
			Choice<A> choice) {
		for (A label : choice.getAvailableLabels()) {
			for(S succ : controller.getTransitions(state).getImage(label)){
				if(!added.contains(succ)){
					pending.add(succ);
					added.add(succ);
				}
			}
		}
	}
	
	private List<Choice<A>> getChoices(S state) {
		return choices.get(state);
	}

}


package MTSSynthesis.controller.gr.time;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import MTSSynthesis.controller.gr.time.model.Choice;
import MTSTools.ac.ic.doc.mtstools.model.LTS;

public class ControllerChoicesGenerator<S, A> extends ChoicesGenerator<S, A> {
	Set<S> finalStates;
	
	public ControllerChoicesGenerator(LTS<S, A> environment, Set<A> controllableActions, Set<S> finalStates) {
		super();
		this.finalStates = finalStates;
		init(environment, controllableActions);
		this.choices = this.getChoices();
	}
	
	@Override
	protected List<Choice<A>> getStateChoices(S st) {
		List<Choice<A>> choices = new ArrayList<Choice<A>>();
		if(!finalStates.contains(st)){
			//We are generating controllers with one or zero  
			//controllable actions enabled in each state. 
			Set<A> controllableActions = controllable.get(st);
			Set<A> uncontrollableAction = new HashSet<A>();
			uncontrollableAction.addAll(uncontrollable.get(st));
			uncontrollableAction.addAll(ends.get(st));
			
			for (A c : controllableActions) {
				Set<A> choice  = new HashSet<A>();
				choice.add(c);
				choice.addAll(uncontrollableAction);
				choices.add(new Choice<A>(choice));
			}
			
			if(!uncontrollableAction.isEmpty()){
				choices.add(new Choice<A>(uncontrollableAction));
			}
		}
		return choices;
	}
	
	
	
}

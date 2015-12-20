package controller.gr.time;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import controller.gr.time.model.Choice;
import ac.ic.doc.mtstools.model.LTS;

public class SchedulerChoicesGenerator<S, A> extends ChoicesGenerator<S, A> {
	protected SchedulerChoicesGenerator() {}
	public SchedulerChoicesGenerator(LTS<S, A> environment, Set<A> controllableActions) {
		super(environment, controllableActions);
		this.choices = this.getChoices();
	}
	
	@Override
	protected List<Choice<A>> getStateChoices(S st) {
		List<Choice<A>> choices = new ArrayList<Choice<A>>();
		//We are choosing all the controllable set, since we assume 
		//the controllers that we are going to schedule
		//have only one controllable action enabled. 
		Set<A> controllableActions = getControllableFromState(st);
		if(uncontrollable.get(st).isEmpty() && ends.get(st).isEmpty()){
			choices.add(new Choice<A>(controllableActions));	
		}else{
			if(!controllableActions.isEmpty()){
				for (A u : uncontrollable.get(st)) {
					Set<A> alternative  = new HashSet<A>();
					alternative.add(u);
					choices.add(new Choice<A>(controllableActions,alternative));
					choices.add(new Choice<A>(alternative));
				}
			}else{
				for (A u : uncontrollable.get(st)) {
					Set<A> choice  = new HashSet<A>();
					choice.add(u);
					choices.add(new Choice<A>(choice));
				}
			}
			if(!ends.get(st).isEmpty()){
				choices.add(new Choice<A>(ends.get(st)));
				if(!controllableActions.isEmpty()){
					choices.add(new Choice<A>(controllableActions,ends.get(st)));	
				}
			}
		}
		return choices;
	}

	protected Set<A> getControllableFromState(S st) {
		return controllable.get(st);
	}
}

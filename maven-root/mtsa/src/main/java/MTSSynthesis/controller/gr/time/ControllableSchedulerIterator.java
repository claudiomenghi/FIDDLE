package MTSSynthesis.controller.gr.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import MTSSynthesis.controller.gr.time.model.Choice;
import MTSTools.ac.ic.doc.mtstools.model.MTS;


public class ControllableSchedulerIterator<S,A> extends StrategyIterator<S,A> {

	public ControllableSchedulerIterator(MTS<S, A> mts, Set<A> controllableActions, Set<S> finalStates) {
		super(mts, controllableActions, finalStates);
	}
	
	@Override
	protected Map<S , List<Choice<A>>> getChoices(Set<S> finalState) {
		Map<S , List<Choice<A>>>  result = new HashMap<S , List<Choice<A>>>();
		for (S st : controllable.keySet()) {
			List<Choice<A>> choices = new ArrayList<Choice<A>>();
			if(!finalState.contains(st)){
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
			result.put(st, choices);
		}
		return result;
	}
}

package controller.gr.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import controller.gr.time.model.Choice;
import controller.gr.time.model.Scheduler;
import ac.ic.doc.mtstools.model.MTS;

public class SchedulerIterator<S,A> extends RandomStrategyIterator<S, A>{
	private Map<A,Integer> restrictions;
	private ArrayList<Set<A>> relatedActions;
	protected SchedulerIterator(){}
	public SchedulerIterator(MTS<S, A> mts, Set<A> controllableActions, Set<S> finalStates, ArrayList<Set<A>> relatedActions) {
		restrictions = new HashMap<A,Integer>();
		this.relatedActions = relatedActions;
		for (int i=0; i< relatedActions.size();i++) {
			Set<A> set = relatedActions.get(i);
			for (A a : set) {
				restrictions.put(a, i);
			}
		}
		super.init(mts, controllableActions, finalStates);
	}
	
	@Override
	protected Map<S , List<Choice<A>>> getChoices(Set<S> finalStates) {
		Map<S, List<Choice<A>>>  result = new HashMap<S, List<Choice<A>>>();
		for (S st : controllable.keySet()) {
			if(!finalStates.contains(st)){
				List<Choice<A>> choices = getStateChoices(st);
				result.put(st, choices);
			}else{
				result.put(st, new ArrayList<Choice<A>>());
			}
		}
		return result;
	}
	private List<Choice<A>> getStateChoices(S st) {
		List<Choice<A>> choices = new ArrayList<Choice<A>>();
		//We are choosing all the controllable set, since we assume
		//the controllers that we are going to schedule
		//have only one controllable action enabled.
		Set<A> controllableActions = controllable.get(st);
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
	
	@Override
	protected boolean compatible(Scheduler<S, A> sl, Scheduler<S, A> sc) {
		return (super.compatible(sl, sc) && consistentUncontrollable(sl.getUncontrollableChoices(), sc.getUncontrollableChoices()));
	}

	private boolean consistentUncontrollable(Set<A> ucl, Set<A> ucc) {
		for(A u :ucl){
			if(restrictions.containsKey(u)){
				for (A u2: ucc) {
					if(relatedActions.get(restrictions.get(u)).contains(u2) && !u2.equals(u)){
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public void setIter(int i){
		validateIndex(i);
		iter = i;
	}
	
	public Scheduler<S, A> get(int next) {
		validateIndex(next);
		return schedulers.get(next);
	}
	private void validateIndex(int next) {
		if(next >= schedulers.size()){
			throw new RuntimeException("Index out of bound for the Strategy Iterator");
		}
	}
	
}

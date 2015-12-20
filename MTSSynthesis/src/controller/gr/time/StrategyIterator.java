package controller.gr.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import controller.gr.time.model.Choice;
import controller.gr.time.model.ChoiceType;
import controller.gr.time.model.Scheduler;
import ac.ic.doc.commons.relations.BinaryRelation;
import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.MTS.TransitionType;

public abstract class StrategyIterator<S,A>{
	Map<S, Set<A>> controllable;
	Map<S, Set<A>> uncontrollable;
	Map<S, Set<A>> ends;
	List<Scheduler<S,A>> schedulers;
	private Set<A> controllableActions;
	private Set<A> uncontrollableActions;
	Map<S , List<Choice<A>>>  choices;
	int iter;
	
	public int getIter() {
		return iter;
	}
	
	protected StrategyIterator() {}
	
	public StrategyIterator(MTS<S, A> mts, Set<A> controllableActions2, Set<S> finalStates) {
		init(mts, controllableActions2, finalStates);
	}

	protected void init(MTS<S, A> mts,
			Set<A> controllableActions2, Set<S> finalStates) {
		this.controllable = new HashMap<S, Set<A>>();
		this.uncontrollable = new HashMap<S, Set<A>>();
		this.ends = new HashMap<S, Set<A>>();
		this.setUncontrollableActions(new HashSet<A>());
		iter = 0;
		this.setControllableActions(controllableActions2);
		for(S state : mts.getStates()){
			identifyTransitionType(mts.getTransitions(state, TransitionType.REQUIRED), controllableActions2, state);
		}
		this.choices = this.getChoices(finalStates);
		this.schedulers = this.generate(mts);
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
				getUncontrollableActions().add(label);
			}else if(type.equals(ChoiceType.ENDS))
				ends.add(label);
			else 
				throw new RuntimeException();
		}
		this.controllable.put(state,controllable);
		this.uncontrollable.put(state,uncontrollable);
		this.ends.put(state,ends);
	}
	
	protected  List<Scheduler<S,A>> generate(MTS<S, A> mts){
		return this.generate(mts, mts.getInitialState());
	}
	
	protected List<Scheduler<S,A>> generate(MTS<S, A> mts, S st) {
		List<Scheduler<S, A>> res = new ArrayList<Scheduler<S,A>>();
		//TODO: No there are not empty choices states. 
		if(!this.choices.get(st).isEmpty()){
			BinaryRelation<A, S> transitions = mts.getTransitions(st, TransitionType.REQUIRED);
			for(Choice<A> c: this.getChoices(st)){
				Map<A, List<Scheduler<S,A>>> Ss =  new HashMap<A, List<Scheduler<S,A>>>();
				for (A l: c.getAvailableLabels()){
					Set<S> succs = transitions.getImage(l);
					for (S succ : succs) {
						//TODO: Avoid revisiting states 
						if(!st.equals(succ)){
							List<Scheduler<S,A>> scheds = generate(mts, succ);
							if(!Ss.containsKey(l))
								Ss.put(l, scheds);
							else 
								Ss.put(l, mergeSchedullers(Ss.get(l),scheds));
						}else{
							List<Scheduler<S, A>> sub_res = new ArrayList<Scheduler<S,A>>();
							Scheduler<S, A> s = new Scheduler<S, A>(this);
							s.setChoice(st, new Choice<A>());
							sub_res.add(s);
							Ss.put(l, sub_res);
						}
					}
				}
				List<Scheduler<S,A>> cs = cartesiano(Ss,new HashSet<A>(Ss.keySet()));
				for (Scheduler<S, A> scheduller : cs) {
					scheduller.setChoice(st, c);
					res.add(scheduller);
				}
			}
			return res;
		}else{
			Scheduler<S, A> s = new Scheduler<S, A>(this);
			s.setChoice(st, new Choice<A>());
			res.add(s);
		}
		return res;
	}

	protected List<Choice<A>> getChoices(S st) {
		return choices.get(st);
	}

	private List<Scheduler<S, A>> cartesiano(Map<A, List<Scheduler<S, A>>> ss, Set<A> ks) {
		if(ks.isEmpty())
			return new ArrayList<Scheduler<S,A>>();
		A k =  ks.iterator().next();
		ks.remove(k);
		return mergeSchedullers(ss.get(k),cartesiano(ss, new HashSet<A>(ks)));
	}

	private List<Scheduler<S, A>> mergeSchedullers(List<Scheduler<S, A>> k_list, List<Scheduler<S, A>> c_list) {
		if(c_list.isEmpty())
			return k_list;
		else{
			List<Scheduler<S, A>> res = new ArrayList<Scheduler<S,A>>();
			for (Scheduler<S, A> sl: k_list) {
				for (Scheduler<S, A> sc: c_list) {
					if(compatible(sl,sc))
						res.add(merge(sl,sc));
				}
			}
			return res;
		}
	}

	protected boolean compatible(Scheduler<S, A> sl, Scheduler<S, A> sc) {
		for (S s1: sl.getStates()) {
			if(sc.isDefined(s1) && !sc.getChoice(s1).equals(sl.getChoice(s1))){
				return false;
			}
		}
		return true;
	}

	private Scheduler<S, A> merge(Scheduler<S, A> sl, Scheduler<S, A> sc) {
		Scheduler<S, A> sched = new Scheduler<S,A>(this);
		for (S s : sl.getStates()) {
			sched.setChoice(s, sl.getChoice(s));
		}
		for (S s : sc.getStates()) {
			sched.setChoice(s, sc.getChoice(s));
		}
		return sched;
	}

	protected abstract Map<S , List<Choice<A>>> getChoices(Set<S> finalStates);

	public boolean hasNext(){
		 return  iter < this.schedulers.size(); 
	}
	
	public Scheduler<S, A> next(){
		return this.schedulers.get(iter++);
	}
	
	public Scheduler<S, A> peek(){
		return this.schedulers.get(iter);
	}

	public void reset() {
		this.iter = 0;
	}
	
	public Integer getSize(){
		if(schedulers!=null){
			return schedulers.size();
		}else{
			return 0;
		}
	}

	public Set<A> getUncontrollableActions() {
		return uncontrollableActions;
	}

	public void setUncontrollableActions(Set<A> uncontrollableActions) {
		this.uncontrollableActions = uncontrollableActions;
	}

	public Set<A> getControllableActions() {
		return controllableActions;
	}

	public void setControllableActions(Set<A> controllableActions) {
		this.controllableActions = controllableActions;
	}
}

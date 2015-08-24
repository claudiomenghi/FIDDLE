package controller.gr.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ac.ic.doc.mtstools.model.MTS;


public abstract class RandomStrategyIterator<S, A> extends StrategyIterator<S, A> {
	HashMap<S, Integer> depth;
	Random random;
	static private int SEED_UID = 34496723;
	MTS<S, A> mts;
	Set<Map<S,Integer>> choosed;
	Map<S,Integer> actualChoice;
	protected RandomStrategyIterator(){}
	public RandomStrategyIterator(MTS<S, A> mts, Set<A> controllableActions, Set<S> finalState, ArrayList<Set<A>> relatedActions) {
		this.depth = new HashMap<S, Integer>();
		init(mts, controllableActions, finalState);
	}
	@Override
	protected void init(MTS<S, A> mts, Set<A> controllableActions, Set<S> finalStates) {
		this.random = new Random(mts.getStates().size()*SEED_UID);
		this.choosed = new HashSet<Map<S,Integer>>();
		this.mts = mts;
		super.init(mts, controllableActions, finalStates);
	}
	
	@Override
	protected List<Scheduler<S,A>> generate(MTS<S, A> mts){
		this.actualChoice = new HashMap<S, Integer>();
		return generate(mts,mts.getInitialState());
	}
	
	protected List<Scheduler<S,A>> generate(MTS<S, A> mts, S st, int previous_deepth) {
		if(!depth.containsKey(st)){
			depth.put(st, previous_deepth);
		}
		List<Scheduler<S,A>> schedulers = super.generate(mts, st);
		return schedulers;
	}

	@Override
		public boolean hasNext() {
			if(super.hasNext()){
				return true;
			}
			this.iter = 0;
			generateNext();
			return super.hasNext();
		}
	
	public void generateNext(){
		int tries = 2000;
		//this is the previous choice
		choosed.add(actualChoice);
		List<Scheduler<S,A>> schedulers = null;
		do{
			schedulers = generate(this.mts);
			tries--;
		}while(choosed.contains(actualChoice) && tries > 0);
		
		if(tries > 0){
			this.schedulers = schedulers;
		}else{
			this.schedulers = new ArrayList<Scheduler<S,A>>();
		}
		
	}
	
	@Override
	protected boolean compatible(Scheduler<S, A> sl, Scheduler<S, A> sc) {
		return (super.compatible(sl, sc) && randomChoice());
	}

	private boolean randomChoice() {
		return true;
	}
	
	@Override
	protected List<Choice<A>> getChoices(S st) {
		int choice;
		if (actualChoice.containsKey(st)){
			choice = actualChoice.get(st);
		}else{
			if(this.choices.get(st).size()>1){
				choice = random.nextInt(this.choices.get(st).size());
			}else{
				choice = 0;
			}
			actualChoice.put(st, choice);
		}
		return super.getChoices(st).subList(choice, choice+1);
	}
	
	
	
}

package controller.gr.time;

import java.util.HashSet;
import java.util.Set;

public class Scheduler<S,A> extends Chooser<S,A> {
	Set<A> uncontrollableChoices;

	public Scheduler(StrategyIterator<S, A> strategyIterator) {
		super(strategyIterator);
		this.uncontrollableChoices = new HashSet<A>();
	}
	
	@Override
	public void setChoice(S s,Choice<A> c){
		super.setChoice(s, c);
		if(this.iterator.uncontrollableActions.containsAll(c.getChoice())){
			uncontrollableChoices.addAll(c.getChoice());
		}else if(c.hasAlternative() && this.iterator.uncontrollableActions.containsAll(c.getAlternative())){
			uncontrollableChoices.addAll(c.getAlternative());
		}
	}
}
